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
import helpers.WiremockSpec
import models.giftAid.{GiftAidPaymentsModel, GiftsModel, SubmittedGiftAidModel}
import models.{APIErrorBodyModel, APIErrorModel, APIErrorsBodyModel}
import org.scalatestplus.play.PlaySpec
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier

class GetGiftAidConnectorISpec extends PlaySpec with WiremockSpec {

  lazy val connector: IncomeTaxGiftAidConnector = app.injector.instanceOf[IncomeTaxGiftAidConnector]

  val nino: String = "AA123123A"
  val taxYear: Int = 1999

  val mtditidHeader = ("mtditid", "123123123")
  val requestHeaders: Seq[HttpHeader] = Seq(new HttpHeader("mtditid", "123123123"))

  val giftAidPayments: GiftAidPaymentsModel = GiftAidPaymentsModel(Some(List("non uk charity name","non uk charity name 2")), Some(12345.67), Some(12345.67), Some(12345.67), Some(12345.67), Some(12345.67))
  val gifts: GiftsModel = GiftsModel(Some(List("charity name")), Some(12345.67), Some(12345.67) , Some(12345.67))

  "IncomeTaxGiftAidConnector" should {
    "return a SubmittedGiftAidModel" when {
      "all values are present" in {
        val expectedResult = Some(SubmittedGiftAidModel(Some(giftAidPayments), Some(gifts)))

        stubGetWithResponseBody(s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
          OK, Json.toJson(expectedResult).toString(),
          requestHeaders
        )

        implicit val hc = HeaderCarrier().withExtraHeaders(mtditidHeader)
        val result = await(connector.getSubmittedGiftAid(nino, taxYear)(hc))

        result mustBe Right(expectedResult)
      }
    }

    "return a none when no gift aid values found" in {

      val body = SubmittedGiftAidModel(None, None)
      stubGetWithResponseBody(s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
        OK, Json.toJson(body).toString(),
        requestHeaders)

      implicit val hc = HeaderCarrier().withExtraHeaders(mtditidHeader)
      val result = await(connector.getSubmittedGiftAid(nino, taxYear)(hc))

      result mustBe Right(None)
    }

    "return a None for notfound" in {
      stubGetWithResponseBody(s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=1999", NOT_FOUND, "{}", requestHeaders)
      implicit val hc = HeaderCarrier().withExtraHeaders(mtditidHeader)
      val result = await(connector.getSubmittedGiftAid(nino, taxYear)(hc))

      result mustBe Right(None)
    }

    "API Returns multiple errors" in {
      val expectedResult = APIErrorModel(BAD_REQUEST, APIErrorsBodyModel(Seq(
        APIErrorBodyModel("INVALID_IDTYPE","ID is invalid"),
        APIErrorBodyModel("INVALID_IDTYPE_2","ID 2 is invalid"))))

      val responseBody = Json.obj(
        "failures" -> Json.arr(
          Json.obj("code" -> "INVALID_IDTYPE",
            "reason" -> "ID is invalid"),
          Json.obj("code" -> "INVALID_IDTYPE_2",
            "reason" -> "ID 2 is invalid")
        )
      )
      stubGetWithResponseBody(s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=1999", BAD_REQUEST, responseBody.toString(), requestHeaders)

      implicit val hc = HeaderCarrier().withExtraHeaders(mtditidHeader)
      val result = await(connector.getSubmittedGiftAid(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return a BadRequest" in {
      val errorBody: APIErrorBodyModel = APIErrorBodyModel("BAD_REQUEST", "That request was bad")
      val expectedResult = APIErrorModel(BAD_REQUEST, errorBody)

      stubGetWithResponseBody(
        s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=1999", BAD_REQUEST, Json.toJson(errorBody).toString(), requestHeaders)
      implicit val hc = HeaderCarrier().withExtraHeaders(mtditidHeader)
      val result = await(connector.getSubmittedGiftAid(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return an InternalServerError " in {
      val errorBody: APIErrorBodyModel = APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong")

      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, errorBody)

      stubGetWithResponseBody(
        s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=1999", INTERNAL_SERVER_ERROR, Json.toJson(errorBody).toString(), requestHeaders)
      implicit val hc = HeaderCarrier().withExtraHeaders(mtditidHeader)
      val result = await(connector.getSubmittedGiftAid(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return an InternalServerError due to parsing error" in {
      val invalidJson = Json.obj(
        "giftAidPayments" -> ""
      )

      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)

      stubGetWithResponseBody(s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=1999", OK, invalidJson.toString(), requestHeaders)
      implicit val hc = HeaderCarrier().withExtraHeaders(mtditidHeader)
      val result = await(connector.getSubmittedGiftAid(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return an InternalServerError with parsing error when we can't parse the error body" in {
      val errorBody = "INTERNAL_SERVER_ERROR"

      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)

      stubGetWithResponseBody(s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=1999", INTERNAL_SERVER_ERROR, Json.toJson(errorBody).toString(), requestHeaders)
      implicit val hc = HeaderCarrier().withExtraHeaders(mtditidHeader)
      val result = await(connector.getSubmittedGiftAid(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return an InternalServerError when an unexpected status is thrown" in {
      val errorBody: APIErrorBodyModel = APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong")

      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, errorBody)

      stubGetWithResponseBody(
        s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=1999", IM_A_TEAPOT, Json.toJson(errorBody).toString(), requestHeaders)
      implicit val hc = HeaderCarrier().withExtraHeaders(mtditidHeader)
      val result = await(connector.getSubmittedGiftAid(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return an InternalServerError when an unexpected status is thrown and there is no body" in {

      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)

      stubGetWithoutResponseBody(
        s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=1999", IM_A_TEAPOT)
      implicit val hc = HeaderCarrier().withExtraHeaders(mtditidHeader)
      val result = await(connector.getSubmittedGiftAid(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return a ServiceUnavailableError" in {
      val errorBody: APIErrorBodyModel = APIErrorBodyModel("SERVICE_UNAVAILABLE", "Service went down")
      val expectedResult = APIErrorModel(SERVICE_UNAVAILABLE, errorBody)

      stubGetWithResponseBody(
        s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=1999", SERVICE_UNAVAILABLE, Json.toJson(errorBody).toString(), requestHeaders)
      implicit val hc = HeaderCarrier().withExtraHeaders(mtditidHeader)
      val result = await(connector.getSubmittedGiftAid(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

  }
}
