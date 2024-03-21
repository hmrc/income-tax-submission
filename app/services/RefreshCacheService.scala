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

package services

import common.IncomeSources._
import config.AppConfig
import models._
import models.cis.AllCISDeductions
import models.employment.AllEmploymentData
import models.gains.InsurancePoliciesModel
import models.gifts.GiftAid
import models.pensions.Pensions
import models.statebenefits.AllStateBenefitsData
import play.api.Logging
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.{NoContent, NotFound, Status}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RefreshCacheService @Inject()(getIncomeSourcesService: GetIncomeSourcesService,
                                    incomeTaxUserDataService: IncomeTaxUserDataService) extends Logging {

  def getLatestDataAndRefreshCache(taxYear: Int, incomeSource: String)
                                  (implicit user: User[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {

    getLatestDataForIncomeSource(taxYear, incomeSource).flatMap {
      case Right(None) => updateCacheBasedOnNewData(taxYear, incomeSource, None)
      case Right(Some(model: Dividends)) => updateCacheBasedOnNewData[Dividends](taxYear, incomeSource, Some(model))
      case Right(Some(model: AllEmploymentData)) => updateCacheBasedOnNewData[AllEmploymentData](taxYear, incomeSource, Some(model))
      case Right(Some(model: GiftAid)) => updateCacheBasedOnNewData[GiftAid](taxYear, incomeSource, Some(model))
      case Right(Some(model: List[Interest])) => updateCacheBasedOnNewData[List[Interest]](taxYear, incomeSource, Some(model))
      case Right(Some(model: Pensions)) => updateCacheBasedOnNewData[Pensions](taxYear, incomeSource, Some(model))
      case Right(Some(model: AllCISDeductions)) => updateCacheBasedOnNewData[AllCISDeductions](taxYear, incomeSource, Some(model))
      case Right(Some(model: AllStateBenefitsData)) => updateCacheBasedOnNewData[AllStateBenefitsData](taxYear, incomeSource, Some(model))
      case Right(Some(model: SavingsIncomeDataModel)) => updateCacheBasedOnNewData[SavingsIncomeDataModel](taxYear, incomeSource, Some(model))
      case Right(Some(model: InsurancePoliciesModel)) => updateCacheBasedOnNewData[InsurancePoliciesModel](taxYear, incomeSource, Some(model))
      case Left(error) => Future.successful(Status(error.status)(error.toJson))
      case _ => Future.successful(Status(INTERNAL_SERVER_ERROR)(Json.toJson(APIErrorBodyModel.parsingError)))
    }
  }

  private def getLatestDataForIncomeSource(taxYear: Int, incomeSource: String)
                                          (implicit user: User[_], hc: HeaderCarrier): Future[Either[APIErrorModel, Option[Any]]] = {
    val nino = user.nino

    incomeSource match {
      case DIVIDENDS => getIncomeSourcesService.getDividends(nino, taxYear, user.mtditid)
      case INTEREST => getIncomeSourcesService.getInterest(nino, taxYear, user.mtditid)
      case GIFT_AID => getIncomeSourcesService.getGiftAid(nino, taxYear, user.mtditid)
      case EMPLOYMENT => getIncomeSourcesService.getEmployment(nino, taxYear, user.mtditid)
      case PENSIONS => getIncomeSourcesService.getPensions(nino, taxYear, user.mtditid)
      case CIS => getIncomeSourcesService.getCIS(nino, taxYear, user.mtditid)
      case STATE_BENEFITS => getIncomeSourcesService.getStateBenefits(nino, taxYear, user.mtditid)
      case INTEREST_SAVINGS => getIncomeSourcesService.getSavingsInterest(nino, taxYear, user.mtditid)
      case GAINS => getIncomeSourcesService.getGains(nino, taxYear, user.mtditid)
    }
  }

  private def log(method: String): String = s"[RefreshIncomeSourcesController][$method]"

  private def createModelFromNewData[A](newData: Option[A], currentData: IncomeSources, incomeSource: String): IncomeSources = {
    newData match {
      case Some(model: Dividends) => currentData.copy(dividends = Some(model))
      case Some(model: AllEmploymentData) => currentData.copy(employment = Some(model))
      case Some(model: GiftAid) => currentData.copy(giftAid = Some(model))
      case Some(model: List[Interest]) => currentData.copy(interest = Some(model))
      case Some(model: Pensions) => currentData.copy(pensions = Some(model))
      case Some(model: AllCISDeductions) => currentData.copy(cis = Some(model))
      case Some(model: AllStateBenefitsData) => currentData.copy(stateBenefits = Some(model))
      case Some(model: SavingsIncomeDataModel) => currentData.copy(interestSavings = Some(model))
      case Some(model: InsurancePoliciesModel) => currentData.copy(gains = Some(model))
      case _ => defaultCurrentData(currentData, incomeSource)

    }
  }

  private def defaultCurrentData(currentData: IncomeSources, incomeSource: String): IncomeSources = {
    incomeSource match {
      case DIVIDENDS => currentData.copy(dividends = None)
      case INTEREST => currentData.copy(interest = None)
      case GIFT_AID => currentData.copy(giftAid = None)
      case EMPLOYMENT => currentData.copy(employment = None)
      case PENSIONS => currentData.copy(pensions = None)
      case CIS => currentData.copy(cis = None)
      case STATE_BENEFITS => currentData.copy(stateBenefits = None)
      case INTEREST_SAVINGS => currentData.copy(interestSavings = None)
      case GAINS => currentData.copy(gains = None)
    }
  }

  private def updateCacheBasedOnNewData[A](taxYear: Int, incomeSource: String, newData: Option[A])
                                          (implicit user: User[_], ec: ExecutionContext): Future[Result] = {

    incomeTaxUserDataService.findUserData(user, taxYear).flatMap {
      case Right(None) | Right(Some(IncomeSources(None, None, None, None, None, None, None, None, None, None, None))) =>

        logger.info(s"${log("updateCacheBasedOnNewData")} User doesn't have any cache data or doesn't have any income source data." +
          s" SessionId: ${user.sessionId}")

        newData match {
          case data@Some(_) =>

            val model = createModelFromNewData(data, IncomeSources(), incomeSource)
            incomeTaxUserDataService.saveUserData(taxYear, Some(model))(NoContent)

          case None =>

            logger.info(s"${log("updateCacheBasedOnNewData")} User has no new data to refresh the cache. SessionId: ${user.sessionId}")
            Future.successful(NotFound)
        }

      case Right(Some(currentData: IncomeSources)) =>

        logIfNoIncomeSourceData(incomeSource, currentData)

        val model = createModelFromNewData(newData, currentData, incomeSource)
        incomeTaxUserDataService.saveUserData(taxYear, Some(model))(NoContent)

      case Left(error) => Future.successful(Status(error.status)(error.toJson))
    }
  }

  private def logIfNoIncomeSourceData(incomeSource: String, data: IncomeSources)
                                     (implicit user: User[_]): Unit = {

    def noDataLog(isEmpty: Boolean): Unit =
      if (isEmpty) logger.info(s"${log("logIncomeSource")} User doesn't have any cache data for $incomeSource. SessionId: ${user.sessionId}")

    incomeSource match {
      case DIVIDENDS => noDataLog(data.dividends.isEmpty)
      case INTEREST => noDataLog(data.interest.isEmpty)
      case GIFT_AID => noDataLog(data.giftAid.isEmpty)
      case EMPLOYMENT => noDataLog(data.employment.isEmpty)
      case PENSIONS => noDataLog(data.pensions.isEmpty)
      case CIS => noDataLog(data.cis.isEmpty)
      case STATE_BENEFITS => noDataLog(data.stateBenefits.isEmpty)
      case INTEREST_SAVINGS => noDataLog(data.interestSavings.isEmpty)
      case GAINS => noDataLog(data.gains.isEmpty)
      case STOCK_DIVIDENDS => noDataLog(data.stockDividends.isEmpty)
    }
  }
}
