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

package connectors

import builders.models.SavingsIncomeBuilder.anSavingIncome
import com.github.tomakehurst.wiremock.http.HttpHeader
import models.{APIErrorBodyModel, APIErrorModel, APIErrorsBodyModel, Dividends}
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, SessionId}
import utils.{ConnectorIntegrationTest, MockAppConfig}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class IncomeTaxInterestSavingsConnectorISpec extends ConnectorIntegrationTest {

  private val nino = "AA123123A"
  private val taxYear = 1999
  private val mtditidHeader = ("mtditid", "123123123")
  private val requestHeaders = Seq(new HttpHeader("mtditid", "123123123"))

  private val underTest: IncomeTaxInterestSavingsConnector = new IncomeTaxInterestSavingsConnector(httpClientV2, new MockAppConfig())

  "IncomeTaxInterestSavingsConnector" should {
    val expectedResult = Some(anSavingIncome)
    val responseBody = Json.toJson(expectedResult).toString()
    implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))

    "include internal headers" when {
      val headersSent = Seq(new HttpHeader(HeaderNames.xSessionId, "sessionIdValue"))

      "the host is 'Internal'" in {
        stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/savings\\?taxYear=$taxYear", OK, responseBody, headersSent)

        Await.result(underTest.getSubmittedInterestSavings(nino, taxYear), Duration.Inf) shouldBe Right(expectedResult)
      }

      "the host is 'External'" in {
        stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/savings\\?taxYear=$taxYear", OK, responseBody, headersSent)

        Await.result(underTest.getSubmittedInterestSavings(nino, taxYear), Duration.Inf) shouldBe Right(expectedResult)
      }
    }

    "return a InterestSavingsModel" when {
      "all values are present" in {
        stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/savings\\?taxYear=$taxYear", OK, responseBody, requestHeaders)

        Await.result(underTest.getSubmittedInterestSavings(nino, taxYear), Duration.Inf) shouldBe Right(expectedResult)
      }
    }

    "return a none when no values found" in {
      val responseBody = Json.toJson(Dividends(None, None))
      stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/savings\\?taxYear=$taxYear", OK, responseBody.toString(), requestHeaders)

      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedInterestSavings(nino, taxYear), Duration.Inf) shouldBe Right(None)
    }

    "return a None for not found" in {
      stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/savings\\?taxYear=1999", NOT_FOUND, "{}", requestHeaders)
      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedInterestSavings(nino, taxYear), Duration.Inf) shouldBe Right(None)
    }

    "API Returns multiple errors" in {
      val expectedResult = APIErrorModel(BAD_REQUEST, APIErrorsBodyModel(Seq(
        APIErrorBodyModel("INVALID_IDTYPE", "ID is invalid"),
        APIErrorBodyModel("INVALID_IDTYPE_2", "ID 2 is invalid")
      )))

      val responseBody = Json.obj("failures" -> Json.arr(
        Json.obj("code" -> "INVALID_IDTYPE", "reason" -> "ID is invalid"),
        Json.obj("code" -> "INVALID_IDTYPE_2", "reason" -> "ID 2 is invalid")
      ))
      stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/savings\\?taxYear=1999", BAD_REQUEST, responseBody.toString(), requestHeaders)

      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedInterestSavings(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }

    "return a BadRequest" in {
      val errorBody: APIErrorBodyModel = APIErrorBodyModel("BAD_REQUEST", "That request was bad")
      val expectedResult = APIErrorModel(BAD_REQUEST, errorBody)

      stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/savings\\?taxYear=1999",
        BAD_REQUEST, Json.toJson(errorBody).toString(), requestHeaders)
      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedInterestSavings(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }

    "return an InternalServerError " in {
      val errorBody: APIErrorBodyModel = APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong")
      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, errorBody)

      stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/savings\\?taxYear=1999",
        INTERNAL_SERVER_ERROR, Json.toJson(errorBody).toString(), requestHeaders)
      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedInterestSavings(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }

    "return an InternalServerError due to parsing error" in {
      val invalidJson = Json.obj("securities" -> "")
      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)

      stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/savings\\?taxYear=1999", OK, invalidJson.toString(), requestHeaders)
      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedInterestSavings(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }

    "return an InternalServerError with parsing error when we can't parse the error body" in {
      val errorResponseBody = Json.toJson("INTERNAL_SERVER_ERROR")
      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)

      stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/savings\\?taxYear=1999",
        INTERNAL_SERVER_ERROR, errorResponseBody.toString(), requestHeaders)
      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedInterestSavings(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }

    "return an InternalServerError when an unexpected status is thrown" in {
      val errorResponseBody = Json.toJson(APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong"))
      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong"))

      stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/savings\\?taxYear=1999", IM_A_TEAPOT, errorResponseBody.toString(), requestHeaders)
      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedInterestSavings(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }

    "return an InternalServerError when an unexpected status is thrown and there is no body" in {
      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)

      stubGetWithoutResponseBody(s"/income-tax-interest/income-tax/nino/$nino/savings\\?taxYear=1999", IM_A_TEAPOT)
      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedInterestSavings(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }

    "return a ServiceUnavailableError" in {
      val errorRequestBody = Json.toJson(APIErrorBodyModel("SERVICE_UNAVAILABLE", "Service went down")).toString()
      val expectedResult = APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel("SERVICE_UNAVAILABLE", "Service went down"))

      stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/savings\\?taxYear=1999", SERVICE_UNAVAILABLE, errorRequestBody, requestHeaders)
      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedInterestSavings(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }
  }
}
