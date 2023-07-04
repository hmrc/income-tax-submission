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

package models.otheremployment

import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.crypto.EncryptedValue
import utils.AesGcmAdCrypto
import utils.CypherSyntax.{DecryptableOps, EncryptableOps}

case class Disability(customerReference: Option[String] = None,
                      amountDeducted: BigDecimal) {

  def encrypted()(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): EncryptedDisability = EncryptedDisability(
    customerReference = customerReference.map(_.encrypted),
    amountDeducted = amountDeducted.encrypted
  )
}

object Disability {
  implicit val format: OFormat[Disability] = Json.format[Disability]
}

case class EncryptedDisability(customerReference: Option[EncryptedValue] = None,
                               amountDeducted: EncryptedValue) {

  def decrypted()(implicit secureGCMCipher: AesGcmAdCrypto, associatedText: String): Disability = Disability(
    customerReference = customerReference.map(_.decrypted[String]),
    amountDeducted = amountDeducted.decrypted[BigDecimal]
  )
}

object EncryptedDisability {
  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]

  implicit val format: Format[EncryptedDisability] = Json.format[EncryptedDisability]
}
