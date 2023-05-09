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

package controllers

import common.IncomeSources._
import controllers.predicates.AuthorisedAction
import models.mongo.ExclusionUserDataModel
import models.{ExcludeJourneyModel, GetExclusionsDataModel, User}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import services.ExcludeJourneyService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ExcludeJourneyController @Inject()(
                                          controllerComponents: ControllerComponents,
                                          auth: AuthorisedAction,
                                          excludeJourneyService: ExcludeJourneyService
                                        )(implicit ec: ExecutionContext) extends BackendController(controllerComponents) {

  val allJourneys: Seq[String] = Seq(INTEREST, DIVIDENDS, GIFT_AID, EMPLOYMENT, CIS, PENSIONS, STATE_BENEFITS, GAINS, STOCK_DIVIDENDS)

  def getExclusions(taxYear: Int, nino: String): Action[AnyContent] = auth.async { implicit user =>
    excludeJourneyService.findExclusionData(taxYear).map {
      case Right(data) =>
        val model = GetExclusionsDataModel(data.map(_.exclusionModel).getOrElse(Seq.empty))
        Ok(model.toJson)
      case Left(error) => InternalServerError(error.message)
    }
  }

  def excludeJourney(taxYear: Int, nino: String): Action[AnyContent] = auth.async { implicit user =>
    user.body.asJson match {
      case None =>
        Future.successful(BadRequest("Invalid Body"))
      case Some(json) =>
        (json \ "journey").asOpt[String] match {
          case None =>
            Future.successful(BadRequest("Incorrect Json Body"))
          case Some(journeyString) =>
            if (allJourneys.contains(journeyString)) {
              handleKey(taxYear, journeyString)
            } else {
              Future.successful(BadRequest("Invalid Journey Key"))
            }
        }
    }
  }

  private[controllers] def handleKey(taxYear: Int, journeyKey: String)(implicit user: User[_], hc: HeaderCarrier): Future[Result] = {
    excludeJourneyService.findExclusionData(taxYear).flatMap {
      case Right(optionalData) =>
        lazy val userData = optionalData.getOrElse(ExclusionUserDataModel(user.nino, taxYear, Seq.empty))

        excludeJourneyService.journeyKeyToModel(taxYear, journeyKey).flatMap {
          case Right(model) =>
            excludeJourneyService.createOrUpdate(
              model,
              userData,
              optionalData.nonEmpty
            ).map(_.fold(error => {
              InternalServerError(error.message)
            }, _ => NoContent))
          case Left(_) => Future.successful(InternalServerError("Error accessing database"))
        }
      case Left(error) => Future.successful(InternalServerError(error.message))
    }
  }

  def clearJourneys(taxYear: Int, nino: String): Action[AnyContent] = auth.async { implicit user =>
    user.body.asJson match {
      case None => Future.successful(BadRequest("Invalid Body"))
      case Some(json) =>
        val updatedJourneys = (json \ "journeys").asOpt[Seq[String]].getOrElse(Seq.empty)

        excludeJourneyService.findExclusionData(taxYear).flatMap {
          case Right(data) =>
            val userData = data.getOrElse(ExclusionUserDataModel(user.nino, taxYear, Seq.empty))
            val newJourneys: Seq[ExcludeJourneyModel] = userData.exclusionModel
              .filterNot(model => updatedJourneys.contains(model.journey))

            excludeJourneyService.createOrUpdate(userData.copy(exclusionModel = newJourneys), data.nonEmpty)
              .map(_.fold(error => InternalServerError(error.message), _ => NoContent))
          case Left(error) => Future.successful(InternalServerError(error.message))
        }
    }
  }

}
