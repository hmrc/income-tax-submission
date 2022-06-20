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

import common.IncomeSources.{GIFT_AID, INTEREST}
import play.api.libs.json.Json
import utils.TestUtils

class GetExclusionsDataModelSpec extends TestUtils {

  private val validModel: GetExclusionsDataModel = GetExclusionsDataModel(Seq(
    ExcludeJourneyModel(INTEREST, Some("immahash")),
    ExcludeJourneyModel(GIFT_AID, None)
  ))

  private val validJson = Json.obj("journeys" -> Json.arr(
    Json.obj("journey" -> INTEREST, "hash" -> "immahash"),
    Json.obj("journey" -> GIFT_AID)
  ))

  "GetExclusionsDataModel" must {

    "correctly write to json" in {
      Json.toJson(validModel) mustBe validJson
    }

  }

  ".toJson" must {

    "correctly convert the model to json" in {
      validModel.toJson mustBe validJson
    }

  }
}
