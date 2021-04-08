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

import connectors.httpParsers.{SubmittedDividendsParser, SubmittedInterestParser}
import connectors.{IncomeTaxDividendsConnector, IncomeTaxInterestConnector}
import javax.inject.Inject
import models._
import services.util.FutureEitherOps
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import common.IncomeSources._
import play.api.Logging

class GetIncomeSourcesService @Inject()(dividendsConnector: IncomeTaxDividendsConnector,
                                        interestConnector: IncomeTaxInterestConnector,
                                        implicit val ec: ExecutionContext) extends Logging {

  type IncomeSourceResponse = Either[APIErrorModel, IncomeSourcesResponseModel]

  def getAllIncomeSources(nino: String, taxYear: Int, mtditid: String, excludedIncomeSources: Seq[String] = Seq())
                         (implicit hc: HeaderCarrier): Future[IncomeSourceResponse] =  {
    (for {
      dividends <- FutureEitherOps[APIErrorModel, Option[SubmittedDividendsModel]](getDividends(nino,taxYear,mtditid,excludedIncomeSources))
      interest <- FutureEitherOps[APIErrorModel, Option[Seq[SubmittedInterestModel]]](getInterest(nino,taxYear,mtditid,excludedIncomeSources))
    } yield {
      IncomeSourcesResponseModel(dividends.map(res => DividendsResponseModel(res.ukDividends, res.otherUkDividends)), interest)
    }).value
  }

  def getDividends(nino: String, taxYear: Int, mtditid: String, excludedIncomeSources: Seq[String])
                  (implicit hc: HeaderCarrier): Future[SubmittedDividendsParser.IncomeSourcesResponseModel] = {

    if(excludedIncomeSources.contains(DIVIDENDS)){
      shutteredIncomeSourceLog(DIVIDENDS)
      Future(Right(None))
    } else {
      dividendsConnector.getSubmittedDividends(nino, taxYear)(hc.withExtraHeaders(("mtditid", mtditid)))
    }
  }

  def getInterest(nino: String, taxYear: Int, mtditid: String, excludedIncomeSources: Seq[String])
                  (implicit hc: HeaderCarrier): Future[SubmittedInterestParser.IncomeSourcesResponseModel] = {

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
