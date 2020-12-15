
package connectors

import helpers.WiremockSpec
import models.SubmittedInterestModel
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

        val expectedResult = Some(SubmittedInterestModel(accountName, incomeSourceId, taxedUkInterest, untaxedUkInterest))

        stubGetWithResponseBody(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear&mtditid=$mtditid",
          OK, Json.toJson(expectedResult).toString())

        implicit val hc = HeaderCarrier()
        val result = await(connector.getSubmittedInterest(nino, taxYear, mtditid)(hc))

        result mustBe Right(expectedResult)

      }
    }
  }
}
