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

import com.google.inject.Inject
import common.IncomeSources._
import controllers.predicates.AuthorisedAction
import models.{APIErrorBodyModel, IncomeSources, RefreshIncomeSource}
import play.api.Logging
import play.api.libs.json.{JsSuccess, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.{GetIncomeSourcesService, IncomeTaxUserDataService, RefreshCacheService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

class IncomeSourcesController @Inject()(getIncomeSourcesService: GetIncomeSourcesService,
                                        incomeTaxUserDataService: IncomeTaxUserDataService,
                                        refreshCacheService: RefreshCacheService,
                                        cc: ControllerComponents,
                                        authorisedAction: AuthorisedAction
                                       )(implicit ec: ExecutionContext) extends BackendController(cc) with Logging {

  def getIncomeSources(nino: String, taxYear: Int): Action[AnyContent] = authorisedAction.async { implicit user =>

    val excludedIncomeSources: Seq[String] = user.headers.get("excluded-income-sources").fold(Seq[String]())(_.split(",").toIndexedSeq)

    getIncomeSourcesService.getAllIncomeSources(nino, taxYear, user.mtditid, excludedIncomeSources).flatMap {
      case Right(IncomeSources(None, None, None, None, None, None, None, None, None, None, None)) =>
        incomeTaxUserDataService.saveUserData(taxYear, None)(NoContent)
      case Right(responseModel) =>
        incomeTaxUserDataService.saveUserData(taxYear, Some(responseModel))(Ok(Json.toJson(responseModel)))
      case Left(error) => Future(Status(error.status)(error.toJson))
    }
  }

  def getIncomeSourcesFromSession(nino: String, taxYear: Int): Action[AnyContent] = authorisedAction.async { implicit user =>
    lazy val noDataLog = s"[IncomeTaxUserDataService][findUserData] No user data found. SessionId: ${user.sessionId}"

    incomeTaxUserDataService.findUserData(user, taxYear).map {
      case Right(None) =>
        logger.info(noDataLog)
        NoContent
      case Right(Some(IncomeSources(None, None, None, None, None, None, None, None, None, None, None))) =>
        logger.info(noDataLog)
        NoContent
      case Right(Some(responseModel)) => Ok(Json.toJson(responseModel))
      case Left(error) => Status(error.status)(error.toJson)
    }
  }

  def refreshIncomeSource(nino: String, taxYear: Int): Action[AnyContent] = authorisedAction.async { implicit user =>

    user.body.asJson.map(_.validate[RefreshIncomeSource]) match {
      case Some(JsSuccess(RefreshIncomeSource(incomeSource), _)) =>

        incomeSource match {
          case DIVIDENDS | INTEREST | GIFT_AID | EMPLOYMENT | PENSIONS | CIS | STATE_BENEFITS | INTEREST_SAVINGS | GAINS | STOCK_DIVIDENDS =>
            refreshCacheService.getLatestDataAndRefreshCache(taxYear, incomeSource)
          case _ => Future.successful(BadRequest(Json.toJson(
            APIErrorBodyModel("INVALID_INCOME_SOURCE_PARAMETER", "Invalid income source value."))))
        }

      case _ => Future.successful(BadRequest)
    }
  }
}
