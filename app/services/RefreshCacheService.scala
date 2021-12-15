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

import common.IncomeSources.{DIVIDENDS, EMPLOYMENT, GIFT_AID, INTEREST}
import config.AppConfig
import javax.inject.{Inject, Singleton}
import models.employment.frontend.AllEmploymentData
import models.giftAid.GiftAidModel
import models._
import play.api.Logging
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.{NoContent, NotFound, Status}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class RefreshCacheService @Inject()(getIncomeSourcesService: GetIncomeSourcesService,
                                    incomeTaxUserDataService: IncomeTaxUserDataService,
                                    implicit private val appConfig: AppConfig) extends Logging {

  def getLatestDataAndRefreshCache(taxYear: Int, incomeSource: String)
                                  (implicit user: User[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {

    getLatestDataForIncomeSource(taxYear, incomeSource).flatMap {
      case Right(None) => updateCacheBasedOnNewData(taxYear,incomeSource,None)
      case Right(Some(model: DividendsModel)) => updateCacheBasedOnNewData[DividendsModel](taxYear,incomeSource,Some(model))
      case Right(Some(model: AllEmploymentData)) => updateCacheBasedOnNewData[AllEmploymentData](taxYear,incomeSource,Some(model))
      case Right(Some(model: GiftAidModel)) => updateCacheBasedOnNewData[GiftAidModel](taxYear,incomeSource,Some(model))
      case Right(Some(model: List[InterestModel])) => updateCacheBasedOnNewData[List[InterestModel]](taxYear,incomeSource,Some(model))
      case Left(error) => Future.successful(Status(error.status)(error.toJson))
      case _ => Future.successful(Status(INTERNAL_SERVER_ERROR)(Json.toJson(APIErrorBodyModel.parsingError)))

    }
  }

  private def getLatestDataForIncomeSource[Response](taxYear: Int, incomeSource: String)
                                            (implicit user: User[_], hc: HeaderCarrier): Future[Either[APIErrorModel, Option[Any]]] = {
    val nino = user.nino

    incomeSource match {
      case DIVIDENDS => getIncomeSourcesService.getDividends(nino,taxYear,user.mtditid)
      case INTEREST => getIncomeSourcesService.getInterest(nino,taxYear,user.mtditid)
      case GIFT_AID  => getIncomeSourcesService.getGiftAid(nino,taxYear,user.mtditid)
      case EMPLOYMENT => getIncomeSourcesService.getEmployment(nino,taxYear,user.mtditid)
    }
  }

  private def log(method: String): String = s"[RefreshIncomeSourcesController][$method]"

  private def createModelFromNewData[A](newData: Option[A], currentData: IncomeSourcesResponseModel, incomeSource: String): IncomeSourcesResponseModel ={
    newData match {
      case Some(model: DividendsModel) =>  currentData.copy(dividends = Some(model))
      case Some(model: AllEmploymentData) => currentData.copy(employment = Some(model))
      case Some(model: GiftAidModel) => currentData.copy(giftAid = Some(model))
      case Some(model: List[InterestModel]) => currentData.copy(interest = Some(model))
      case _ =>

        incomeSource match {
          case DIVIDENDS => currentData.copy(dividends = None)
          case INTEREST => currentData.copy(interest = None)
          case GIFT_AID  => currentData.copy(giftAid = None)
          case EMPLOYMENT => currentData.copy(employment = None)
        }
    }
  }

  private def updateCacheBasedOnNewData[A](taxYear: Int, incomeSource: String, newData: Option[A])
                                          (implicit user: User[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] ={

    incomeTaxUserDataService.findUserData(user, taxYear).flatMap {
      case Right(None) | Right(Some(IncomeSourcesResponseModel(None, None, None, None))) =>

        logger.info(s"${log("updateCacheBasedOnNewData")} User doesn't have any cache data or doesn't have any income source data." +
          s" SessionId: ${user.sessionId}")

        newData match {
          case data@Some(_) =>

            val model = createModelFromNewData(data, IncomeSourcesResponseModel(), incomeSource)
            incomeTaxUserDataService.saveUserData(taxYear, Some(model))(NoContent)

          case None =>

            logger.info(s"${log("updateCacheBasedOnNewData")} User has no new data to refresh the cache. SessionId: ${user.sessionId}")
            Future.successful(NotFound)
        }

      case Right(Some(currentData: IncomeSourcesResponseModel)) =>

        logIfNoIncomeSourceData(incomeSource,currentData)

        val model = createModelFromNewData(newData,currentData,incomeSource)
        incomeTaxUserDataService.saveUserData(taxYear, Some(model))(NoContent)

      case Left(error) => Future.successful(Status(error.status)(error.toJson))
    }
  }

  private def logIfNoIncomeSourceData(incomeSource: String, data: IncomeSourcesResponseModel)
                                     (implicit user: User[_]): Unit ={

    lazy val noDataLog: Unit = logger.info(s"${log("logIncomeSource")} User doesn't have any cache data for $incomeSource. SessionId: ${user.sessionId}")

    incomeSource match {
      case DIVIDENDS => if(data.dividends.isEmpty) noDataLog
      case INTEREST => if(data.interest.isEmpty) noDataLog
      case GIFT_AID  => if(data.giftAid.isEmpty) noDataLog
      case EMPLOYMENT => if(data.employment.isEmpty) noDataLog
    }
  }
}
