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
import connectors._
import models._
import models.cis.{AllCISDeductions, CISSource}
import models.employment.AllEmploymentData
import models.gains.InsurancePoliciesModel
import models.gifts.GiftAid
import models.pensions.Pensions
import models.statebenefits.AllStateBenefitsData
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TaskListService @Inject()(dividendsConnector: IncomeTaxDividendsConnector,
                                interestConnector: IncomeTaxInterestConnector,
                                giftAidConnector: IncomeTaxGiftAidConnector,
                                employmentConnector: IncomeTaxEmploymentConnector,
                                pensionsConnector: IncomeTaxPensionsConnector,
                                cisConnector: IncomeTaxCISConnector,
                                stateBenefitsConnector: IncomeTaxStateBenefitsConnector,
                                interestSavingsConnector: IncomeTaxInterestSavingsConnector,
                                gainsConnector: IncomeTaxGainsConnector,
                                stockDividendsConnector: IncomeTaxStockDividendsConnector,
                                implicit val ec: ExecutionContext) extends Logging {

  type IncomeSourceResponse = Either[APIErrorModel, IncomeSources]


  private def handleUnavailableService(service: String, data: Either[APIErrorModel, _])(implicit hc: HeaderCarrier): (String, APIErrorBody) = {
    val correlationId = hc.extraHeaders.find(_._1 == "X-Correlation-Id")
    data.fold(
      error => {
        logger.error(
          s"[GetIncomeSourcesService][handleUnavailableService] $service has responded with status: ${error.status} with correlation id: $correlationId"
        )
        error.toJson.validate[APIErrorBodyModel].fold(
          _ => {
            logger.error(s"[GetIncomeSourcesService][handleUnavailableService] Error Json validation failed: ${error.body} with correlation id: $correlationId")
            (service, APIErrorBodyModel("INTERNAL_SERVER_ERROR", APIErrorBodyModel.parsingError.reason))
          },
          valid => {
            logger.info(s"[GetIncomeSourcesService][handleUnavailableService] Passed validation, response: $valid")
            (service, valid)
          }
        )
      },
      _ => ("remove", APIErrorModel(0, APIErrorBodyModel.parsingError).body)
    )
  }

  def getAllIncomeSources(nino: String, taxYear: Int, mtditid: String, excludedIncomeSources: Seq[String] = Seq())
                         (implicit hc: HeaderCarrier): Future[TaskListResponse] = {
    /**
     * retrieve all income sources
     *  - check if data exist
     *  - compare the sections with Tailoring Tasklist section
     *  - populate missing sections
     *  - Need to derive status as well here (in particular CHECK status)
     *  - we need to think about CIP explicit audit events
     *  - currently the overview page allows to work on other journeys even if they are under maintenance - do we support this?
     *  - explore refresh cache service? what is the purpose of this?
     *  - Think about unique document identifier for tasklist
     *  - if any service is not available then we create the SECTION and link will be disabled with UNAVAILABLE STATUS
     *    - What is parial data from tailoring service is available and only thing we dont know is , if there need to be
     *      additional link, based on existing data
     *      EXAMPLE PENSIONS
     *      STATE PENSION (user did not select this) - if service is up and data available then this is "CHECK NOW"
     *      OTHER UK PENSION
     *      UNAUTHORISED PAYMENT
     *      SHORT SERVICE REFUNDS
     *      NON UK PENSION
     *      => In above if the user did n;t select STATE PENSION, But based on downstream data - STATE PENSION LINK was
             *      suppose to be populated, but as the service is down we dont know if this would be there or not
             *      however based on tailoring data, we know below 4 link needs to be displayed
                   OTHER UK PENSION
                   UNAUTHORISED PAYMENT
                   SHORT SERVICE REFUNDS
                   NON UK PENSION
                    "Downstream unavaiable"
     *
     * Under maintenance status, should stop user from continuing the journey.
     * what happens currenlty when some journye is under maintenance and user clicks on continue or user had not ansered all the journeys
     */
    for {
      dividends <- getDividends(nino, taxYear, mtditid, excludedIncomeSources)//
      interest <- getInterest(nino, taxYear, mtditid, excludedIncomeSources)
      giftAid <- getGiftAid(nino, taxYear, mtditid, excludedIncomeSources)
      employment <- getEmployment(nino, taxYear, mtditid, excludedIncomeSources)
      pensions <- getPensions(nino, taxYear, mtditid, excludedIncomeSources)
      cis <- getCIS(nino, taxYear, mtditid, excludedIncomeSources)
      stateBenefits <- getStateBenefits(nino, taxYear, mtditid, excludedIncomeSources)
      interestSavings <- getSavingsInterest(nino, taxYear, mtditid, excludedIncomeSources)
      gains <- getGains(nino, taxYear, mtditid, excludedIncomeSources)
      stockDividends <- getStockDividends(nino, taxYear, mtditid, excludedIncomeSources)
      tailoring <- getTailoringTaskList(nino, taxYear, mtditid, excludedIncomeSources)
    } yield {
      Right(
        IncomeSources(
          Some(Seq(
            handleUnavailableService(common.IncomeSources.DIVIDENDS, dividends),
            handleUnavailableService(common.IncomeSources.INTEREST, interest),
            handleUnavailableService(common.IncomeSources.GIFT_AID, giftAid),
            handleUnavailableService(common.IncomeSources.EMPLOYMENT, employment),
            handleUnavailableService(common.IncomeSources.PENSIONS, pensions),
            handleUnavailableService(common.IncomeSources.CIS, cis),
            handleUnavailableService(common.IncomeSources.STATE_BENEFITS, stateBenefits),
            handleUnavailableService(common.IncomeSources.INTEREST_SAVINGS, interestSavings),
            handleUnavailableService(common.IncomeSources.GAINS, gains),
            handleUnavailableService(common.IncomeSources.STOCK_DIVIDENDS, stockDividends)
          ).filter(elem => elem._1 != "remove")),
          dividends.fold(_ => Some(Dividends(None, None)), data => data),
          interest.fold(_ => Some(Seq(Interest("", "", Some(0), Some(0)))), data => data),
          giftAid.fold(_ => Some(GiftAid(None, None)), data => data),
          employment.fold(_ => Some(AllEmploymentData(Seq.empty, None, Seq.empty, None, None)), data => data.map(_.excludePensionIncome())),
          pensions.fold(_ => Some(Pensions(None, None, None, None, None)), data => data.map(_.copy(
            employmentPensions = employment.fold(_ => None, data => data.map(_.buildEmploymentPensions()))
          ))),
          cis.fold(_ => Some(AllCISDeductions(Some(CISSource(None, None, None, Seq.empty)), None)), data => data),
          stateBenefits.fold(_ => Some(AllStateBenefitsData(None)), data => data),
          interestSavings.fold(_ => Some(SavingsIncomeDataModel(None, None, None)), data => data),
          gains.fold(_ => Some(InsurancePoliciesModel(None, None, None, None, None, None)), data => data),
          stockDividends.fold(_ => Some(StockDividends(None, None, None, None)), data => data)
        )
      )
    }
  }

  def getGiftAid(nino: String, taxYear: Int, mtditid: String, excludedIncomeSources: Seq[String] = Seq())
                (implicit hc: HeaderCarrier): Future[Either[APIErrorModel, Option[GiftAid]]] = {

    if (excludedIncomeSources.contains(GIFT_AID)) {
      shutteredIncomeSourceLog(GIFT_AID)
      Future(Right(None))
    } else {
      giftAidConnector.getSubmittedGiftAid(nino, taxYear)(hc.withExtraHeaders(("mtditid", mtditid)))
    }
  }

  def getEmployment(nino: String, taxYear: Int, mtditid: String, excludedIncomeSources: Seq[String] = Seq())
                   (implicit hc: HeaderCarrier): Future[Either[APIErrorModel, Option[AllEmploymentData]]] = {

    if (excludedIncomeSources.contains(EMPLOYMENT)) {
      shutteredIncomeSourceLog(EMPLOYMENT)
      Future(Right(None))
    } else {
      employmentConnector.getSubmittedEmployment(nino, taxYear)(hc.withExtraHeaders(("mtditid", mtditid)))
    }
  }

  def getDividends(nino: String, taxYear: Int, mtditid: String, excludedIncomeSources: Seq[String] = Seq())
                  (implicit hc: HeaderCarrier): Future[Either[APIErrorModel, Option[Dividends]]] = {

    if (excludedIncomeSources.contains(DIVIDENDS)) {
      shutteredIncomeSourceLog(DIVIDENDS)
      Future(Right(None))
    } else {
      dividendsConnector.getSubmittedDividends(nino, taxYear)(hc.withExtraHeaders(("mtditid", mtditid)))
    }
  }

  def getInterest(nino: String, taxYear: Int, mtditid: String, excludedIncomeSources: Seq[String] = Seq())
                 (implicit hc: HeaderCarrier): Future[Either[APIErrorModel, Option[List[Interest]]]] = {

    if (excludedIncomeSources.contains(INTEREST)) {
      shutteredIncomeSourceLog(INTEREST)
      Future(Right(None))
    } else {
      interestConnector.getSubmittedInterest(nino, taxYear)(hc.withExtraHeaders(("mtditid", mtditid)))
    }
  }

  def getPensions(nino: String, taxYear: Int, mtditid: String, excludedIncomeSources: Seq[String] = Seq())
                 (implicit hc: HeaderCarrier): Future[Either[APIErrorModel, Option[Pensions]]] = {

    if (excludedIncomeSources.contains(PENSIONS)) {
      shutteredIncomeSourceLog(PENSIONS)
      Future(Right(None))
    } else {
      pensionsConnector.getSubmittedPensions(nino, taxYear)(hc.withExtraHeaders(("mtditid", mtditid)))
    }
  }

  def getCIS(nino: String, taxYear: Int, mtditid: String, excludedIncomeSources: Seq[String] = Seq())
            (implicit hc: HeaderCarrier): Future[Either[APIErrorModel, Option[AllCISDeductions]]] = {

    if (excludedIncomeSources.contains(CIS)) {
      shutteredIncomeSourceLog(CIS)
      Future(Right(None))
    } else {
      cisConnector.getSubmittedCIS(nino, taxYear)(hc.withExtraHeaders(("mtditid", mtditid)))
    }
  }

  def getStateBenefits(nino: String, taxYear: Int, mtditid: String, excludedIncomeSources: Seq[String] = Seq())
                      (implicit hc: HeaderCarrier): Future[Either[APIErrorModel, Option[AllStateBenefitsData]]] = {
    if (excludedIncomeSources.contains(STATE_BENEFITS)) {
      shutteredIncomeSourceLog(STATE_BENEFITS)
      Future(Right(None))
    } else {
      stateBenefitsConnector.getSubmittedStateBenefits(nino, taxYear)(hc.withExtraHeaders(("mtditid", mtditid)))
    }
  }

  def getSavingsInterest(nino: String, taxYear: Int, mtditid: String, excludedIncomeSources: Seq[String] = Seq())
                        (implicit hc: HeaderCarrier): Future[Either[APIErrorModel, Option[SavingsIncomeDataModel]]] = {
    if (excludedIncomeSources.contains(INTEREST_SAVINGS)) {
      shutteredIncomeSourceLog(INTEREST_SAVINGS)
      Future(Right(None))
    } else {
      interestSavingsConnector.getSubmittedInterestSavings(nino, taxYear)(hc.withExtraHeaders(("mtditid", mtditid)))
    }
  }

  def getGains(nino: String, taxYear: Int, mtditid: String, excludedIncomeSources: Seq[String] = Seq())
              (implicit hc: HeaderCarrier): Future[Either[APIErrorModel, Option[InsurancePoliciesModel]]] = {
    if (excludedIncomeSources.contains(GAINS)) {
      shutteredIncomeSourceLog(GAINS)
      Future(Right(None))
    } else {
      gainsConnector.getSubmittedGains(nino, taxYear)(hc.withExtraHeaders(("mtditid", mtditid)))
    }
  }

  def getStockDividends(nino: String, taxYear: Int, mtditid: String, excludedIncomeSources: Seq[String] = Seq())
                  (implicit hc: HeaderCarrier): Future[Either[APIErrorModel, Option[StockDividends]]] = {

    if (excludedIncomeSources.contains(STOCK_DIVIDENDS)) {
      shutteredIncomeSourceLog(STOCK_DIVIDENDS)
      Future(Right(None))
    } else {
      stockDividendsConnector.getSubmittedStockDividends(nino, taxYear)(hc.withExtraHeaders(("mtditid", mtditid)))
    }
  }

  def shutteredIncomeSourceLog(source: String): Unit = {
    logger.info(s"Income source $source is currently shuttered. Not retrieving data for $source.")
  }
}
