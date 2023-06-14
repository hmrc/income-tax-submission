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

package controllers


import builders.models.IncomeSourcesBuilder.anIncomeSources
import builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import models._
import models.gifts.{GiftAid, GiftAidPayments, Gifts}
import org.scalamock.handlers.{CallHandler3, CallHandler5}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results._
import play.api.test.FakeRequest
import services.{GetIncomeSourcesService, IncomeTaxUserDataService, RefreshCacheService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtils

import scala.concurrent.{ExecutionContext, Future}

class IncomeSourcesControllerSpec extends TestUtils {

  private val mockIncomeTaxUserDataService = mock[IncomeTaxUserDataService]
  private val mockRefreshCacheService = mock[RefreshCacheService]

  def mockSaveData(data: Option[IncomeSources],
                   outcome: Result): CallHandler5[Int, Option[IncomeSources], Result, User[_], ExecutionContext, Future[Result]] = {
    (mockIncomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSources])(_: Result)(_: User[_], _: ExecutionContext))
      .expects(*, data, *, *, *)
      .returning(Future.successful(outcome))
  }

  def mockFindData(data: Either[APIErrorModel, Option[IncomeSources]]
                  ): CallHandler3[User[_], Int, ExecutionContext, Future[Either[APIErrorModel, Option[IncomeSources]]]] = {
    (mockIncomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
      .expects(*, *, *)
      .returning(Future.successful(data))
  }

  val getIncomeSourcesService: GetIncomeSourcesService = mock[GetIncomeSourcesService]
  val controller = new IncomeSourcesController(getIncomeSourcesService, mockIncomeTaxUserDataService,
    mockRefreshCacheService, mockControllerComponents, authorisedAction)
  val nino: String = "123456789"
  val taxYear: Int = 1234
  private val fakeGetRequestWithHeaderAndSession = FakeRequest("GET",
    "/income-tax-submission-service/income-tax/nino/AA123456A/sources?taxYear=2022").withHeaders("mtditid" -> "1234567890", "sessionId" -> "sessionId")
  private val fakeGetRequestWithExcludedHeader = FakeRequest("GET",
    "/income-tax-submission-service/income-tax/nino/AA123456A/sources?taxYear=2022").withHeaders("mtditid" -> "1234567890",
    "excluded-income-sources" -> "dividends,interest,gift-aid,employment", "sessionId" -> "sessionId")
  private val fakeGetRequestWithoutHeader = FakeRequest("GET",
    "/income-tax-submission-service/income-tax/nino/AA123456A/sources?taxYear=2022")

  def mockGetIncomeSourcesTurnedOff(): CallHandler5[String, Int, String, Seq[String], HeaderCarrier, Future[getIncomeSourcesService.IncomeSourceResponse]] = {
    (getIncomeSourcesService.getAllIncomeSources(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
      .expects(*, *, *, Seq("dividends", "interest", "gift-aid", "employment"), *)
      .returning(Future.successful(Right(IncomeSources(None, None, None, None, otherEmploymentIncome = None))))
  }

  val giftAidPayments: GiftAidPayments = {
    GiftAidPayments(Some(List("")), Some(12345.67), Some(12345.67), Some(12345.67), Some(12345.67), Some(12345.67))
  }
  val gifts: Gifts = Gifts(Some(List("")), Some(12345.67), Some(12345.67), Some(12345.67))

  val incomeSources: IncomeSources = IncomeSources(None, Some(Dividends(Some(12345.67), Some(12345.67))), Some(Seq(Interest("someName", "12345", Some(12345.67), Some(12345.67)))), Some(GiftAid(Some(giftAidPayments), Some(gifts))), Some(anAllEmploymentData), otherEmploymentIncome = None)

  def mockGetIncomeSourcesValid(): CallHandler5[String, Int, String, Seq[String], HeaderCarrier, Future[getIncomeSourcesService.IncomeSourceResponse]] = {
    (getIncomeSourcesService.getAllIncomeSources(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
      .expects(*, *, *, Seq(), *)
      .returning(Future.successful(Right(incomeSources)))
  }

  def mockGetIncomeSourcesInvalid(): CallHandler5[String, Int, String, Seq[String], HeaderCarrier, Future[getIncomeSourcesService.IncomeSourceResponse]] = {
    val invalidIncomeSource: APIErrorModel = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong"))
    (getIncomeSourcesService.getAllIncomeSources(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
      .expects(*, *, *, Seq(), *)
      .returning(Future.successful(Left(invalidIncomeSource)))
  }

  "calling .getIncomeSourcesFromSession" should {
    "with data populated" should {
      "return a NO_CONTENT response with no data" in {
        val result = {
          mockAuth()
          mockFindData(Right(None))
          controller.getIncomeSourcesFromSession(nino, taxYear)(fakeGetRequestWithExcludedHeader)
        }
        status(result) mustBe NO_CONTENT
      }

      "return an OK response with data" in {
        val result = {
          mockAuth()
          mockFindData(Right(Some(anIncomeSources)))
          controller.getIncomeSourcesFromSession(nino, taxYear)(fakeGetRequestWithExcludedHeader)
        }
        status(result) mustBe OK
        Json.parse(bodyOf(result)) mustBe Json.toJson(anIncomeSources)
      }

      "return a NO_CONTENT response when data is empty" in {
        val result = {
          mockAuth()
          mockFindData(Right(Some(IncomeSources(otherEmploymentIncome = None))))
          controller.getIncomeSourcesFromSession(nino, taxYear)(fakeGetRequestWithExcludedHeader)
        }
        status(result) mustBe NO_CONTENT
      }
      "return an INTERNAL SERVER ERROR response when an error occurs" in {
        val result = {
          mockAuth()
          mockFindData(Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)))
          controller.getIncomeSourcesFromSession(nino, taxYear)(fakeGetRequestWithExcludedHeader)
        }
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "calling .refreshIncomeSource" should {
    "return a 204 response when it refreshed the cache for dividends" in {
      val result: Future[Result] = {
        mockAuth()
        (mockRefreshCacheService.getLatestDataAndRefreshCache(_: Int, _: String)(_: User[_], _: HeaderCarrier, _: ExecutionContext))
          .expects(taxYear, "dividends", *, *, *)
          .returning(Future.successful(NoContent))

        controller.refreshIncomeSource(nino, taxYear)(fakeGetRequestWithHeaderAndSession.withJsonBody(Json.toJson(RefreshIncomeSource("dividends"))))
      }
      status(result) mustBe NO_CONTENT
    }
    "return a 400 response when no body" in {
      val result: Future[Result] = {
        mockAuth()

        controller.refreshIncomeSource(nino, taxYear)(fakeGetRequestWithHeaderAndSession)
      }
      status(result) mustBe BAD_REQUEST
    }
  }

  "calling .getIncomeSources" should {

    "with either existing dividend, interest or giftaid" should {

      "return a 204 response when sources are turned off" in {
        val result = {
          mockAuth()
          mockGetIncomeSourcesTurnedOff()
          mockSaveData(None, NoContent)
          controller.getIncomeSources(nino, taxYear)(fakeGetRequestWithExcludedHeader)
        }
        status(result) mustBe NO_CONTENT
      }

      "return an OK 200 response when called as an individual" in {
        val result = {
          mockAuth()
          mockGetIncomeSourcesValid()
          mockSaveData(Some(incomeSources), Ok(Json.toJson(incomeSources)))
          controller.getIncomeSources(nino, taxYear)(fakeGetRequestWithHeaderAndSession)
        }
        status(result) mustBe OK
        Json.parse(bodyOf(result)) mustBe Json.toJson(incomeSources)
      }

      "return an OK 200 response when called as an agent" in {
        val result = {
          mockAuthAsAgent()
          mockGetIncomeSourcesValid()
          mockSaveData(Some(incomeSources), Ok(Json.toJson(incomeSources)))
          controller.getIncomeSources(nino, taxYear)(fakeGetRequestWithHeaderAndSession)
        }
        status(result) mustBe OK
        Json.parse(bodyOf(result)) mustBe Json.toJson(incomeSources)
      }
    }

    "without existing dividend, interest and giftaid" should {
      "return an InternalServerError response when called as an individual" in {
        val result = {
          mockAuth()
          mockGetIncomeSourcesInvalid()
          controller.getIncomeSources(nino, taxYear)(fakeGetRequestWithHeaderAndSession)
        }
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "return an InternalServerError response when called as an agent" in {
        val result = {
          mockAuthAsAgent()
          mockGetIncomeSourcesInvalid()
          controller.getIncomeSources(nino, taxYear)(fakeGetRequestWithHeaderAndSession)
        }
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "without mtditid present in header" should {
      "return an UNAUTHORIZED 401 response when called as an individual" in {
        val result = {
          controller.getIncomeSources(nino, taxYear)(fakeGetRequestWithoutHeader)
        }
        status(result) mustBe UNAUTHORIZED
      }

      "return an UNAUTHORIZED 401 response when called as an agent" in {
        val result = {
          controller.getIncomeSources(nino, taxYear)(fakeGetRequestWithoutHeader)
        }
        status(result) mustBe UNAUTHORIZED
      }
    }
  }
}
