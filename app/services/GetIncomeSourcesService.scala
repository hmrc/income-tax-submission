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

package services

import connectors.{IncomeTaxDividendsConnector, IncomeTaxEmploymentConnector, IncomeTaxGiftAidConnector, IncomeTaxInterestConnector}
import javax.inject.Inject
import models._
import models.giftAid.GiftAidModel
import services.util.FutureEitherOps
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.{ExecutionContext, Future}
import common.IncomeSources._
import models.employment.frontend.AllEmploymentData
import play.api.Logging

class GetIncomeSourcesService @Inject()(dividendsConnector: IncomeTaxDividendsConnector,
                                        interestConnector: IncomeTaxInterestConnector,
                                        giftAidConnector: IncomeTaxGiftAidConnector,
                                        employmentConnector: IncomeTaxEmploymentConnector,
                                        implicit val ec: ExecutionContext) extends Logging {

  type IncomeSourceResponse = Either[APIErrorModel, IncomeSourcesResponseModel]

  def getAllIncomeSources(nino: String, taxYear: Int, mtditid: String, excludedIncomeSources: Seq[String] = Seq())
                         (implicit hc: HeaderCarrier): Future[IncomeSourceResponse] =  {
    (for {
      dividends <- FutureEitherOps[APIErrorModel, Option[DividendsModel]](getDividends(nino,taxYear,mtditid,excludedIncomeSources))
      interest <- FutureEitherOps[APIErrorModel, Option[Seq[InterestModel]]](getInterest(nino,taxYear,mtditid,excludedIncomeSources))
      giftAid <- FutureEitherOps[APIErrorModel, Option[GiftAidModel]](getGiftAid(nino,taxYear,mtditid,excludedIncomeSources))
      employment <- FutureEitherOps[APIErrorModel, Option[AllEmploymentData]](getEmployment(nino,taxYear,mtditid,excludedIncomeSources))
    } yield {
      IncomeSourcesResponseModel(
        dividends.map(res => DividendsModel(res.ukDividends, res.otherUkDividends)),
        interest,
        giftAid,
        employment
      )
    }).value
  }

  def getGiftAid(nino: String, taxYear: Int, mtditid: String, excludedIncomeSources: Seq[String] = Seq())
                  (implicit hc: HeaderCarrier): Future[Either[APIErrorModel, Option[GiftAidModel]]] = {

    if(excludedIncomeSources.contains(GIFT_AID)){
      shutteredIncomeSourceLog(GIFT_AID)
      Future(Right(None))
    } else {
      giftAidConnector.getSubmittedGiftAid(nino, taxYear)(hc.withExtraHeaders(("mtditid", mtditid)))
    }
  }

  def getEmployment(nino: String, taxYear: Int, mtditid: String, excludedIncomeSources: Seq[String] = Seq())
                   (implicit hc: HeaderCarrier): Future[Either[APIErrorModel, Option[AllEmploymentData]]] = {

    if(excludedIncomeSources.contains(EMPLOYMENT)){
      shutteredIncomeSourceLog(EMPLOYMENT)
      Future(Right(None))
    } else {
      employmentConnector.getSubmittedEmployment(nino, taxYear)(hc.withExtraHeaders(("mtditid", mtditid)))
    }
  }

  def getDividends(nino: String, taxYear: Int, mtditid: String, excludedIncomeSources: Seq[String] = Seq())
                  (implicit hc: HeaderCarrier): Future[Either[APIErrorModel, Option[DividendsModel]]] = {

    if(excludedIncomeSources.contains(DIVIDENDS)){
      shutteredIncomeSourceLog(DIVIDENDS)
      Future(Right(None))
    } else {
      dividendsConnector.getSubmittedDividends(nino, taxYear)(hc.withExtraHeaders(("mtditid", mtditid)))
    }
  }

  def getInterest(nino: String, taxYear: Int, mtditid: String, excludedIncomeSources: Seq[String] = Seq())
                  (implicit hc: HeaderCarrier): Future[Either[APIErrorModel, Option[List[InterestModel]]]] = {

    if(excludedIncomeSources.contains(INTEREST)){
      shutteredIncomeSourceLog(INTEREST)
      Future(Right(None))
    } else {
      interestConnector.getSubmittedInterest(nino, taxYear)(hc.withExtraHeaders(("mtditid", mtditid)))
    }
  }

  def shutteredIncomeSourceLog(source: String): Unit = {
    logger.info(s"Income source $source is currently shuttered. Not retrieving data for $source.")
  }

}
