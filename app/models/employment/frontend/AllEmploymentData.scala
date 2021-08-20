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

package models.employment.frontend

import play.api.libs.json.{Json, OFormat}
import utils.EncryptedValue

case class AllEmploymentData(hmrcEmploymentData: Seq[EmploymentSource],
                             hmrcExpenses: Option[EmploymentExpenses],
                             customerEmploymentData: Seq[EmploymentSource],
                             customerExpenses: Option[EmploymentExpenses])

object AllEmploymentData {
  implicit val format: OFormat[AllEmploymentData] = Json.format[AllEmploymentData]
}

case class EncryptedAllEmploymentData(hmrcEmploymentData: Seq[EncryptedEmploymentSource],
                                      hmrcExpenses: Option[EncryptedEmploymentExpenses],
                                      customerEmploymentData: Seq[EncryptedEmploymentSource],
                                      customerExpenses: Option[EncryptedEmploymentExpenses])

object EncryptedAllEmploymentData {
  implicit val format: OFormat[EncryptedAllEmploymentData] = Json.format[EncryptedAllEmploymentData]
}

case class EmploymentSource(employmentId: String,
                            employerName: String,
                            employerRef: Option[String],
                            payrollId: Option[String],
                            startDate: Option[String],
                            cessationDate: Option[String],
                            dateIgnored: Option[String],
                            submittedOn: Option[String],
                            employmentData: Option[EmploymentData],
                            employmentBenefits: Option[EmploymentBenefits])

object EmploymentSource {
  implicit val format: OFormat[EmploymentSource] = Json.format[EmploymentSource]
}

case class EncryptedEmploymentSource(employmentId: EncryptedValue,
                                     employerName: EncryptedValue,
                                     employerRef: Option[EncryptedValue],
                                     payrollId: Option[EncryptedValue],
                                     startDate: Option[EncryptedValue],
                                     cessationDate: Option[EncryptedValue],
                                     dateIgnored: Option[EncryptedValue],
                                     submittedOn: Option[EncryptedValue],
                                     employmentData: Option[EncryptedEmploymentData],
                                     employmentBenefits: Option[EncryptedEmploymentBenefits])

object EncryptedEmploymentSource {
  implicit val format: OFormat[EncryptedEmploymentSource] = Json.format[EncryptedEmploymentSource]
}
