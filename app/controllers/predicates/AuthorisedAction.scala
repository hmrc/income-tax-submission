/*
 * Copyright 2022 HM Revenue & Customs
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

import common.{EnrolmentIdentifiers, EnrolmentKeys}
import models.User
import play.api.Logger
import play.api.mvc.Results.Unauthorized
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, allEnrolments, confidenceLevel}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

class AuthorisedAction @Inject()()(implicit val authConnector: AuthConnector,
                                   defaultActionBuilder: DefaultActionBuilder,
                                   val cc: ControllerComponents) extends AuthorisedFunctions {

  lazy val logger: Logger = Logger.apply(this.getClass)
  implicit val executionContext: ExecutionContext = cc.executionContext

  val unauthorized: Future[Result] = Future(Unauthorized)

  def async(block: User[AnyContent] => Future[Result]): Action[AnyContent] = defaultActionBuilder.async { implicit request =>

    implicit lazy val headerCarrier: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    request.headers.get("mtditid").fold {
      logger.warn("[AuthorisedAction][async] - No MTDITID in the header. Returning unauthorised.")
      unauthorized
    }(
      mtdItId =>
        authorised.retrieve(affinityGroup) {
          case Some(AffinityGroup.Agent) => agentAuthentication(block, mtdItId)(request, headerCarrier)
          case _ => individualAuthentication(block, mtdItId)(request, headerCarrier)
        } recover {
          case _: NoActiveSession =>
            logger.info(s"[AuthorisedAction][async] - No active session.")
            Unauthorized
          case _: AuthorisationException =>
            logger.info(s"[AuthorisedAction][async] - User failed to authenticate")
            Unauthorized
        }
    )
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
    authorised.retrieve(allEnrolments and confidenceLevel) {
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
            logger.info(s"[AuthorisedAction][individualAuthentication] - MTDITID in request does not match id in auth")
            unauthorized
          case (_, None) =>
            logger.info(s"[AuthorisedAction][individualAuthentication] - User has no nino.")
            unauthorized
          case (None, _) =>
            logger.info(s"[AuthorisedAction][individualAuthentication] - User has no MTD IT enrolment.")
            unauthorized
        }
      case _ =>
        logger.info("[AuthorisedAction][individualAuthentication] User has confidence level below 250.")
        unauthorized
    }
  }

  private[predicates] def agentAuthentication[A](block: User[A] => Future[Result], requestMtdItId: String)
                                                (implicit request: Request[A], hc: HeaderCarrier): Future[Result] = {

    lazy val agentDelegatedAuthRuleKey = "mtd-it-auth"

    lazy val agentAuthPredicate: String => Enrolment = identifierId =>
      Enrolment(EnrolmentKeys.Individual)
        .withIdentifier(EnrolmentIdentifiers.individualId, identifierId)
        .withDelegatedAuthRule(agentDelegatedAuthRuleKey)

    val nino: Regex = ".*/nino/(.*)/sources.*".r

    val optionalNino = request.uri match {
      case nino(group) => Some(group)
      case _ => None
    }

    optionalNino match {
      case Some(nino) =>

        authorised(agentAuthPredicate(requestMtdItId))
          .retrieve(allEnrolments) { enrolments =>

            enrolmentGetIdentifierValue(EnrolmentKeys.Agent, EnrolmentIdentifiers.agentReference, enrolments) match {
              case Some(arn) =>

                sessionId.fold {
                  logger.info(s"[AuthorisedAction][individualAuthentication] - No session id in request")
                  unauthorized
                } { sessionId =>
                  block(User(requestMtdItId, Some(arn), nino, sessionId))
                }

              case None =>
                logger.info("[AuthorisedAction][agentAuthentication] Agent with no HMRC-AS-AGENT enrolment.")
                unauthorized
            }
          } recover {
          case _: NoActiveSession =>
            logger.info(s"[AuthorisedAction][agentAuthentication] - No active session.")
            Unauthorized
          case _: AuthorisationException =>
            logger.info(s"[AuthorisedAction][agentAuthentication] - Agent does not have delegated authority for Client.")
            Unauthorized
        }
      case _ =>
        logger.info(s"[AuthorisedAction][agentAuthentication] - Could not parse Nino from uri")
        Future(Unauthorized)
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
