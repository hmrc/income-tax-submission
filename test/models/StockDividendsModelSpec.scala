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

import play.api.libs.json.{JsObject, Json}
import builders.models.StockDividendsBuilder.anStockDividends
import utils.TestUtils

class StockDividendsModelSpec extends TestUtils {

  val validJson: JsObject = Json.obj(
    "submittedOn" -> "",
    "foreignDividend" -> Json.toJson(anStockDividends.foreignDividend),
    "dividendIncomeReceivedWhilstAbroad" -> Json.toJson(anStockDividends.dividendIncomeReceivedWhilstAbroad),
    "stockDividend" -> Json.toJson(anStockDividends.stockDividend),
    "redeemableShares" -> Json.toJson(anStockDividends.redeemableShares),
    "bonusIssuesOfSecurities" -> Json.toJson(anStockDividends.bonusIssuesOfSecurities),
    "closeCompanyLoansWrittenOff" -> Json.toJson(anStockDividends.closeCompanyLoansWrittenOff)
  )


  val validModel: StockDividends = anStockDividends

  "StockDividendsResponseModel" should {

    "correctly parse from Json" in {
      validJson.as[StockDividends] mustBe validModel
    }

    "correctly parse to Json" in {
      Json.toJson(validModel) mustBe validJson
    }

  }

}
