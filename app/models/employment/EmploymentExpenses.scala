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

package models.employment

import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.crypto.EncryptedValue

case class EmploymentExpenses(submittedOn: Option[String],
                              dateIgnored: Option[String],
                              totalExpenses: Option[BigDecimal],
                              expenses: Option[Expenses])

object EmploymentExpenses {
  implicit val format: OFormat[EmploymentExpenses] = Json.format[EmploymentExpenses]
}

case class EncryptedEmploymentExpenses(submittedOn: Option[EncryptedValue],
                                       dateIgnored: Option[EncryptedValue],
                                       totalExpenses: Option[EncryptedValue],
                                       expenses: Option[EncryptedExpenses])

object EncryptedEmploymentExpenses {
  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]

  implicit val format: Format[EncryptedEmploymentExpenses] = Json.format[EncryptedEmploymentExpenses]
}
