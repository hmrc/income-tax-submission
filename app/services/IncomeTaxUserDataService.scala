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
import models.mongo.{DatabaseError, UserData}
import models.{APIErrorBodyModel, APIErrorModel, IncomeSourcesResponseModel, User}
import play.api.Logging
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.mvc.Result
import play.api.mvc.Results.Status
import repositories.IncomeTaxUserDataRepository
import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class IncomeTaxUserDataService @Inject()(incomeTaxUserDataRepository: IncomeTaxUserDataRepository,
                                         implicit private val appConfig: AppConfig) extends Logging {

  def saveUserData(taxYear: Int,
                   incomeSourcesModel: Option[IncomeSourcesResponseModel] = None)
                  (result: Result)
                  (implicit user: User[_], ec: ExecutionContext): Future[Result] = {

    updateUserData(user.sessionId, user.mtditid, user.nino, taxYear, incomeSourcesModel).map {
      case Left(error) =>
        val errorResponse = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("FAILED_TO_SAVE_USER_DATA", error.message))
        Status(errorResponse.status)(errorResponse.toJson)
      case Right(_) =>
        logger.info(s"[IncomeTaxUserDataService][saveUserData] Saved new user data. SessionId: ${user.sessionId}")
        result
    }
  }

  private def updateUserData(sessionId: String,
                             mtdItId: String,
                             nino: String,
                             taxYear: Int,
                             incomeSourcesModel: Option[IncomeSourcesResponseModel]): Future[Either[DatabaseError, Unit]] = {

    val userData = UserData(
      sessionId, mtdItId, nino, taxYear,
      dividends = incomeSourcesModel.flatMap(_.dividends),
      interest = incomeSourcesModel.flatMap(_.interest),
      giftAid = incomeSourcesModel.flatMap(_.giftAid),
      employment = incomeSourcesModel.flatMap(_.employment)
    )

    incomeTaxUserDataRepository.update(userData)
  }

  def findUserData(user: User[_], taxYear: Int)(implicit ec: ExecutionContext): Future[Either[APIErrorModel, Option[IncomeSourcesResponseModel]]] = {
    incomeTaxUserDataRepository.find(user, taxYear).map {
      case Right(Some(userData: UserData)) => Right(Some(userData.toIncomeSourcesResponseModel))
      case Right(None) => Right(None)
      case Left(error) => Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("FAILED_TO_FIND_USER_DATA", error.message)))
    }
  }
}
