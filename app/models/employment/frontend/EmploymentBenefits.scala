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

import models.employment.shared.{Benefits, EncryptedBenefits}
import play.api.libs.json.{Json, OFormat}
import utils.EncryptedValue

case class EmploymentBenefits(submittedOn: String,
                              benefits: Option[Benefits])

object EmploymentBenefits {
  implicit val formats: OFormat[EmploymentBenefits] = Json.format[EmploymentBenefits]
}

case class EncryptedEmploymentBenefits(submittedOn: EncryptedValue,
                                       benefits: Option[EncryptedBenefits])

object EncryptedEmploymentBenefits {
  implicit val formats: OFormat[EncryptedEmploymentBenefits] = Json.format[EncryptedEmploymentBenefits]
}
