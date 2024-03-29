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

case class EmploymentData(submittedOn: String,
                          employmentSequenceNumber: Option[String],
                          companyDirector: Option[Boolean],
                          closeCompany: Option[Boolean],
                          directorshipCeasedDate: Option[String],
                          occPen: Option[Boolean],
                          disguisedRemuneration: Option[Boolean],
                          offPayrollWorker: Option[Boolean],
                          pay: Option[Pay],
                          deductions: Option[Deductions]) {

  lazy val hasOccPen: Boolean = occPen.contains(true)
}

object EmploymentData {
  implicit val formats: OFormat[EmploymentData] = Json.format[EmploymentData]
}

case class EncryptedEmploymentData(submittedOn: EncryptedValue,
                                   employmentSequenceNumber: Option[EncryptedValue],
                                   companyDirector: Option[EncryptedValue],
                                   closeCompany: Option[EncryptedValue],
                                   directorshipCeasedDate: Option[EncryptedValue],
                                   occPen: Option[EncryptedValue],
                                   disguisedRemuneration: Option[EncryptedValue],
                                   offPayrollWorker: Option[EncryptedValue],
                                   pay: Option[EncryptedPay],
                                   deductions: Option[EncryptedDeductions])

object EncryptedEmploymentData {
  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]

  implicit val formats: Format[EncryptedEmploymentData] = Json.format[EncryptedEmploymentData]
}
