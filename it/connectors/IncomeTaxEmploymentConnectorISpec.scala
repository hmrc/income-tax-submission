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

package connectors

import builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import com.github.tomakehurst.wiremock.http.HttpHeader
import models.employment.AllEmploymentData
import models.{APIErrorBodyModel, APIErrorModel, APIErrorsBodyModel}
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, SessionId}
import utils.{ConnectorIntegrationTest, MockAppConfig}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class IncomeTaxEmploymentConnectorISpec extends ConnectorIntegrationTest {

  private val nino = "AA123123A"
  private val taxYear = 2022

  private val mtditidHeader = ("mtditid", "123123123")
  private val requestHeaders = Seq(new HttpHeader("mtditid", "123123123"))

  private val underTest = new IncomeTaxEmploymentConnector(httpClient, new MockAppConfig())

  "IncomeTaxEmploymentConnector" should {
    "include internal headers" when {
      val expectedResult = Some(anAllEmploymentData)
      val responseBody = Json.toJson(expectedResult).toString()

      val headersSentToEmployment = Seq(new HttpHeader(HeaderNames.xSessionId, "sessionIdValue"))

      "the host for Employment is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))

        stubGetWithResponseBody(s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=$taxYear", OK, responseBody, headersSentToEmployment)

        Await.result(underTest.getSubmittedEmployment(nino, taxYear), Duration.Inf) shouldBe Right(expectedResult)
      }

      "the host for Employment is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))

        stubGetWithResponseBody(s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=$taxYear", OK, responseBody, headersSentToEmployment)

        Await.result(underTest.getSubmittedEmployment(nino, taxYear), Duration.Inf) shouldBe Right(expectedResult)
      }
    }

    "return a AllEmploymentData" when {
      "all values are present" in {
        val requestBody = Json.toJson(Some(anAllEmploymentData))
        implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

        stubGetWithResponseBody(s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=$taxYear", OK, requestBody.toString(), requestHeaders)

        Await.result(underTest.getSubmittedEmployment(nino, taxYear), Duration.Inf) shouldBe Right(Some(anAllEmploymentData))
      }
    }

    "return a none when no employment info found" in {
      val response = Json.toJson(AllEmploymentData(Seq(), None, Seq(), None)).toString()
      stubGetWithResponseBody(s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=$taxYear", OK, response, requestHeaders)

      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedEmployment(nino, taxYear), Duration.Inf) shouldBe Right(None)
    }

    "return a None for no content" in {
      stubGetWithResponseBody(s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=2022", NO_CONTENT, "{}", requestHeaders)

      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedEmployment(nino, taxYear), Duration.Inf) shouldBe Right(None)
    }

    "API Returns multiple errors" in {
      val expectedResult = APIErrorModel(BAD_REQUEST, APIErrorsBodyModel(Seq(
        APIErrorBodyModel("INVALID_IDTYPE", "ID is invalid"),
        APIErrorBodyModel("INVALID_IDTYPE_2", "ID 2 is invalid"))
      ))

      val response = Json.obj("failures" -> Json.arr(
        Json.obj("code" -> "INVALID_IDTYPE", "reason" -> "ID is invalid"),
        Json.obj("code" -> "INVALID_IDTYPE_2", "reason" -> "ID 2 is invalid")
      ))
      stubGetWithResponseBody(s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=2022", BAD_REQUEST, response.toString(), requestHeaders)

      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedEmployment(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }

    "return a BadRequest" in {
      val errorBody: APIErrorBodyModel = APIErrorBodyModel("BAD_REQUEST", "That request was bad")
      val expectedResult = APIErrorModel(BAD_REQUEST, errorBody)

      stubGetWithResponseBody(s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=2022",
        BAD_REQUEST, Json.toJson(errorBody).toString(), requestHeaders)
      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedEmployment(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }

    "return an InternalServerError " in {
      val errorBody: APIErrorBodyModel = APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong")
      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, errorBody)

      stubGetWithResponseBody(s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=2022",
        INTERNAL_SERVER_ERROR, Json.toJson(errorBody).toString(), requestHeaders)
      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedEmployment(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }

    "return an InternalServerError due to parsing error" in {
      val invalidJson = Json.obj("employment" -> "")
      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)

      stubGetWithResponseBody(s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=2022", OK, invalidJson.toString(), requestHeaders)
      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedEmployment(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }

    "return an InternalServerError with parsing error when we can't parse the error body" in {
      val errorBody = "INTERNAL_SERVER_ERROR"
      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)

      stubGetWithResponseBody(s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=2022",
        INTERNAL_SERVER_ERROR, Json.toJson(errorBody).toString(), requestHeaders)
      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedEmployment(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }

    "return an InternalServerError when an unexpected status is thrown" in {
      val errorBody: APIErrorBodyModel = APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong")
      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, errorBody)

      stubGetWithResponseBody(s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=2022",
        IM_A_TEAPOT, Json.toJson(errorBody).toString(), requestHeaders)
      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedEmployment(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }

    "return an InternalServerError when an unexpected status is thrown and there is no body" in {
      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)

      stubGetWithoutResponseBody(s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=2022", IM_A_TEAPOT)
      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedEmployment(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }

    "return a ServiceUnavailableError" in {
      val errorBody: APIErrorBodyModel = APIErrorBodyModel("SERVICE_UNAVAILABLE", "Service went down")
      val expectedResult = APIErrorModel(SERVICE_UNAVAILABLE, errorBody)

      stubGetWithResponseBody(s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=2022",
        SERVICE_UNAVAILABLE, Json.toJson(errorBody).toString(), requestHeaders)
      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedEmployment(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }
  }
}
