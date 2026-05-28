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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results._
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, status}
import services.{GetIncomeSourcesService, IncomeTaxUserDataService, RefreshCacheService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtils

import scala.concurrent.{ExecutionContext, Future}

class IncomeSourcesControllerSpec extends TestUtils {

  private val mockIncomeTaxUserDataService = mock[IncomeTaxUserDataService]
  private val mockRefreshCacheService = mock[RefreshCacheService]

  def mockSaveData(data: Option[IncomeSources], outcome: Result): Unit = {
    when(mockIncomeTaxUserDataService.saveUserData(any[Int], any[Option[IncomeSources]])(any[Result])(anyUser, any[ExecutionContext]))
      .thenReturn(Future.successful(outcome))
  }

  def mockFindData(data: Either[APIErrorModel, Option[IncomeSources]]): Unit = {
    when(mockIncomeTaxUserDataService.findUserData(anyUser, any[Int])(any[ExecutionContext]))
      .thenReturn(Future.successful(data))
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

  def mockGetIncomeSourcesTurnedOff(): Unit = {
    when(getIncomeSourcesService.getAllIncomeSources(any[String], any[Int], any[String], any[Seq[String]])(any[HeaderCarrier]))
      .thenReturn(Future.successful(Right(IncomeSources())))
  }

  val giftAidPayments: GiftAidPayments = {
    GiftAidPayments(Some(List("")), Some(12345.67), Some(12345.67), Some(12345.67), Some(12345.67), Some(12345.67))
  }
  val gifts: Gifts = Gifts(Some(List("")), Some(12345.67), Some(12345.67), Some(12345.67))

  val incomeSources: IncomeSources = IncomeSources(None, Some(Dividends(Some(12345.67), Some(12345.67))),
    Some(Seq(Interest("someName", "12345", Some(12345.67), Some(12345.67)))), Some(GiftAid(Some(giftAidPayments), Some(gifts))), Some(anAllEmploymentData))

  def mockGetIncomeSourcesValid(): Unit = {
    when(getIncomeSourcesService.getAllIncomeSources(any[String], any[Int], any[String], any[Seq[String]])(any[HeaderCarrier]))
      .thenReturn(Future.successful(Right(incomeSources)))
  }

  def mockGetIncomeSourcesInvalid(): Unit = {
    val invalidIncomeSource: APIErrorModel = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong"))
    when(getIncomeSourcesService.getAllIncomeSources(any[String], any[Int], any[String], any[Seq[String]])(any[HeaderCarrier]))
      .thenReturn(Future.successful(Left(invalidIncomeSource)))
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
        contentAsJson(result) mustBe Json.toJson(anIncomeSources)
      }

      "return a NO_CONTENT response when data is empty" in {
        val result = {
          mockAuth()
          mockFindData(Right(Some(IncomeSources())))
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
        when(mockRefreshCacheService.getLatestDataAndRefreshCache(any[Int], any[String])(anyUser, any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.successful(NoContent))

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
        contentAsJson(result) mustBe Json.toJson(incomeSources)
      }

      "return an OK 200 response when called as an agent" in {
        val result = {
          mockAuthAsAgent()
          mockGetIncomeSourcesValid()
          mockSaveData(Some(incomeSources), Ok(Json.toJson(incomeSources)))
          controller.getIncomeSources(nino, taxYear)(fakeGetRequestWithHeaderAndSession)
        }
        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(incomeSources)
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
