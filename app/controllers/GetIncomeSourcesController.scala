/*
 * Copyright 2021 HM Revenue & Customs
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

import com.google.inject.Inject
import controllers.predicates.AuthorisedAction
import models.IncomeSourcesResponseModel
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.{GetIncomeSourcesService, IncomeTaxUserDataService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

class GetIncomeSourcesController @Inject()(getIncomeSourcesService: GetIncomeSourcesService,
                                           incomeTaxUserDataService: IncomeTaxUserDataService,
                                           cc: ControllerComponents,
                                           authorisedAction: AuthorisedAction
                                          )(implicit ec: ExecutionContext) extends BackendController(cc) with Logging {

  def getIncomeSources(nino: String, taxYear: Int): Action[AnyContent] = authorisedAction.async { implicit user =>

    val excludedIncomeSources: Seq[String] = user.headers.get("excluded-income-sources").fold[Seq[String]](Seq.empty)(_.split(","))

    getIncomeSourcesService.getAllIncomeSources(nino, taxYear, user.mtditid, excludedIncomeSources).flatMap {
      case Right(IncomeSourcesResponseModel(None, None, None, None)) =>
        incomeTaxUserDataService.saveUserData(taxYear,None)(NoContent)
      case Right(responseModel) =>
        incomeTaxUserDataService.saveUserData(taxYear,Some(responseModel))(Ok(Json.toJson(responseModel)))
      case Left(error) => Future(Status(error.status)(error.toJson))
    }
  }

  def getIncomeSourcesFromSession(nino: String, taxYear: Int): Action[AnyContent] = authorisedAction.async { implicit user =>
    lazy val noDataLog = s"[IncomeTaxUserDataService][findUserData] No user data found. SessionId: ${user.sessionId}"

    incomeTaxUserDataService.findUserData(user, taxYear).map {
      case Right(None) =>
        logger.info(noDataLog)
        NoContent
      case Right(Some(IncomeSourcesResponseModel(None, None, None, None))) =>
        logger.info(noDataLog)
        NoContent
      case Right(Some(responseModel)) => Ok(Json.toJson(responseModel))
      case Left(error) => Status(error.status)(error.toJson)
    }
  }
}
