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

import com.codahale.metrics.SharedMetricRegistries
import connectors.httpParsers.SubmittedDividendsParser.{IncomeSourcesResponseModel => IncomeSourceResponseDividends}
import connectors.httpParsers.SubmittedInterestParser.{IncomeSourcesResponseModel => IncomeSourceResponseInterest}
import connectors.{IncomeTaxDividendsConnector, IncomeTaxEmploymentConnector, IncomeTaxGiftAidConnector, IncomeTaxInterestConnector}
import models._
import models.giftAid.{GiftAidPaymentsModel, GiftsModel, GiftAidModel}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtils

import scala.concurrent.Future

class GetIncomeSourcesServiceSpec extends TestUtils {
  SharedMetricRegistries.clear()

  val dividendsConnector: IncomeTaxDividendsConnector = mock[IncomeTaxDividendsConnector]
  val interestConnector: IncomeTaxInterestConnector = mock[IncomeTaxInterestConnector]
  val giftsConnector: IncomeTaxGiftAidConnector = mock[IncomeTaxGiftAidConnector]
  val employmentConnector: IncomeTaxEmploymentConnector = mock[IncomeTaxEmploymentConnector]
  val service: GetIncomeSourcesService = new GetIncomeSourcesService(dividendsConnector, interestConnector,
    giftsConnector, employmentConnector, scala.concurrent.ExecutionContext.global)
  val mockHeaderCarrier: HeaderCarrier = emptyHeaderCarrier.withExtraHeaders(("mtditid", "87654321"))


  ".getAllIncomeSources" when {

    "the income sources are off" should {
      "return an empty response" in {
        val result = await(service.getAllIncomeSources("12345678", 1234, "87654321", Seq("dividends","interest","gift-aid","employment")))

        result mustBe Right(IncomeSourcesResponseModel(None,None,None,None))
      }
    }

    "there are no errors" should {

      "return an IncomeSourceResponseModel with existing dividends, interest and giftaid and employment" in {

        val expectedDividendsResult: IncomeSourceResponseDividends = Right(Some(DividendsModel(Some(12345.67), Some(12345.67))))
        val expectedInterestResult: IncomeSourceResponseInterest = Right(Some(List(
          InterestModel("someName", "123", Some(1234.56), Some(1234.56))
        )))

        val giftAidPayments: GiftAidPaymentsModel = GiftAidPaymentsModel(Some(List("")), Some(12345.67), Some(12345.67), Some(12345.67), Some(12345.67), Some(12345.67))
        val gifts: GiftsModel = GiftsModel(Some(List("someName")), Some(12345.67), Some(12345.67) , Some(12345.67))

        val incomeSourcesResult = Right(IncomeSourcesResponseModel(Some(DividendsModel(Some(12345.67), Some(12345.67))),
          Some(List(InterestModel("someName", "123", Some(1234.56), Some(1234.56)))),Some(GiftAidModel(Some(giftAidPayments), Some(gifts))),
          Some(allEmploymentData)))


        (dividendsConnector.getSubmittedDividends(_: String, _: Int)(_: HeaderCarrier))
          .expects("12345678", 1234, mockHeaderCarrier)
          .returning(Future.successful(expectedDividendsResult))

        (interestConnector.getSubmittedInterest(_: String, _: Int)(_: HeaderCarrier))
          .expects("12345678", 1234, mockHeaderCarrier)
          .returning(Future.successful(expectedInterestResult))

        (employmentConnector.getSubmittedEmployment(_: String, _: Int)(_: HeaderCarrier))
          .expects("12345678", 1234, mockHeaderCarrier)
          .returning(Future.successful(Right(Some(allEmploymentData))))

        (giftsConnector.getSubmittedGiftAid(_: String, _: Int)(_: HeaderCarrier))
          .expects("12345678", 1234, mockHeaderCarrier)
          .returning(Future.successful(Right(Some(GiftAidModel(Some(giftAidPayments), Some(gifts))))))

        val result = await(service.getAllIncomeSources("12345678", 1234, "87654321"))

        result mustBe incomeSourcesResult

      }

    }
  }

    "when there are errors" should {

      "return an InternalServerError" in {

        val errorModel: APIErrorModel = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong"))
        val expectedDividendsResult: IncomeSourceResponseDividends = Right(Some(DividendsModel(Some(12345.67), Some(12345.67))))

        (dividendsConnector.getSubmittedDividends(_: String, _: Int)(_: HeaderCarrier))
        .expects("12345678", 1234, mockHeaderCarrier)
        .returning(Future.successful(expectedDividendsResult))

        (interestConnector.getSubmittedInterest(_: String, _: Int)(_: HeaderCarrier))
        .expects("12345678", 1234, mockHeaderCarrier)
        .returning(Future.successful(Left(errorModel)))

        val result = await(service.getAllIncomeSources("12345678", 1234, "87654321"))

        result mustBe Left(errorModel)

    }
  }
}
