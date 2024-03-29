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

package models

import models.gifts.{GiftAid, GiftAidPayments, Gifts}
import play.api.libs.json.{JsObject, Json}
import utils.TestUtils

class GiftAidSpec extends TestUtils {

  val validGiftAidPaymentsModel: GiftAidPayments = GiftAidPayments(
    nonUkCharitiesCharityNames = Some(List("non uk charity name", "non uk charity name 2")),
    currentYear = Some(12345.67),
    oneOffCurrentYear = Some(12345.67),
    currentYearTreatedAsPreviousYear = Some(12345.67),
    nextYearTreatedAsCurrentYear = Some(12345.67),
    nonUkCharities = Some(12345.67)
  )

  val validGiftsModel: Gifts = Gifts(
    investmentsNonUkCharitiesCharityNames = Some(List("charity name")),
    landAndBuildings = Some(12345.67),
    sharesOrSecurities = Some(12345.67),
    investmentsNonUkCharities = Some(12345.67)
  )

  val validGiftAidModel: GiftAid = GiftAid(
    Some(validGiftAidPaymentsModel),
    Some(validGiftsModel)
  )

  val validJson: JsObject = Json.obj(
    "giftAidPayments" -> Json.obj(
      "nonUkCharitiesCharityNames" -> Json.arr(
        "non uk charity name",
        "non uk charity name 2"
      ),
      "currentYear" -> 12345.67,
      "oneOffCurrentYear" -> 12345.67,
      "currentYearTreatedAsPreviousYear" -> 12345.67,
      "nextYearTreatedAsCurrentYear" -> 12345.67,
      "nonUkCharities" -> 12345.67
    ),
    "gifts" -> Json.obj(
      "investmentsNonUkCharitiesCharityNames" -> Json.arr(
        "charity name"
      ),
      "landAndBuildings" -> 12345.67,
      "sharesOrSecurities" -> 12345.67,
      "investmentsNonUkCharities" -> 12345.67
    )
  )

  "GiftAidSubmission" should {

    "parse from json" in {
      validJson.as[GiftAid] mustBe validGiftAidModel
    }

    "parse to json" in {
      Json.toJson(validGiftAidModel) mustBe validJson
    }

  }

}
