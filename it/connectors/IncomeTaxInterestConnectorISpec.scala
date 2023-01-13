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

import com.github.tomakehurst.wiremock.http.HttpHeader
import models.{APIErrorBodyModel, APIErrorModel, Interest}
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, SessionId}
import utils.{ConnectorIntegrationTest, MockAppConfig}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class IncomeTaxInterestConnectorISpec extends ConnectorIntegrationTest {

  private val nino = "AA123123A"
  private val taxYear = 2020
  private val mtditidHeader = ("mtditid", "123123123")
  private val requestHeaders = Seq(new HttpHeader("mtditid", "123123123"))

  private val accountName = "SomeName"
  private val incomeSourceId = "12345"
  private val underTest = new IncomeTaxInterestConnector(httpClient, new MockAppConfig())

  "IncomeTaxInterestConnector" should {
    "include internal headers" when {
      val expectedResult = Some(Seq(Interest(accountName, incomeSourceId, Some(12345.67), Some(12345.67))))
      val responseBody = Json.toJson(expectedResult).toString()
      val headersSentToInterest = Seq(new HttpHeader(HeaderNames.xSessionId, "sessionIdValue"))

      "the host for Interest is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))

        stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear", OK, responseBody, headersSentToInterest)

        Await.result(underTest.getSubmittedInterest(nino, taxYear), Duration.Inf) shouldBe Right(expectedResult)
      }

      "the host for Interest is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))

        stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear", OK, responseBody, headersSentToInterest)

        Await.result(underTest.getSubmittedInterest(nino, taxYear), Duration.Inf) shouldBe Right(expectedResult)
      }
    }

    "return a SubmittedInterestModel" when {
      "all values are present" in {
        val expectedResult = Some(Seq(Interest(accountName, incomeSourceId, Some(12345.67), Some(12345.67))))

        stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
          OK, Json.toJson(expectedResult).toString(), requestHeaders)

        implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

        Await.result(underTest.getSubmittedInterest(nino, taxYear), Duration.Inf) shouldBe Right(expectedResult)
      }
    }

    "return a none when no interest values are found" in {
      stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
        OK, Json.toJson(Seq.empty[Interest]).toString(), requestHeaders)

      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedInterest(nino, taxYear), Duration.Inf) shouldBe Right(None)
    }

    "return a none for a NotFound" in {
      stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear", NOT_FOUND, "{}", requestHeaders)

      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedInterest(nino, taxYear), Duration.Inf) shouldBe Right(None)
    }

    "return a BadRequest" in {
      val errorBody = APIErrorBodyModel("BAD_REQUEST", "That request was bad")
      val expectedResult = APIErrorModel(BAD_REQUEST, errorBody)

      stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
        BAD_REQUEST, Json.toJson(errorBody).toString(), requestHeaders)

      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedInterest(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }

    "return an InternalServerError" in {
      val errorBody: APIErrorBodyModel = APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong")
      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, errorBody)

      stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
        INTERNAL_SERVER_ERROR, Json.toJson(errorBody).toString(), requestHeaders)

      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedInterest(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }

    "return an InternalServerError due to parsing error" in {
      val invalidJson = Json.obj("accountName" -> "")
      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)

      stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
        OK, invalidJson.toString(), requestHeaders)

      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedInterest(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }

    "return an InternalServerError with parsing error when we can't parse the error body" in {
      val errorBody = "INTERNAL_SERVER_ERROR"
      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)

      stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
        INTERNAL_SERVER_ERROR, Json.toJson(errorBody).toString(), requestHeaders)

      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedInterest(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }

    "return an InternalServerError when an unexpected status is thrown" in {
      val errorBody: APIErrorBodyModel = APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong")
      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, errorBody)

      stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
        IM_A_TEAPOT, Json.toJson(errorBody).toString(), requestHeaders)

      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedInterest(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }

    "return an InternalServerError when an unexpected status is thrown and there is no body" in {
      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)

      stubGetWithoutResponseBody(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
        IM_A_TEAPOT, requestHeaders)

      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedInterest(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }

    "return a ServiceUnavailableError" in {
      val errorBody = APIErrorBodyModel("SERVICE_UNAVAILABLE", "Something went wrong")
      val expectedResult = APIErrorModel(SERVICE_UNAVAILABLE, errorBody)

      stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
        SERVICE_UNAVAILABLE, Json.toJson(errorBody).toString(), requestHeaders)

      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedInterest(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }
  }
}
