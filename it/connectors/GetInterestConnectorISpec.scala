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

package connectors

import com.github.tomakehurst.wiremock.http.HttpHeader
import config.AppConfig
import helpers.WiremockSpec
import models.{APIErrorBodyModel, APIErrorModel, InterestModel}
import play.api.Configuration
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpClient, SessionId}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class GetInterestConnectorISpec extends WiremockSpec {

  lazy val connector: IncomeTaxInterestConnector = app.injector.instanceOf[IncomeTaxInterestConnector]

  lazy val httpClient: HttpClient = app.injector.instanceOf[HttpClient]
  def appConfig(interestHost: String): AppConfig = new AppConfig(app.injector.instanceOf[Configuration], app.injector.instanceOf[ServicesConfig]) {
    override val interestBaseUrl: String = s"http://$interestHost:$wireMockPort"
  }

  val nino: String = "AA123123A"
  val taxYear: Int = 2020
  val mtditidHeader = ("mtditid", "123123123")
  val requestHeaders: Seq[HttpHeader] = Seq(new HttpHeader("mtditid", "123123123"))


  val accountName: String = "SomeName"
  val incomeSourceId: String = "12345"
  val untaxedUkInterest: Option[BigDecimal] = Some(12345.67)
  val taxedUkInterest: Option[BigDecimal] = Some(12345.67)


  "IncomeTaxInterestConnector" should {

    "include internal headers" when {
      val expectedResult = Some(Seq(InterestModel(accountName, incomeSourceId, taxedUkInterest, untaxedUkInterest)))
      val responseBody = Json.toJson(expectedResult).toString()

      val headersSentToInterest = Seq(new HttpHeader(HeaderNames.xSessionId, "sessionIdValue"))

      val internalHost = "localhost"
      val externalHost = "127.0.0.1"

      "the host for Interest is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new IncomeTaxInterestConnector(httpClient, appConfig(internalHost))

        stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear", OK, responseBody, headersSentToInterest)

        val result = await(connector.getSubmittedInterest(nino, taxYear)(hc))

        result mustBe Right(expectedResult)
      }

      "the host for Interest is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new IncomeTaxInterestConnector(httpClient, appConfig(externalHost))

        stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear", OK, responseBody, headersSentToInterest)

        val result = await(connector.getSubmittedInterest(nino, taxYear)(hc))

        result mustBe Right(expectedResult)
      }
    }

    "return a SubmittedInterestModel" when {

      "all values are present" in {

        val expectedResult = Some(Seq(InterestModel(accountName, incomeSourceId, taxedUkInterest, untaxedUkInterest)))

        stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
          OK, Json.toJson(expectedResult).toString(), requestHeaders)

        implicit val hc = HeaderCarrier().withExtraHeaders(mtditidHeader)
        val result = await(connector.getSubmittedInterest(nino, taxYear)(hc))

        result mustBe Right(expectedResult)

      }
    }

    "return a none when no interest values are found" in {

      val body = Seq.empty[InterestModel]
      stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
        OK, Json.toJson(body).toString(), requestHeaders)

      implicit val hc = HeaderCarrier().withExtraHeaders(mtditidHeader)
      val result = await(connector.getSubmittedInterest(nino, taxYear)(hc))

      result mustBe Right(None)
    }

    "return a none for a NotFound" in {

      stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
        NOT_FOUND, "{}", requestHeaders)

      implicit val hc = HeaderCarrier().withExtraHeaders(mtditidHeader)
      val result = await(connector.getSubmittedInterest(nino, taxYear)(hc))

      result mustBe Right(None)
    }

    "return a BadRequest" in {
      val errorBody: APIErrorBodyModel = APIErrorBodyModel("BAD_REQUEST", "That request was bad")
      val expectedResult = APIErrorModel(BAD_REQUEST, errorBody)

      stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
        BAD_REQUEST, Json.toJson(errorBody).toString(), requestHeaders)

      implicit val hc = HeaderCarrier().withExtraHeaders(mtditidHeader)
      val result = await(connector.getSubmittedInterest(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return an InternalServerError" in {
      val errorBody: APIErrorBodyModel = APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong")

      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, errorBody)

      stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
        INTERNAL_SERVER_ERROR, Json.toJson(errorBody).toString(), requestHeaders)

      implicit val hc = HeaderCarrier().withExtraHeaders(mtditidHeader)
      val result = await(connector.getSubmittedInterest(nino, taxYear)(hc))

      result mustBe Left(expectedResult)

    }

    "return an InternalServerError due to parsing error" in {

      val invalidJson = Json.obj(
        "accountName" -> ""
      )

        val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)

      stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
        OK, invalidJson.toString(), requestHeaders)

      implicit val hc = HeaderCarrier().withExtraHeaders(mtditidHeader)
      val result = await(connector.getSubmittedInterest(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return an InternalServerError with parsing error when we can't parse the error body" in {
      val errorBody = "INTERNAL_SERVER_ERROR"

      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)

      stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
        INTERNAL_SERVER_ERROR, Json.toJson(errorBody).toString(), requestHeaders)

      implicit val hc = HeaderCarrier().withExtraHeaders(mtditidHeader)
      val result = await(connector.getSubmittedInterest(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return an InternalServerError when an unexpected status is thrown" in {
      val errorBody: APIErrorBodyModel = APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong")

      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, errorBody)

      stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
        IM_A_TEAPOT, Json.toJson(errorBody).toString(), requestHeaders)

      implicit val hc = HeaderCarrier().withExtraHeaders(mtditidHeader)
      val result = await(connector.getSubmittedInterest(nino, taxYear)(hc))

      result mustBe Left(expectedResult)

    }

    "return an InternalServerError when an unexpected status is thrown and there is no body" in {

      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)

      stubGetWithoutResponseBody(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
        IM_A_TEAPOT, requestHeaders)

      implicit val hc = HeaderCarrier().withExtraHeaders(mtditidHeader)
      val result = await(connector.getSubmittedInterest(nino, taxYear)(hc))

      result mustBe Left(expectedResult)

    }

    "return a ServiceUnavailableError" in {

      val errorBody = APIErrorBodyModel("SERVICE_UNAVAILABLE", "Something went wrong")
      val expectedResult = APIErrorModel(SERVICE_UNAVAILABLE, errorBody)

      stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
        SERVICE_UNAVAILABLE, Json.toJson(errorBody).toString(), requestHeaders)

      implicit val hc = HeaderCarrier().withExtraHeaders(mtditidHeader)
      val result = await(connector.getSubmittedInterest(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }


  }
}
