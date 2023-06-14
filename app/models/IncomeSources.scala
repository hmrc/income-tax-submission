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

import models.cis.AllCISDeductions
import models.employment.{AllEmploymentData, OtherEmploymentIncome}
import models.gains.InsurancePoliciesModel
import models.gifts.GiftAid
import models.pensions.Pensions
import models.statebenefits.AllStateBenefitsData
import play.api.libs.json.{Format, Json, OFormat}

case class IncomeSources(
                          errors: Option[Seq[(String, APIErrorBody)]] = None,
                          dividends: Option[Dividends] = None,
                          interest: Option[Seq[Interest]] = None,
                          giftAid: Option[GiftAid] = None,
                          employment: Option[AllEmploymentData] = None,
                          pensions: Option[Pensions] = None,
                          cis: Option[AllCISDeductions] = None,
                          stateBenefits: Option[AllStateBenefitsData] = None,
                          interestSavings: Option[SavingsIncomeDataModel] = None,
                          gains: Option[InsurancePoliciesModel] = None,
                          stockDividends: Option[StockDividends] = None,
                          otherEmploymentIncome: Option[OtherEmploymentIncome] = None
                        )

object IncomeSources {
  implicit val errorFormat: Format[APIErrorBody] = Json.format[APIErrorBody]
  implicit val format: OFormat[IncomeSources] = Json.format[IncomeSources]
}
