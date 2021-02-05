/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package connectors



import helpers.WiremockSpec
import models.{ErrorBodyModel, ErrorResponseModel, SubmittedDividendsModel}
import org.scalatestplus.play.PlaySpec
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier

class GetDividendsConnectorISpec extends PlaySpec with WiremockSpec{

  lazy val connector: IncomeTaxDividendsConnector = app.injector.instanceOf[IncomeTaxDividendsConnector]

  val nino: String = "AA123123A"
  val taxYear: Int = 1999
  val dividendResult: Option[BigDecimal] = Some(123456.78)

  "IncomeTaxDividendsConnector" should {
    "return a SubmittedDividendsModel" when {
      "all values are present" in {
        val expectedResult = Some(SubmittedDividendsModel(dividendResult, dividendResult))

        stubGetWithResponseBody(s"/income-tax-dividends/income-tax/nino/$nino/sources\\?taxYear=$taxYear&mtditid=123123123",
          OK, Json.toJson(expectedResult).toString())

        implicit val hc = HeaderCarrier()
        val result = await(connector.getSubmittedDividends(nino, taxYear, "123123123")(hc))

        result mustBe Right(expectedResult)
      }
    }

      "return a none when no dividend values found" in {

        val body = SubmittedDividendsModel(None, None)
        stubGetWithResponseBody(s"/income-tax-dividends/income-tax/nino/$nino/sources\\?taxYear=$taxYear&mtditid=123123123",
          OK, Json.toJson(body).toString())

        implicit val hc = HeaderCarrier()
        val result = await(connector.getSubmittedDividends(nino, taxYear, "123123123")(hc))

        result mustBe Right(None)
      }

    "return a None for notfound" in {
      stubGetWithResponseBody(s"/income-tax-dividends/income-tax/nino/$nino/sources\\?taxYear=1999&mtditid=123123123", NOT_FOUND, "{}")
      implicit val hc = HeaderCarrier()
      val result = await(connector.getSubmittedDividends(nino, taxYear, "123123123")(hc))

      result mustBe Right(None)
    }

    "return a BadRequest" in {
      val errorBody: ErrorBodyModel = ErrorBodyModel("BAD_REQUEST", "That request was bad")
      val expectedResult = ErrorResponseModel(BAD_REQUEST, errorBody)

      stubGetWithResponseBody(s"/income-tax-dividends/income-tax/nino/$nino/sources\\?taxYear=1999&mtditid=123123123", BAD_REQUEST, Json.toJson(errorBody).toString())
      implicit val hc = HeaderCarrier()
      val result = await(connector.getSubmittedDividends(nino, taxYear, "123123123")(hc))

      result mustBe Left(expectedResult)
    }

    "return an InternalServerError " in {
      val errorBody: ErrorBodyModel = ErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong")

      val expectedResult = ErrorResponseModel(INTERNAL_SERVER_ERROR, errorBody)

      stubGetWithResponseBody(s"/income-tax-dividends/income-tax/nino/$nino/sources\\?taxYear=1999&mtditid=123123123", INTERNAL_SERVER_ERROR, Json.toJson(errorBody).toString())
      implicit val hc = HeaderCarrier()
      val result = await(connector.getSubmittedDividends(nino, taxYear, "123123123")(hc))

      result mustBe Left(expectedResult)
    }

    "return an InternalServerError due to parsing error" in {
      val invalidJson = Json.obj(
        "ukDividends" -> ""
      )

      val expectedResult = ErrorResponseModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError)

      stubGetWithResponseBody(s"/income-tax-dividends/income-tax/nino/$nino/sources\\?taxYear=1999&mtditid=123123123", OK, invalidJson.toString())
      implicit val hc = HeaderCarrier()
      val result = await(connector.getSubmittedDividends(nino, taxYear, "123123123")(hc))

      result mustBe Left(expectedResult)
    }

    "return an InternalServerError with parsing error when we can't parse the error body" in {
      val errorBody = "INTERNAL_SERVER_ERROR"

      val expectedResult = ErrorResponseModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError)

      stubGetWithResponseBody(s"/income-tax-dividends/income-tax/nino/$nino/sources\\?taxYear=1999&mtditid=123123123", INTERNAL_SERVER_ERROR, Json.toJson(errorBody).toString())
      implicit val hc = HeaderCarrier()
      val result = await(connector.getSubmittedDividends(nino, taxYear, "123123123")(hc))

      result mustBe Left(expectedResult)
    }

    "return an InternalServerError when an unexpected status is thrown" in {
      val errorBody: ErrorBodyModel = ErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong")

      val expectedResult = ErrorResponseModel(INTERNAL_SERVER_ERROR, errorBody)

      stubGetWithResponseBody(s"/income-tax-dividends/income-tax/nino/$nino/sources\\?taxYear=1999&mtditid=123123123", IM_A_TEAPOT, Json.toJson(errorBody).toString())
      implicit val hc = HeaderCarrier()
      val result = await(connector.getSubmittedDividends(nino, taxYear, "123123123")(hc))

      result mustBe Left(expectedResult)
    }

    "return an InternalServerError when an unexpected status is thrown and there is no body" in {

      val expectedResult = ErrorResponseModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError)

      stubGetWithoutResponseBody(s"/income-tax-dividends/income-tax/nino/$nino/sources\\?taxYear=1999&mtditid=123123123", IM_A_TEAPOT)
      implicit val hc = HeaderCarrier()
      val result = await(connector.getSubmittedDividends(nino, taxYear, "123123123")(hc))

      result mustBe Left(expectedResult)
    }

    "return a ServiceUnavailableError" in {
      val errorBody: ErrorBodyModel = ErrorBodyModel("SERVICE_UNAVAILABLE", "Service went down")
      val expectedResult = ErrorResponseModel(SERVICE_UNAVAILABLE, errorBody)

      stubGetWithResponseBody(s"/income-tax-dividends/income-tax/nino/$nino/sources\\?taxYear=1999&mtditid=123123123", SERVICE_UNAVAILABLE, Json.toJson(errorBody).toString())
      implicit val hc = HeaderCarrier()
      val result = await(connector.getSubmittedDividends(nino, taxYear, "123123123")(hc))

      result mustBe Left(expectedResult)
    }

  }
}
