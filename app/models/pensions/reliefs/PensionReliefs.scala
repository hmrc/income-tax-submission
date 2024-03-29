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

package models.pensions.reliefs

import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.crypto.EncryptedValue

case class PensionReliefs(submittedOn: String,
                          deletedOn: Option[String],
                          pensionReliefs: Reliefs)

object PensionReliefs {
  implicit val formats: OFormat[PensionReliefs] = Json.format[PensionReliefs]
}

case class EncryptedPensionReliefs(submittedOn: EncryptedValue,
                                   deletedOn: Option[EncryptedValue],
                                   pensionReliefs: EncryptedReliefs)

object EncryptedPensionReliefs {
  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]

  implicit val formats: Format[EncryptedPensionReliefs] = Json.format[EncryptedPensionReliefs]
}
