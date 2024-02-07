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

import builders.models.SavingsIncomeBuilder.anSavingIncome
import builders.models.StockDividendsBuilder.anStockDividends
import builders.models.cis.AllCISDeductionsBuilder.anAllCISDeductions
import builders.models.employment.AllEmploymentDataBuilder.{anAllEmploymentData, anAllEmploymentDataWithOccPen}
import builders.models.gains.GainsBuilder.anGains
import builders.models.pensions.PensionsBuilder.aPensionsWithEmployments
import builders.models.statebenefits.AllStateBenefitsDataBuilder.anAllStateBenefitsData
import com.codahale.metrics.SharedMetricRegistries
import connectors._
import connectors.parsers.SubmittedDividendsParser.{IncomeSourcesResponseModel => IncomeSourceResponseDividends}
import models._
import models.gains.InsurancePoliciesModel
import models.gifts.{GiftAid, GiftAidPayments, Gifts}
import models.pensions.Pensions
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtils

import scala.concurrent.Future

class GetIncomeSourcesServiceSpec extends TestUtils {
  SharedMetricRegistries.clear()

  private val taxYear = 2021

  private val dividendsConnector: IncomeTaxDividendsConnector = mock[IncomeTaxDividendsConnector]
  private val interestConnector: IncomeTaxInterestConnector = mock[IncomeTaxInterestConnector]
  private val giftsConnector: IncomeTaxGiftAidConnector = mock[IncomeTaxGiftAidConnector]
  private val employmentConnector: IncomeTaxEmploymentConnector = mock[IncomeTaxEmploymentConnector]
  private val pensionsConnector: IncomeTaxPensionsConnector = mock[IncomeTaxPensionsConnector]
  private val cisConnector: IncomeTaxCISConnector = mock[IncomeTaxCISConnector]
  private val stateBenefitsConnector: IncomeTaxStateBenefitsConnector = mock[IncomeTaxStateBenefitsConnector]
  private val incomeTaxInterestSavingsConnector: IncomeTaxInterestSavingsConnector = mock[IncomeTaxInterestSavingsConnector]
  private val gainsConnector: IncomeTaxGainsConnector = mock[IncomeTaxGainsConnector]
  private val stockDividendsConnector: IncomeTaxStockDividendsConnector = mock[IncomeTaxStockDividendsConnector]
  private val mockHeaderCarrier: HeaderCarrier = emptyHeaderCarrier.withExtraHeaders(("mtditid", "87654321"))

  private val underTest: GetIncomeSourcesService = new GetIncomeSourcesService(
    dividendsConnector,
    interestConnector,
    giftsConnector,
    employmentConnector,
    pensionsConnector,
    cisConnector,
    stateBenefitsConnector,
    incomeTaxInterestSavingsConnector,
    gainsConnector,
    stockDividendsConnector,
    scala.concurrent.ExecutionContext.global
  )

  ".getAllIncomeSources" when {
    "the income sources are off" should {
      "return an empty response" in {
        val eventualResponse = underTest.getAllIncomeSources(
          nino = "12345678",
          taxYear = taxYear,
          mtditid = "87654321",
          excludedIncomeSources = Seq(
            "dividends",
            "interest",
            "gift-aid",
            "employment",
            "pensions",
            "cis",
            "state-benefits",
            "interest-savings",
            "gains",
            "stock-dividends")
        )

        await(eventualResponse) mustBe Right(IncomeSources(Some(List()), None, None, None, None, None, None, None))
      }
    }

    "there are no errors" should {
      "return an IncomeSourceResponseModel with existing dividends, interest and gift aid, employment and pensions" in {
        val expectedDividendsResult = Right(Some(Dividends(Some(12345.67), Some(12345.67))))
        val expectedInterestResult = Right(Some(List(Interest("someName", "123", Some(1234.56), Some(1234.56)))))

        val giftAidPayments = GiftAidPayments(Some(List("")), Some(12345.67), Some(12345.67), Some(12345.67), Some(12345.67), Some(12345.67))
        val gifts: Gifts = Gifts(Some(List("someName")), Some(12345.67), Some(12345.67), Some(12345.67))

        val incomeSourcesResult = Right(IncomeSources(
          Some(List()),
          Some(Dividends(Some(12345.67), Some(12345.67))),
          Some(List(Interest("someName", "123", Some(1234.56), Some(1234.56)))),
          Some(GiftAid(Some(giftAidPayments), Some(gifts))),
          Some(anAllEmploymentData),
          Some(aPensionsWithEmployments),
          Some(anAllCISDeductions),
          Some(anAllStateBenefitsData),
          Some(anSavingIncome),
          Some(anGains),
          Some(anStockDividends)
        ))

        (dividendsConnector.getSubmittedDividends(_: String, _: Int)(_: HeaderCarrier))
          .expects("12345678", taxYear, mockHeaderCarrier)
          .returning(Future.successful(expectedDividendsResult))

        (interestConnector.getSubmittedInterest(_: String, _: Int)(_: HeaderCarrier))
          .expects("12345678", taxYear, mockHeaderCarrier)
          .returning(Future.successful(expectedInterestResult))

        (employmentConnector.getSubmittedEmployment(_: String, _: Int)(_: HeaderCarrier))
          .expects("12345678", taxYear, mockHeaderCarrier)
          .returning(Future.successful(Right(Some(anAllEmploymentDataWithOccPen))))

        (giftsConnector.getSubmittedGiftAid(_: String, _: Int)(_: HeaderCarrier))
          .expects("12345678", taxYear, mockHeaderCarrier)
          .returning(Future.successful(Right(Some(GiftAid(Some(giftAidPayments), Some(gifts))))))

        (pensionsConnector.getSubmittedPensions(_: String, _: Int)(_: HeaderCarrier))
          .expects("12345678", taxYear, mockHeaderCarrier)
          .returning(Future.successful(Right(Some(aPensionsWithEmployments))))

        (cisConnector.getSubmittedCIS(_: String, _: Int)(_: HeaderCarrier))
          .expects("12345678", taxYear, mockHeaderCarrier)
          .returning(Future.successful(Right(Some(anAllCISDeductions))))

        (stateBenefitsConnector.getSubmittedStateBenefits(_: String, _: Int)(_: HeaderCarrier))
          .expects("12345678", taxYear, mockHeaderCarrier)
          .returning(Future.successful(Right(Some(anAllStateBenefitsData))))

        (incomeTaxInterestSavingsConnector.getSubmittedInterestSavings(_: String, _: Int)(_: HeaderCarrier))
          .expects("12345678", taxYear, mockHeaderCarrier)
          .returning(Future.successful(Right(Some(anSavingIncome))))

        (gainsConnector.getSubmittedGains(_: String, _: Int)(_: HeaderCarrier))
          .expects("12345678", taxYear, mockHeaderCarrier)
          .returning(Future.successful(Right(Some(anGains))))

        (stockDividendsConnector.getSubmittedStockDividends(_: String, _: Int)(_: HeaderCarrier))
          .expects("12345678", taxYear, mockHeaderCarrier)
          .returning(Future.successful(Right(Some(anStockDividends))))

        await(underTest.getAllIncomeSources("12345678", taxYear, "87654321")) mustBe incomeSourcesResult
      }
    }

    "when there are errors" should {
      "return an InternalServerError" in {
        val errorModel: APIErrorModel = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong"))
        val expectedDividendsResult: IncomeSourceResponseDividends = Right(Some(Dividends(Some(12345.67), Some(12345.67))))

        val incomeSourcesResult = Right(IncomeSources(
          Some(List(("interest", errorModel.body), ("gift-aid", errorModel.body), ("pensions", errorModel.body),
            ("interest-savings", errorModel.body), ("gains", errorModel.body), ("stock-dividends", errorModel.body))),
          Some(Dividends(Some(12345.67), Some(12345.67))),
          Some(List(Interest("", "", Some(0), Some(0)))),
          Some(GiftAid(None, None)),
          Some(anAllEmploymentData),
          Some(Pensions(None, None, None, None, None)),
          Some(anAllCISDeductions),
          Some(anAllStateBenefitsData),
          Some(SavingsIncomeDataModel(None, None, None)),
          Some(InsurancePoliciesModel(None, None, None, None, None, None)),
          Some(StockDividends(None, None, None, None, None, None))
        ))

        (dividendsConnector.getSubmittedDividends(_: String, _: Int)(_: HeaderCarrier))
          .expects("12345678", taxYear, mockHeaderCarrier)
          .returning(Future.successful(expectedDividendsResult))

        (interestConnector.getSubmittedInterest(_: String, _: Int)(_: HeaderCarrier))
          .expects("12345678", taxYear, mockHeaderCarrier)
          .returning(Future(Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong")))))

        (employmentConnector.getSubmittedEmployment(_: String, _: Int)(_: HeaderCarrier))
          .expects("12345678", taxYear, mockHeaderCarrier)
          .returning(Future.successful(Right(Some(anAllEmploymentDataWithOccPen))))

        (giftsConnector.getSubmittedGiftAid(_: String, _: Int)(_: HeaderCarrier))
          .expects("12345678", taxYear, mockHeaderCarrier)
          .returning(Future.successful(Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong")))))

        (pensionsConnector.getSubmittedPensions(_: String, _: Int)(_: HeaderCarrier))
          .expects("12345678", taxYear, mockHeaderCarrier)
          .returning(Future.successful(Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong")))))

        (cisConnector.getSubmittedCIS(_: String, _: Int)(_: HeaderCarrier))
          .expects("12345678", taxYear, mockHeaderCarrier)
          .returning(Future.successful(Right(Some(anAllCISDeductions))))

        (stateBenefitsConnector.getSubmittedStateBenefits(_: String, _: Int)(_: HeaderCarrier))
          .expects("12345678", taxYear, mockHeaderCarrier)
          .returning(Future.successful(Right(Some(anAllStateBenefitsData))))

        (incomeTaxInterestSavingsConnector.getSubmittedInterestSavings(_: String, _: Int)(_: HeaderCarrier))
          .expects("12345678", taxYear, mockHeaderCarrier)
          .returning(Future.successful(Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong")))))

        (gainsConnector.getSubmittedGains(_: String, _: Int)(_: HeaderCarrier))
          .expects("12345678", taxYear, mockHeaderCarrier)
          .returning(Future.successful(Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong")))))

        (stockDividendsConnector.getSubmittedStockDividends(_: String, _: Int)(_: HeaderCarrier))
          .expects("12345678", taxYear, mockHeaderCarrier)
          .returning(Future.successful(Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong")))))

        await(underTest.getAllIncomeSources("12345678", taxYear, "87654321")) mustBe incomeSourcesResult
      }
    }
  }
}
