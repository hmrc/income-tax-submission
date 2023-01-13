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
import models.cis.AllCISDeductions
import models.employment.AllEmploymentData
import models.gifts.GiftAid
import models.pensions.Pensions
import models.statebenefits.AllStateBenefitsData
import play.api.Logging
import services.util.FutureEitherOps
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GetIncomeSourcesService @Inject()(dividendsConnector: IncomeTaxDividendsConnector,
                                        interestConnector: IncomeTaxInterestConnector,
                                        giftAidConnector: IncomeTaxGiftAidConnector,
                                        employmentConnector: IncomeTaxEmploymentConnector,
                                        pensionsConnector: IncomeTaxPensionsConnector,
                                        cisConnector: IncomeTaxCISConnector,
                                        stateBenefitsConnector: IncomeTaxStateBenefitsConnector,
                                        interestSavingsConnector: IncomeTaxInterestSavingsConnector,
                                        gainsConnector: IncomeTaxGainsConnector,
                                        implicit val ec: ExecutionContext) extends Logging {

  type IncomeSourceResponse = Either[APIErrorModel, IncomeSources]

  def getAllIncomeSources(nino: String, taxYear: Int, mtditid: String, excludedIncomeSources: Seq[String] = Seq())
                         (implicit hc: HeaderCarrier): Future[IncomeSourceResponse] = {
    (for {
      dividends <- FutureEitherOps[APIErrorModel, Option[Dividends]](getDividends(nino, taxYear, mtditid, excludedIncomeSources))
      interest <- FutureEitherOps[APIErrorModel, Option[Seq[Interest]]](getInterest(nino, taxYear, mtditid, excludedIncomeSources))
      giftAid <- FutureEitherOps[APIErrorModel, Option[GiftAid]](getGiftAid(nino, taxYear, mtditid, excludedIncomeSources))
      employment <- FutureEitherOps[APIErrorModel, Option[AllEmploymentData]](getEmployment(nino, taxYear, mtditid, excludedIncomeSources))
      pensions <- FutureEitherOps[APIErrorModel, Option[Pensions]](getPensions(nino, taxYear, mtditid, excludedIncomeSources))
      cis <- FutureEitherOps[APIErrorModel, Option[AllCISDeductions]](getCIS(nino, taxYear, mtditid, excludedIncomeSources))
      stateBenefits <- FutureEitherOps[APIErrorModel, Option[AllStateBenefitsData]](getStateBenefits(nino, taxYear, mtditid, excludedIncomeSources))
      interestSavings <- FutureEitherOps[APIErrorModel, Option[SavingsIncomeDataModel]](getSavingsInterest(nino, taxYear, mtditid, excludedIncomeSources))
      gains <- FutureEitherOps[APIErrorModel, Option[InsurancePoliciesModel]](getGains(nino, taxYear, mtditid, excludedIncomeSources))
    } yield {
      IncomeSources(
        dividends.map(res => Dividends(res.ukDividends, res.otherUkDividends)),
        interest,
        giftAid,
        employment.map(_.excludePensionIncome()),
        pensions.map(_.copy(
          employmentPensions = employment.map(_.buildEmploymentPensions())
        )),
        cis,
        stateBenefits,
        interestSavings,
        gains
      )
    }).value
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

  def shutteredIncomeSourceLog(source: String): Unit = {
    logger.info(s"Income source $source is currently shuttered. Not retrieving data for $source.")
  }
}
