
package models

import play.api.libs.json.{JsObject, Json}
import utils.TestUtils

class InterestResponseModelSpec extends TestUtils {

  val validJson: JsObject = Json.obj(
    "taxedUkInterest" -> 50,
    "untaxedUkInterest" -> 10
  )

  val validModel: InterestResponseModel = InterestResponseModel(
    taxedUkInterest = Some(10),
    untaxedUkInterest = Some(20)
  )

  "InterestResponseModel" should {

    "correctly parse from Json" in {
      validJson.as[InterestResponseModel] mustBe validModel
    }

    "correctly parse to Json" in {
      Json.toJson(validModel) mustBe validJson
    }
  }

}
