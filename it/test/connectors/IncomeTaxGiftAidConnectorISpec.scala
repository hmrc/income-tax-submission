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
import models.gifts.{GiftAid, GiftAidPayments, Gifts}
import models.{APIErrorBodyModel, APIErrorModel, APIErrorsBodyModel}
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, SessionId}
import utils.{ConnectorIntegrationTest, MockAppConfig}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class IncomeTaxGiftAidConnectorISpec extends ConnectorIntegrationTest {

  private val nino = "AA123123A"
  private val taxYear = 1999

  private val mtditidHeader = ("mtditid", "123123123")
  private val requestHeaders = Seq(new HttpHeader("mtditid", "123123123"))

  private val giftAidPayments = GiftAidPayments(
    Some(List("non uk charity name", "non uk charity name 2")),
    Some(12345.67),
    Some(12345.67),
    Some(12345.67),
    Some(12345.67),
    Some(12345.67)
  )

  private val gifts = Gifts(Some(List("charity name")), Some(12345.67), Some(12345.67), Some(12345.67))

  private val underTest = new IncomeTaxGiftAidConnector(httpClient, new MockAppConfig())

  "IncomeTaxGiftAidConnector" should {
    "include internal headers" when {
      val expectedResult = Some(GiftAid(Some(giftAidPayments), Some(gifts)))
      val responseBody = Json.toJson(expectedResult).toString()
      val headersSentToGiftAid = Seq(new HttpHeader(HeaderNames.xSessionId, "sessionIdValue"))

      "the host for GiftAid is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))

        stubGetWithResponseBody(s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=$taxYear", OK, responseBody, headersSentToGiftAid)

        Await.result(underTest.getSubmittedGiftAid(nino, taxYear), Duration.Inf) shouldBe Right(expectedResult)
      }

      "the host for GiftAid is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))

        stubGetWithResponseBody(s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=$taxYear", OK, responseBody, headersSentToGiftAid)

        Await.result(underTest.getSubmittedGiftAid(nino, taxYear), Duration.Inf) shouldBe Right(expectedResult)
      }
    }

    "return a SubmittedGiftAidModel" when {
      "all values are present" in {
        val expectedResult = Some(GiftAid(Some(giftAidPayments), Some(gifts)))

        stubGetWithResponseBody(s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
          OK, Json.toJson(expectedResult).toString(), requestHeaders)

        implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

        Await.result(underTest.getSubmittedGiftAid(nino, taxYear), Duration.Inf) shouldBe Right(expectedResult)
      }
    }

    "return a none when no gift aid values found" in {
      stubGetWithResponseBody(s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
        OK, Json.toJson(GiftAid(None, None)).toString(), requestHeaders)

      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedGiftAid(nino, taxYear), Duration.Inf) shouldBe Right(None)
    }

    "return a None for not found" in {
      stubGetWithResponseBody(s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=1999", NOT_FOUND, "{}", requestHeaders)
      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedGiftAid(nino, taxYear), Duration.Inf) shouldBe Right(None)
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

      stubGetWithResponseBody(s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=1999", BAD_REQUEST, responseBody.toString(), requestHeaders)

      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedGiftAid(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }

    "return a BadRequest" in {
      val errorBody: APIErrorBodyModel = APIErrorBodyModel("BAD_REQUEST", "That request was bad")
      val expectedResult = APIErrorModel(BAD_REQUEST, errorBody)

      stubGetWithResponseBody(s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=1999",
        BAD_REQUEST, Json.toJson(errorBody).toString(), requestHeaders)
      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedGiftAid(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }

    "return an InternalServerError " in {
      val errorBody: APIErrorBodyModel = APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong")
      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, errorBody)

      stubGetWithResponseBody(s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=1999",
        INTERNAL_SERVER_ERROR, Json.toJson(errorBody).toString(), requestHeaders)
      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedGiftAid(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }

    "return an InternalServerError due to parsing error" in {
      val invalidJson = Json.obj("giftAidPayments" -> "")
      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)

      stubGetWithResponseBody(s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=1999", OK, invalidJson.toString(), requestHeaders)
      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedGiftAid(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }

    "return an InternalServerError with parsing error when we can't parse the error body" in {
      val errorBody = "INTERNAL_SERVER_ERROR"
      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)

      stubGetWithResponseBody(s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=1999",
        INTERNAL_SERVER_ERROR, Json.toJson(errorBody).toString(), requestHeaders)
      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedGiftAid(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }

    "return an InternalServerError when an unexpected status is thrown" in {
      val errorBody: APIErrorBodyModel = APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong")
      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, errorBody)

      stubGetWithResponseBody(s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=1999",
        IM_A_TEAPOT, Json.toJson(errorBody).toString(), requestHeaders)
      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedGiftAid(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }

    "return an InternalServerError when an unexpected status is thrown and there is no body" in {
      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)

      stubGetWithoutResponseBody(s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=1999", IM_A_TEAPOT)
      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedGiftAid(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }

    "return a ServiceUnavailableError" in {
      val errorBody: APIErrorBodyModel = APIErrorBodyModel("SERVICE_UNAVAILABLE", "Service went down")
      val expectedResult = APIErrorModel(SERVICE_UNAVAILABLE, errorBody)

      stubGetWithResponseBody(s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=1999",
        SERVICE_UNAVAILABLE, Json.toJson(errorBody).toString(), requestHeaders)
      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtditidHeader)

      Await.result(underTest.getSubmittedGiftAid(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }
  }
}
