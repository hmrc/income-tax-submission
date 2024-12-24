/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.predicates

import common.{DelegatedAuthRules, EnrolmentIdentifiers, EnrolmentKeys}
import config.AppConfig
import models.User
import models.logging.CorrelationIdMdc.withEnrichedCorrelationId
import play.api.Logger
import play.api.mvc.Results.{InternalServerError, Unauthorized}
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, allEnrolments, confidenceLevel}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

class AuthorisedAction @Inject()()(implicit val authConnector: AuthConnector,
                                   defaultActionBuilder: DefaultActionBuilder,
                                   appConfig: AppConfig,
                                   val cc: ControllerComponents) extends AuthorisedFunctions {

  lazy val logger: Logger = Logger.apply(this.getClass)
  implicit val executionContext: ExecutionContext = cc.executionContext

  val unauthorized: Future[Result] = Future(Unauthorized)

  def async(block: User[AnyContent] => Future[Result]): Action[AnyContent] = defaultActionBuilder.async { original =>
    withEnrichedCorrelationId(original) { request =>
      implicit lazy val headerCarrier: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

      request.headers.get("mtditid").fold {
        logger.warn("[AuthorisedAction][async] - No MTDITID in the header. Returning unauthorised.")
        unauthorized
      }(
        mtdItId =>
          authorised().retrieve(affinityGroup) {
            case Some(AffinityGroup.Agent) => agentAuthentication(block, mtdItId)(request, headerCarrier)
            case _ => individualAuthentication(block, mtdItId)(request, headerCarrier)
          } recover {
            case _: NoActiveSession =>
              logger.warn(s"[AuthorisedAction][async] - No active session.")
              Unauthorized
            case _: AuthorisationException =>
              logger.warn(s"[AuthorisedAction][async] - User failed to authenticate")
              Unauthorized
            case e =>
              logger.error(s"[AuthorisedAction][async] - Unexpected exception of type '${e.getClass.getSimpleName}' was caught.")
              InternalServerError
          }
      )
    }

  }

  val minimumConfidenceLevel: Int = ConfidenceLevel.L250.level

  def sessionId(implicit request: Request[_], hc: HeaderCarrier): Option[String] = {
    lazy val key = "sessionId"
    if (hc.sessionId.isDefined) {
      hc.sessionId.map(_.value)
    } else if (request.headers.get(key).isDefined) {
      request.headers.get(key)
    } else {
      None
    }
  }

  private[predicates] def individualAuthentication[A](block: User[A] => Future[Result], requestMtdItId: String)
                                                     (implicit request: Request[A], hc: HeaderCarrier): Future[Result] = {
    authorised().retrieve(allEnrolments and confidenceLevel) {
      case enrolments ~ userConfidence if userConfidence.level >= minimumConfidenceLevel =>
        val optionalMtdItId: Option[String] = enrolmentGetIdentifierValue(EnrolmentKeys.Individual, EnrolmentIdentifiers.individualId, enrolments)
        val optionalNino: Option[String] = enrolmentGetIdentifierValue(EnrolmentKeys.nino, EnrolmentIdentifiers.nino, enrolments)

        (optionalMtdItId, optionalNino) match {
          case (Some(authMTDITID), Some(nino)) if authMTDITID.equals(requestMtdItId) =>

            sessionId.fold {
              logger.info(s"[AuthorisedAction][individualAuthentication] - No session id in request")
              unauthorized
            } { sessionId =>
              block(User(authMTDITID, None, nino, sessionId))
            }

          case (Some(_), Some(_)) =>
            logger.warn(s"[AuthorisedAction][individualAuthentication] - MTDITID in request does not match id in auth")
            unauthorized
          case (_, None) =>
            logger.warn(s"[AuthorisedAction][individualAuthentication] - User has no nino.")
            unauthorized
          case (None, _) =>
            logger.warn(s"[AuthorisedAction][individualAuthentication] - User has no MTD IT enrolment.")
            unauthorized
        }
      case _ =>
        logger.warn("[AuthorisedAction][individualAuthentication] User has confidence level below 250.")
        unauthorized
    }
  }

  private[predicates] def agentAuthPredicate(mtdId: String): Predicate =
    Enrolment(EnrolmentKeys.Individual)
      .withIdentifier(EnrolmentIdentifiers.individualId, mtdId)
      .withDelegatedAuthRule(DelegatedAuthRules.agentDelegatedAuthRule)

  private[predicates] def secondaryAgentPredicate(mtdId: String): Predicate =
    Enrolment(EnrolmentKeys.SupportingAgent)
      .withIdentifier(EnrolmentIdentifiers.individualId, mtdId)
      .withDelegatedAuthRule(DelegatedAuthRules.supportingAgentDelegatedAuthRule)

  private[predicates] def agentAuthentication[A](block: User[A] => Future[Result], requestMtdItId: String)
                                                (implicit request: Request[A], hc: HeaderCarrier): Future[Result] = {

    val ninoRegex: Regex = ".*/nino/(.*)/sources.*".r

    request.uri match {
      case ninoRegex(nino) =>
        authorised(agentAuthPredicate(requestMtdItId))
          .retrieve(allEnrolments) {
            populateAgent(block, requestMtdItId, nino, _, isSupportingAgent = false)
          }.recoverWith(agentRecovery(block, requestMtdItId, nino))
      case _ =>
        logger.warn(s"$agentAuthLogString - Could not parse Nino from uri")
        Future(Unauthorized)
    }
  }

  private val agentAuthLogString: String = "[AuthorisedAction][agentAuthentication]"

  private def agentRecovery[A](block: User[A] => Future[Result],
                               mtdItId: String,
                               nino: String)
                              (implicit request: Request[A], hc: HeaderCarrier): PartialFunction[Throwable, Future[Result]] = {
    case _: NoActiveSession =>
      logger.warn(s"$agentAuthLogString - No active session.")
      unauthorized
    case _: AuthorisationException if appConfig.emaSupportingAgentsEnabled =>
      authorised(secondaryAgentPredicate(mtdItId))
        .retrieve(allEnrolments) {
          populateAgent(block, mtdItId, nino, _, isSupportingAgent = true)
        }.recoverWith {
          case _: AuthorisationException =>
            logger.warn(s"$agentAuthLogString - Agent does not have secondary delegated authority for Client.")
            unauthorized
          case e =>
            logger.error(s"$agentAuthLogString - Unexpected exception of type '${e.getClass.getSimpleName}' was caught.")
            Future.successful(InternalServerError)
        }
    case _: AuthorisationException =>
      logger.warn(s"$agentAuthLogString - Agent does not have delegated authority for Client.")
      unauthorized
    case e =>
      logger.error(s"$agentAuthLogString - Unexpected exception of type '${e.getClass.getSimpleName}' was caught.")
      Future.successful(InternalServerError)
  }

  private def populateAgent[A](block: User[A] => Future[Result],
                               requestMtdItId: String,
                               nino: String,
                               enrolments: Enrolments,
                               isSupportingAgent: Boolean)(implicit request: Request[A], hc: HeaderCarrier): Future[Result] = {
    enrolmentGetIdentifierValue(EnrolmentKeys.Agent, EnrolmentIdentifiers.agentReference, enrolments) match {
      case Some(arn) =>
        sessionId.fold {
          logger.warn(s"$agentAuthLogString - No session id in request")
          unauthorized
        } { sessionId =>
          block(User(requestMtdItId, Some(arn), nino, sessionId, isSupportingAgent))
        }
      case None =>
        logger.warn(s"$agentAuthLogString Agent with no HMRC-AS-AGENT enrolment.")
        unauthorized
    }
  }

  private[predicates] def enrolmentGetIdentifierValue(checkedKey: String,
                                                      checkedIdentifier: String,
                                                      enrolments: Enrolments): Option[String] = enrolments.enrolments.collectFirst {
    case Enrolment(`checkedKey`, enrolmentIdentifiers, _, _) => enrolmentIdentifiers.collectFirst {
      case EnrolmentIdentifier(`checkedIdentifier`, identifierValue) => identifierValue
    }
  }.flatten

}
