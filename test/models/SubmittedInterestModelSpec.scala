
package models

import play.api.libs.json.{JsObject, Json}
import utils.TestUtils

class SubmittedInterestModelSpec extends TestUtils {

  val validJson: JsObject = Json.obj(
    "friendlyName" -> "someName",
    "incomeSourceId" -> "12345",
    "taxedUkInterest" -> 50,
    "untaxedUkInterest" -> 10
  )

  val validModel: SubmittedInterestModel = SubmittedInterestModel(
    friendlyName = "someName",
    incomeSourceId = "12345",
    taxedUkInterest = Some(50),
    untaxedUkInterest = Some(10)
  )

  "SubmittedInterestModel" should {

    "correctly parse to Json" in {

      Json.toJson(validModel) mustBe validJson
    }

    "correctly parse from Json" in {

      validJson.as[SubmittedInterestModel] mustBe validModel

    }
  }

}
