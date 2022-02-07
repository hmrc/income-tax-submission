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

import com.github.tomakehurst.wiremock.http.HttpHeader
import config.{AppConfig, BackendAppConfig}
import helpers.IntegrationSpec
import models.pensions.PensionsModel
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.Configuration
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpClient, SessionId}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class GetPensionsConnectorISpec extends IntegrationSpec {

  lazy val connector: IncomeTaxPensionsConnector = app.injector.instanceOf[IncomeTaxPensionsConnector]
  lazy val httpClient: HttpClient = app.injector.instanceOf[HttpClient]

  def appConfig(pensionsHost: String): AppConfig = new BackendAppConfig(app.injector.instanceOf[Configuration],
    app.injector.instanceOf[ServicesConfig]) {
    override val pensionsBaseUrl: String = s"http://$pensionsHost:$wireMockPort"
  }

  val nino: String = "AA123123A"
  val taxYear: Int = 2022
  val mtditidHeader = ("mtditid", "123123123")
  val requestHeaders: Seq[HttpHeader] = Seq(new HttpHeader("mtditid", "123123123"))

  "IncomeTaxPensionsConnector" should {

    "include internal headers" when {
      val expectedResult = Some(fullPensionsModel)
      val responseBody = Json.toJson(expectedResult).toString()

      val headersSentToPensions = Seq(new HttpHeader(HeaderNames.xSessionId, "sessionIdValue"))

      val internalHost = "localhost"
      val externalHost = "127.0.0.1"

      "the host for Pensions is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new IncomeTaxPensionsConnector(httpClient, appConfig(externalHost))

        stubGetWithResponseBody(s"/income-tax-pensions/income-tax/nino/$nino/sources\\?taxYear=$taxYear", OK, responseBody, headersSentToPensions)

        val result = await(connector.getSubmittedPensions(nino, taxYear)(hc))

        result mustBe Right(expectedResult)
      }

      "the host for Pensions is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new IncomeTaxPensionsConnector(httpClient, appConfig(internalHost))

        stubGetWithResponseBody(s"/income-tax-pensions/income-tax/nino/$nino/sources\\?taxYear=$taxYear", OK, responseBody, headersSentToPensions)

        val result = await(connector.getSubmittedPensions(nino, taxYear)(hc))

        result mustBe Right(expectedResult)
      }
    }

    "return a PensionsModel" when {

      "all values are present" in {
        val responseBody = Json.toJson(fullPensionsModel).toString()
        val expectedResult = Some(fullPensionsModel)

        stubGetWithResponseBody(s"/income-tax-pensions/income-tax/nino/$nino/sources\\?taxYear=$taxYear", OK, responseBody, requestHeaders)

        implicit val hc = HeaderCarrier().withExtraHeaders(mtditidHeader)
        val result = await(connector.getSubmittedPensions(nino, taxYear)(hc))

        result mustBe Right(expectedResult)
      }

      "return a Right None if pensions data is None" in {
        val responseBody = Some(PensionsModel(None, None, None))

        stubGetWithResponseBody(s"/income-tax-pensions/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
          OK, Json.toJson(responseBody).toString(), requestHeaders)

        implicit val hc = HeaderCarrier().withExtraHeaders(mtditidHeader)
        val result = await(connector.getSubmittedPensions(nino, taxYear)(hc))

        result mustBe Right(None)
      }

      "return a BadRequest" in {
        val errorBody: APIErrorBodyModel = APIErrorBodyModel("BAD_REQUEST", "That request was bad")
        val expectedResult = APIErrorModel(BAD_REQUEST, errorBody)

        stubGetWithResponseBody(s"/income-tax-pensions/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
          BAD_REQUEST, Json.toJson(errorBody).toString(), requestHeaders)

        implicit val hc = HeaderCarrier().withExtraHeaders(mtditidHeader)
        val result = await(connector.getSubmittedPensions(nino, taxYear)(hc))

        result mustBe Left(expectedResult)
      }

      "return an InternalServerError" in {
        val errorBody: APIErrorBodyModel = APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong")

        val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, errorBody)

        stubGetWithResponseBody(s"/income-tax-pensions/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
          INTERNAL_SERVER_ERROR, Json.toJson(errorBody).toString(), requestHeaders)

        implicit val hc = HeaderCarrier().withExtraHeaders(mtditidHeader)
        val result = await(connector.getSubmittedPensions(nino, taxYear)(hc))

        result mustBe Left(expectedResult)

      }

      "return an InternalServerError due bad success Json" in {

        val invalidJson = Json.obj(
          "pensionReliefs" -> Some(false)
        )

        val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)

        stubGetWithResponseBody(s"/income-tax-pensions/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
          OK, invalidJson.toString(), requestHeaders)

        implicit val hc = HeaderCarrier().withExtraHeaders(mtditidHeader)
        val result = await(connector.getSubmittedPensions(nino, taxYear)(hc))

        result mustBe Left(expectedResult)
      }

      "return an InternalServerError with a parsing error in the error body" in {
        val errorBody = "INTERNAL_SERVER_ERROR"

        val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)

        stubGetWithResponseBody(s"/income-tax-pensions/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
          INTERNAL_SERVER_ERROR, Json.toJson(errorBody).toString(), requestHeaders)

        implicit val hc = HeaderCarrier().withExtraHeaders(mtditidHeader)
        val result = await(connector.getSubmittedPensions(nino, taxYear)(hc))

        result mustBe Left(expectedResult)
      }

      "return an InternalServerError when an unexpected status is thrown" in {
        val errorBody: APIErrorBodyModel = APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong")

        val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, errorBody)

        stubGetWithResponseBody(s"/income-tax-pensions/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
          IM_A_TEAPOT, Json.toJson(errorBody).toString(), requestHeaders)

        implicit val hc = HeaderCarrier().withExtraHeaders(mtditidHeader)
        val result = await(connector.getSubmittedPensions(nino, taxYear)(hc))

        result mustBe Left(expectedResult)

      }

      "return an InternalServerError when an unexpected status is thrown and there is no body" in {

        val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)

        stubGetWithoutResponseBody(s"/income-tax-pensions/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
          IM_A_TEAPOT, requestHeaders)

        implicit val hc = HeaderCarrier().withExtraHeaders(mtditidHeader)
        val result = await(connector.getSubmittedPensions(nino, taxYear)(hc))

        result mustBe Left(expectedResult)

      }

      "return a ServiceUnavailableError" in {

        val errorBody = APIErrorBodyModel("SERVICE_UNAVAILABLE", "Something went wrong")
        val expectedResult = APIErrorModel(SERVICE_UNAVAILABLE, errorBody)

        stubGetWithResponseBody(s"/income-tax-pensions/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
          SERVICE_UNAVAILABLE, Json.toJson(errorBody).toString(), requestHeaders)

        implicit val hc = HeaderCarrier().withExtraHeaders(mtditidHeader)
        val result = await(connector.getSubmittedPensions(nino, taxYear)(hc))

        result mustBe Left(expectedResult)
      }
    }
  }
}
