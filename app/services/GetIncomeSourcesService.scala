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

import connectors.{IncomeTaxDividendsConnector, IncomeTaxInterestConnector}
import javax.inject.Inject
import models._
import services.util.FutureEitherOps
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class GetIncomeSourcesService @Inject()(dividendsConnector: IncomeTaxDividendsConnector,
                                        interestConnector: IncomeTaxInterestConnector,
                                        implicit val ec: ExecutionContext) {

  type IncomeSourceResponse = Either[ErrorResponseModel, IncomeSourcesResponseModel]

  def getAllIncomeSources(nino: String, taxYear: Int, mtditid: String)(implicit hc: HeaderCarrier): Future[IncomeSourceResponse] =  {
    (for {
      dividends <- FutureEitherOps[ErrorResponseModel, Option[SubmittedDividendsModel]](dividendsConnector.getSubmittedDividends(nino, taxYear, mtditid))
      interest <- FutureEitherOps[ErrorResponseModel, Option[Seq[SubmittedInterestModel]]](interestConnector.getSubmittedInterest(nino, taxYear, mtditid))
    } yield {
      IncomeSourcesResponseModel(dividends.map(res => DividendsResponseModel(res.ukDividends, res.otherUkDividends)), interest)
    }).value
  }

}
