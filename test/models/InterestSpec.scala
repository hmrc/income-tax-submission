/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.libs.json.{JsObject, Json}
import utils.TestUtils

class InterestSpec extends TestUtils {

  val validJson: JsObject = Json.obj(
    "accountName" -> "someName",
    "incomeSourceId" -> "12345",
    "taxedUkInterest" -> 50,
    "untaxedUkInterest" -> 10
  )

  val validModel: Interest = Interest(
    accountName = "someName",
    incomeSourceId = "12345",
    taxedUkInterest = Some(50),
    untaxedUkInterest = Some(10)
  )

  "SubmittedInterestModel" should {

    "correctly parse to Json" in {

      Json.toJson(validModel) mustBe validJson
    }

    "correctly parse from Json" in {

      validJson.as[Interest] mustBe validModel

    }
  }

}
