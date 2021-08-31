/*
 * Copyright 2021 HM Revenue & Customs
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

package models.employment.shared

import play.api.libs.json.{Json, OFormat}
import utils.EncryptedValue

case class Expenses(businessTravelCosts: Option[BigDecimal],
                    jobExpenses: Option[BigDecimal],
                    flatRateJobExpenses: Option[BigDecimal],
                    professionalSubscriptions: Option[BigDecimal],
                    hotelAndMealExpenses: Option[BigDecimal],
                    otherAndCapitalAllowances: Option[BigDecimal],
                    vehicleExpenses: Option[BigDecimal],
                    mileageAllowanceRelief: Option[BigDecimal])

object Expenses {
  implicit val format: OFormat[Expenses] = Json.format[Expenses]
}

case class EncryptedExpenses(businessTravelCosts: Option[EncryptedValue],
                             jobExpenses: Option[EncryptedValue],
                             flatRateJobExpenses: Option[EncryptedValue],
                             professionalSubscriptions: Option[EncryptedValue],
                             hotelAndMealExpenses: Option[EncryptedValue],
                             otherAndCapitalAllowances: Option[EncryptedValue],
                             vehicleExpenses: Option[EncryptedValue],
                             mileageAllowanceRelief: Option[EncryptedValue])

object EncryptedExpenses {
  implicit val format: OFormat[EncryptedExpenses] = Json.format[EncryptedExpenses]
}
