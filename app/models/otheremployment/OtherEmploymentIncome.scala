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

import models.otheremployment._
import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.crypto.EncryptedValue

import java.time.Instant

case class OtherEmploymentIncome(submittedOn: Option[Instant] = None,
                                 shareOptions: Option[Set[ShareOption]] = None,
                                 sharesAwardedOrReceived: Option[Set[SharesAwardedOrReceived]] = None,
                                 lumpSums: Option[Set[LumpSum]] = None,
                                 disability: Option[Disability] = None,
                                 foreignService: Option[ForeignService] = None)

object OtherEmploymentIncome {
  implicit val otherEmploymentIncomeFormat: OFormat[OtherEmploymentIncome] = Json.format[OtherEmploymentIncome]
}

case class EncryptedOtherEmploymentIncome(submittedOn: Option[EncryptedValue],
                                          shareOptions: Option[Set[EncryptedShareOption]],
                                          sharesAwardedOrReceived: Option[Set[EncryptedSharesAwardedOrReceived]],
                                          lumpSums: Option[Set[EncryptedLumpSum]],
                                          disability: Option[EncryptedDisability],
                                          foreignService: Option[EncryptedForeignService])

object EncryptedOtherEmploymentIncome {
  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]

  implicit val format: Format[EncryptedOtherEmploymentIncome] = Json.format[EncryptedOtherEmploymentIncome]
}

