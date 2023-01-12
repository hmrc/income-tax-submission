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
import models._
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, SessionId}
import utils.TaxYearUtil.convertStringTaxYear
import utils.{ConnectorIntegrationTest, MockAppConfig}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class IncomeTaxGainsConnectorISpec extends ConnectorIntegrationTest {

  private val nino = "AA123123A"
  private val taxYear = 1999

  private val mtditidHeader = ("mtditid", "123123123")
  private val requestHeaders = Seq(new HttpHeader("mtditid", "123123123"))

  val taxYearParameter = convertStringTaxYear(taxYear)
  val url = s"/income-tax/insurance-policies/income/$nino/$taxYearParameter"

  val ifReturned: InsurancePoliciesModel = InsurancePoliciesModel(
    submittedOn = "2020-01-04T05:01:01Z",
    lifeInsurance = Seq(LifeInsuranceModel(Some("RefNo13254687"), Some("Life"), 123.45, Some(true), Some(4), Some(3), Some(123.45))),
    capitalRedemption = Some(Seq(CapitalRedemptionModel(Some("RefNo13254687"), Some("Capital"), 123.45, Some(true), Some(3), Some(2), Some(0)))),
    lifeAnnuity = Some(Seq(LifeAnnuityModel(Some("RefNo13254687"), Some("Life"), 0, Some(true), Some(2), Some(22), Some(123.45)))),
    voidedIsa = Some(Seq(VoidedIsaModel(Some("RefNo13254687"), Some("isa"), 123.45, Some(123.45), Some(5), Some(6)))),
    foreign = Some(Seq(ForeignModel(Some("RefNo13254687"), 123.45, Some(123.45), Some(3))))
  )

  private val underTest = new IncomeTaxGainsConnector(httpClient, new MockAppConfig())

  "IncomeTaxGainsConnector" should {
    "include internal headers" when {
      val expectedResult = Some(ifReturned)
      val responseBody = Json.toJson(expectedResult).toString()
      val headersSentToGains = Seq(new HttpHeader(HeaderNames.xSessionId, "sessionIdValue"))

      "the host for Gains is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))

        stubGetWithResponseBody(url, OK, responseBody, headersSentToGains)

        Await.result(underTest.getSubmittedGains(nino, taxYear), Duration.Inf) shouldBe Right(expectedResult)
      }

      "the host for Gains is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))

        stubGetWithResponseBody(url, OK, responseBody, headersSentToGains)

        Await.result(underTest.getSubmittedGains(nino, taxYear), Duration.Inf) shouldBe Right(expectedResult)
      }
    }

    "return a SubmittedGainsModel" when {
      "all values are present" in {
        val expectedResult = Some(ifReturned)

        stubGetWithResponseBody(url, OK, Json.toJson(expectedResult).toString(), requestHeaders)

        implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

        Await.result(underTest.getSubmittedGains(nino, taxYear), Duration.Inf) shouldBe Right(expectedResult)
      }
    }

    "return a none when no Gains values found" in {
      stubGetWithResponseBody(url, OK, Json.toJson(InsurancePoliciesModel("", Seq(), None, None, None, None)).toString(), requestHeaders)

      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedGains(nino, taxYear), Duration.Inf) shouldBe Right(None)
    }

    "return a None for not found" in {
      stubGetWithResponseBody(url, NOT_FOUND, "{}", requestHeaders)
      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedGains(nino, taxYear), Duration.Inf) shouldBe Right(None)
    }

    "API Returns multiple errors" in {
      val expectedResult = APIErrorModel(BAD_REQUEST, APIErrorsBodyModel(Seq(
        APIErrorBodyModel("INVALID_IDTYPE", "ID is invalid"),
        APIErrorBodyModel("INVALID_IDTYPE_2", "ID 2 is invalid"))
      ))
      val responseBody = Json.obj("failures" -> Json.arr(
        Json.obj("code" -> "INVALID_IDTYPE", "reason" -> "ID is invalid"),
        Json.obj("code" -> "INVALID_IDTYPE_2", "reason" -> "ID 2 is invalid")
      ))

      stubGetWithResponseBody(url, BAD_REQUEST, responseBody.toString(), requestHeaders)

      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedGains(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }

    "return a BadRequest" in {
      val errorBody: APIErrorBodyModel = APIErrorBodyModel("BAD_REQUEST", "That request was bad")
      val expectedResult = APIErrorModel(BAD_REQUEST, errorBody)

      stubGetWithResponseBody(url, BAD_REQUEST, Json.toJson(errorBody).toString(), requestHeaders)
      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedGains(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }

    "return an InternalServerError " in {
      val errorBody: APIErrorBodyModel = APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong")
      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, errorBody)

      stubGetWithResponseBody(url, INTERNAL_SERVER_ERROR, Json.toJson(errorBody).toString(), requestHeaders)
      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedGains(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }

    "return an InternalServerError due to parsing error" in {
      val invalidJson = Json.obj("insurancePoliciesModel" -> "")
      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)

      stubGetWithResponseBody(url, OK, invalidJson.toString(), requestHeaders)
      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedGains(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }

    "return an InternalServerError with parsing error when we can't parse the error body" in {
      val errorBody = "INTERNAL_SERVER_ERROR"
      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)

      stubGetWithResponseBody(url, INTERNAL_SERVER_ERROR, Json.toJson(errorBody).toString(), requestHeaders)
      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedGains(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }

    "return an InternalServerError when an unexpected status is thrown" in {
      val errorBody: APIErrorBodyModel = APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong")
      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, errorBody)

      stubGetWithResponseBody(url, IM_A_TEAPOT, Json.toJson(errorBody).toString(), requestHeaders)
      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedGains(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }

    "return an InternalServerError when an unexpected status is thrown and there is no body" in {
      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)

      stubGetWithoutResponseBody(url, IM_A_TEAPOT)
      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedGains(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }

    "return a ServiceUnavailableError" in {
      val errorBody: APIErrorBodyModel = APIErrorBodyModel("SERVICE_UNAVAILABLE", "Service went down")
      val expectedResult = APIErrorModel(SERVICE_UNAVAILABLE, errorBody)

      stubGetWithResponseBody(url, SERVICE_UNAVAILABLE, Json.toJson(errorBody).toString(), requestHeaders)
      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedGains(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }
  }
}
