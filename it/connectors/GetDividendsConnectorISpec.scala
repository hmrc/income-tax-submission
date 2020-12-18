/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package connectors



import helpers.WiremockSpec
import models.{InternalServerError, NotFoundError, ServiceUnavailableError, SubmittedDividendsModel}
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


     "return an InternalServerError" in {
      val invalidJson = Json.obj(
        "ukDividends" -> ""
      )

      val expectedResult = InternalServerError

      stubGetWithResponseBody(s"/income-tax-dividends/income-tax/nino/$nino/sources\\?taxYear=1999&mtditid=123123123", OK, invalidJson.toString())
      implicit val hc = HeaderCarrier()
      val result = await(connector.getSubmittedDividends(nino, taxYear, "123123123")(hc))

      result mustBe Left(expectedResult)
    }
    "return a ServiceUnavailableError" in {
      val expectedResult = ServiceUnavailableError

      stubGetWithResponseBody(s"/income-tax-dividends/income-tax/nino/$nino/sources\\?taxYear=1999&mtditid=123123123", SERVICE_UNAVAILABLE, "{}")
      implicit val hc = HeaderCarrier()
      val result = await(connector.getSubmittedDividends(nino, taxYear, "123123123")(hc))

      result mustBe Left(expectedResult)
    }
    "return a None for notfound" in {
      stubGetWithResponseBody(s"/income-tax-dividends/income-tax/nino/$nino/sources\\?taxYear=1999&mtditid=123123123", NOT_FOUND, "{}")
      implicit val hc = HeaderCarrier()
      val result = await(connector.getSubmittedDividends(nino, taxYear, "123123123")(hc))

      result mustBe Right(None)
    }
  }
}
