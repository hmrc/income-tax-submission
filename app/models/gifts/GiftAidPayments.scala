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

package models.gifts

import play.api.libs.json.{Json, OFormat}
import utils.EncryptedValue

case class GiftAidPayments(nonUkCharitiesCharityNames: Option[List[String]],
                           currentYear: Option[BigDecimal],
                           oneOffCurrentYear: Option[BigDecimal],
                           currentYearTreatedAsPreviousYear: Option[BigDecimal],
                           nextYearTreatedAsCurrentYear: Option[BigDecimal],
                           nonUkCharities: Option[BigDecimal])

object GiftAidPayments {
  implicit val format: OFormat[GiftAidPayments] = Json.format[GiftAidPayments]
}

case class EncryptedGiftAidPayments(nonUkCharitiesCharityNames: Option[List[EncryptedValue]],
                                    currentYear: Option[EncryptedValue],
                                    oneOffCurrentYear: Option[EncryptedValue],
                                    currentYearTreatedAsPreviousYear: Option[EncryptedValue],
                                    nextYearTreatedAsCurrentYear: Option[EncryptedValue],
                                    nonUkCharities: Option[EncryptedValue])

object EncryptedGiftAidPayments {
  implicit val format: OFormat[EncryptedGiftAidPayments] = Json.format[EncryptedGiftAidPayments]
}
