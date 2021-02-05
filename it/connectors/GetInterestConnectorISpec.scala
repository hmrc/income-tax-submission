
package connectors

import helpers.WiremockSpec
import models.{ErrorBodyModel, ErrorResponseModel, SubmittedInterestModel}
import org.scalatestplus.play.PlaySpec
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier

class GetInterestConnectorISpec extends PlaySpec with WiremockSpec {

  lazy val connector: IncomeTaxInterestConnector = app.injector.instanceOf[IncomeTaxInterestConnector]

  val nino: String = "AA123123A"
  val taxYear: Int = 2020
  val mtditid: String = "123123123"

  val accountName: String = "SomeName"
  val incomeSourceId: String = "12345"
  val untaxedUkInterest: Option[BigDecimal] = Some(12345.67)
  val taxedUkInterest: Option[BigDecimal] = Some(12345.67)


  "IncomeTaxInterestConnector" should {
    "return a SubmittedInterestModel" when {

      "all values are present" in {

        val expectedResult = Some(Seq(SubmittedInterestModel(accountName, incomeSourceId, taxedUkInterest, untaxedUkInterest)))

        stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear&mtditid=$mtditid",
          OK, Json.toJson(expectedResult).toString())

        implicit val hc = HeaderCarrier()
        val result = await(connector.getSubmittedInterest(nino, taxYear, mtditid)(hc))

        result mustBe Right(expectedResult)

      }
    }

    "return a none when no interest values are found" in {

      val body = Seq.empty[SubmittedInterestModel]
      stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear&mtditid=$mtditid",
        OK, Json.toJson(body).toString())

      implicit val hc = HeaderCarrier()
      val result = await(connector.getSubmittedInterest(nino, taxYear, mtditid)(hc))

      result mustBe Right(None)
    }

    "return a none for a NotFound" in {

      stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear&mtditid=$mtditid",
        NOT_FOUND, "{}")

      implicit val hc = HeaderCarrier()
      val result = await(connector.getSubmittedInterest(nino, taxYear, mtditid)(hc))

      result mustBe Right(None)
    }

    "return a BadRequest" in {
      val errorBody: ErrorBodyModel = ErrorBodyModel("BAD_REQUEST", "That request was bad")
      val expectedResult = ErrorResponseModel(BAD_REQUEST, errorBody)

      stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear&mtditid=$mtditid",
        BAD_REQUEST, Json.toJson(errorBody).toString())

      implicit val hc = HeaderCarrier()
      val result = await(connector.getSubmittedInterest(nino, taxYear, mtditid)(hc))

      result mustBe Left(expectedResult)
    }

    "return an InternalServerError" in {
      val errorBody: ErrorBodyModel = ErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong")

      val expectedResult = ErrorResponseModel(INTERNAL_SERVER_ERROR, errorBody)

      stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear&mtditid=$mtditid",
        INTERNAL_SERVER_ERROR, Json.toJson(errorBody).toString())

      implicit val hc = HeaderCarrier()
      val result = await(connector.getSubmittedInterest(nino, taxYear, mtditid)(hc))

      result mustBe Left(expectedResult)

    }

    "return an InternalServerError due to parsing error" in {

        val invalidJson = Json.obj(
          "accountName" -> ""
        )

        val expectedResult = ErrorResponseModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError)

        stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear&mtditid=$mtditid",
          OK, invalidJson.toString())

        implicit val hc = HeaderCarrier()
        val result = await(connector.getSubmittedInterest(nino, taxYear, mtditid)(hc))

        result mustBe Left(expectedResult)
    }

    "return an InternalServerError with parsing error when we can't parse the error body" in {
      val errorBody = "INTERNAL_SERVER_ERROR"

      val expectedResult = ErrorResponseModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError)

      stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear&mtditid=$mtditid",
        INTERNAL_SERVER_ERROR, Json.toJson(errorBody).toString())

      implicit val hc = HeaderCarrier()
      val result = await(connector.getSubmittedInterest(nino, taxYear, mtditid)(hc))

      result mustBe Left(expectedResult)
    }

    "return an InternalServerError when an unexpected status is thrown" in {
      val errorBody: ErrorBodyModel = ErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong")

      val expectedResult = ErrorResponseModel(INTERNAL_SERVER_ERROR, errorBody)

      stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear&mtditid=$mtditid",
        IM_A_TEAPOT, Json.toJson(errorBody).toString())

      implicit val hc = HeaderCarrier()
      val result = await(connector.getSubmittedInterest(nino, taxYear, mtditid)(hc))

      result mustBe Left(expectedResult)

    }

    "return an InternalServerError when an unexpected status is thrown and there is no body" in {

      val expectedResult = ErrorResponseModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError)

      stubGetWithoutResponseBody(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear&mtditid=$mtditid",
        IM_A_TEAPOT)

      implicit val hc = HeaderCarrier()
      val result = await(connector.getSubmittedInterest(nino, taxYear, mtditid)(hc))

      result mustBe Left(expectedResult)

    }

    "return a ServiceUnavailableError" in {

      val errorBody = ErrorBodyModel("SERVICE_UNAVAILABLE", "Something went wrong")
      val expectedResult = ErrorResponseModel(SERVICE_UNAVAILABLE, errorBody)

      stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear&mtditid=$mtditid",
        SERVICE_UNAVAILABLE, Json.toJson(errorBody).toString())

      implicit val hc = HeaderCarrier()
      val result = await(connector.getSubmittedInterest(nino, taxYear, mtditid)(hc))

      result mustBe Left(expectedResult)
    }


  }
}
