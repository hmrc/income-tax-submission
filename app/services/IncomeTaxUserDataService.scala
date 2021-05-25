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

package services

import config.AppConfig
import controllers.Assets.{INTERNAL_SERVER_ERROR, Status}
import javax.inject.{Inject, Singleton}
import models.mongo.UserData
import models.{APIErrorBodyModel, APIErrorModel, IncomeSourcesResponseModel, User}
import play.api.Logging
import play.api.mvc.Result
import repositories.IncomeTaxUserDataRepository

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class IncomeTaxUserDataService @Inject()(incomeTaxUserDataRepository: IncomeTaxUserDataRepository,
                                         implicit private val appConfig: AppConfig) extends Logging {

  def saveUserData(taxYear: Int,
                   incomeSourcesModel: Option[IncomeSourcesResponseModel] = None)
                  (result: Result)
                  (implicit user: User[_], ec: ExecutionContext): Future[Result] = {

    updateUserData(user.sessionId, user.mtditid, user.nino, taxYear, incomeSourcesModel).map {
      response =>
        if (response) {
          result
        } else {
          logger.error("[IncomeTaxUserDataService][saveUserData] Failed to save user data")
          val error = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("FAILED_TO_SAVE_USER_DATA", "Failed to save user data"))
          Status(error.status)(error.toJson)
        }
    }
  }

  private def updateUserData(sessionId: String,
                             mtdItId: String,
                             nino: String,
                             taxYear: Int,
                             incomeSourcesModel: Option[IncomeSourcesResponseModel]): Future[Boolean] = {

    val userData = UserData(
      sessionId, mtdItId, nino, taxYear,
      dividends = incomeSourcesModel.flatMap(_.dividends),
      interest = incomeSourcesModel.flatMap(_.interest),
      giftAid = incomeSourcesModel.flatMap(_.giftAid),
      employment = incomeSourcesModel.flatMap(_.employment)
    )

    incomeTaxUserDataRepository.update(userData)
  }

  def findUserData(user: User[_], taxYear: Int)(implicit ec: ExecutionContext): Future[Option[IncomeSourcesResponseModel]] = {
    incomeTaxUserDataRepository.find(user, taxYear).map {
      case Some(userData: UserData) => Some(userData.toIncomeSourcesResponseModel)
      case _ => None
    }
  }
}
