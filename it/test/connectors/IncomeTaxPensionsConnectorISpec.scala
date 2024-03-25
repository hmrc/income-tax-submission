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

import builders.models.pensions.PensionsBuilder.aPensions
import com.github.tomakehurst.wiremock.http.HttpHeader
import models.pensions.Pensions
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, SessionId}
import utils.{ConnectorIntegrationTest, MockAppConfig}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class IncomeTaxPensionsConnectorISpec extends ConnectorIntegrationTest {

  private val nino = "AA123123A"
  private val taxYear = 2022
  private val mtditidHeader = ("mtditid", "123123123")
  private val requestHeaders = Seq(new HttpHeader("mtditid", "123123123"))

  private val underTest = new IncomeTaxPensionsConnector(httpClient, new MockAppConfig())

  "IncomeTaxPensionsConnector" should {
    "include internal headers" when {
      val expectedResult = Some(aPensions)
      val responseBody = Json.toJson(expectedResult).toString()
      val headersSentToPensions = Seq(new HttpHeader(HeaderNames.xSessionId, "sessionIdValue"))

      "the host for Pensions is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))

        stubGetWithResponseBody(s"/income-tax-pensions/income-tax/nino/$nino/sources\\?taxYear=$taxYear", OK, responseBody, headersSentToPensions)

        Await.result(underTest.getSubmittedPensions(nino, taxYear), Duration.Inf) shouldBe Right(expectedResult)
      }

      "the host for Pensions is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))

        stubGetWithResponseBody(s"/income-tax-pensions/income-tax/nino/$nino/sources\\?taxYear=$taxYear", OK, responseBody, headersSentToPensions)

        Await.result(underTest.getSubmittedPensions(nino, taxYear), Duration.Inf) shouldBe Right(expectedResult)
      }
    }

    "return a PensionsModel" when {
      "all values are present" in {
        val responseBody = Json.toJson(aPensions).toString()

        stubGetWithResponseBody(s"/income-tax-pensions/income-tax/nino/$nino/sources\\?taxYear=$taxYear", OK, responseBody, requestHeaders)

        implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

        Await.result(underTest.getSubmittedPensions(nino, taxYear), Duration.Inf) shouldBe Right(Some(aPensions))
      }

      "return a Right None if pensions data is None" in {
        val responseBody = Some(Pensions(None, None, None, None, None))

        stubGetWithResponseBody(s"/income-tax-pensions/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
          OK, Json.toJson(responseBody).toString(), requestHeaders)

        implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

        Await.result(underTest.getSubmittedPensions(nino, taxYear), Duration.Inf) shouldBe Right(None)
      }

      "return a BadRequest" in {
        val errorBody: APIErrorBodyModel = APIErrorBodyModel("BAD_REQUEST", "That request was bad")
        val expectedResult = APIErrorModel(BAD_REQUEST, errorBody)

        stubGetWithResponseBody(s"/income-tax-pensions/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
          BAD_REQUEST, Json.toJson(errorBody).toString(), requestHeaders)

        implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

        Await.result(underTest.getSubmittedPensions(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
      }

      "return an InternalServerError" in {
        val errorBody: APIErrorBodyModel = APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong")
        val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, errorBody)

        stubGetWithResponseBody(s"/income-tax-pensions/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
          INTERNAL_SERVER_ERROR, Json.toJson(errorBody).toString(), requestHeaders)

        implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

        Await.result(underTest.getSubmittedPensions(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
      }

      "return an InternalServerError due bad success Json" in {
        val invalidJson = Json.obj("pensionReliefs" -> Some(false))
        val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)

        stubGetWithResponseBody(s"/income-tax-pensions/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
          OK, invalidJson.toString(), requestHeaders)

        implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

        Await.result(underTest.getSubmittedPensions(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
      }

      "return an InternalServerError with a parsing error in the error body" in {
        val errorBody = "INTERNAL_SERVER_ERROR"
        val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)

        stubGetWithResponseBody(s"/income-tax-pensions/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
          INTERNAL_SERVER_ERROR, Json.toJson(errorBody).toString(), requestHeaders)

        implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

        Await.result(underTest.getSubmittedPensions(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
      }

      "return an InternalServerError when an unexpected status is thrown" in {
        val errorBody: APIErrorBodyModel = APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong")
        val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, errorBody)

        stubGetWithResponseBody(s"/income-tax-pensions/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
          IM_A_TEAPOT, Json.toJson(errorBody).toString(), requestHeaders)

        implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

        Await.result(underTest.getSubmittedPensions(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
      }

      "return an InternalServerError when an unexpected status is thrown and there is no body" in {
        val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)

        stubGetWithoutResponseBody(s"/income-tax-pensions/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
          IM_A_TEAPOT, requestHeaders)

        implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

        Await.result(underTest.getSubmittedPensions(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
      }

      "return a ServiceUnavailableError" in {
        val errorBody = APIErrorBodyModel("SERVICE_UNAVAILABLE", "Something went wrong")
        val expectedResult = APIErrorModel(SERVICE_UNAVAILABLE, errorBody)

        stubGetWithResponseBody(s"/income-tax-pensions/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
          SERVICE_UNAVAILABLE, Json.toJson(errorBody).toString(), requestHeaders)

        implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

        Await.result(underTest.getSubmittedPensions(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
      }
    }
  }
}
