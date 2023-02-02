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

package models.cis

import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.crypto.EncryptedValue
import utils.AesGcmAdCrypto

case class AllCISDeductions(customerCISDeductions: Option[CISSource],
                            contractorCISDeductions: Option[CISSource]) {

  def encrypted()(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): EncryptedAllCISDeductions = EncryptedAllCISDeductions(
    customerCISDeductions = customerCISDeductions.map(_.encrypted()),
    contractorCISDeductions = contractorCISDeductions.map(_.encrypted())
  )
}

object AllCISDeductions {
  implicit val format: OFormat[AllCISDeductions] = Json.format[AllCISDeductions]
}

case class EncryptedAllCISDeductions(customerCISDeductions: Option[EncryptedCISSource],
                                     contractorCISDeductions: Option[EncryptedCISSource]) {

  def decrypted()(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): AllCISDeductions = AllCISDeductions(
    customerCISDeductions = customerCISDeductions.map(_.decrypted()),
    contractorCISDeductions = contractorCISDeductions.map(_.decrypted())
  )
}

object EncryptedAllCISDeductions {
  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]

  implicit val format: Format[EncryptedAllCISDeductions] = Json.format[EncryptedAllCISDeductions]
}