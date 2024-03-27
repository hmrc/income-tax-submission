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
import utils.CypherSyntax.{DecryptableOps, EncryptableOps}


case class CISSource(totalDeductionAmount: Option[BigDecimal],
                     totalCostOfMaterials: Option[BigDecimal],
                     totalGrossAmountPaid: Option[BigDecimal],
                     cisDeductions: Seq[CISDeductions]) {

  def encrypted()(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): EncryptedCISSource = EncryptedCISSource(
    totalDeductionAmount = totalDeductionAmount.map(_.encrypted),
    totalCostOfMaterials = totalCostOfMaterials.map(_.encrypted),
    totalGrossAmountPaid = totalGrossAmountPaid.map(_.encrypted),
    cisDeductions = cisDeductions.map(_.encrypted())
  )
}

object CISSource {
  val empty: CISSource = CISSource(None, None, None, Seq.empty)
  implicit val format: OFormat[CISSource] = Json.format[CISSource]
}

case class EncryptedCISSource(totalDeductionAmount: Option[EncryptedValue],
                              totalCostOfMaterials: Option[EncryptedValue],
                              totalGrossAmountPaid: Option[EncryptedValue],
                              cisDeductions: Seq[EncryptedCISDeductions]) {

  def decrypted()(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): CISSource = CISSource(
    totalDeductionAmount = totalDeductionAmount.map(_.decrypted[BigDecimal]),
    totalCostOfMaterials = totalCostOfMaterials.map(_.decrypted[BigDecimal]),
    totalGrossAmountPaid = totalGrossAmountPaid.map(_.decrypted[BigDecimal]),
    cisDeductions = cisDeductions.map(_.decrypted())
  )
}

object EncryptedCISSource {
  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]

  implicit val format: Format[EncryptedCISSource] = Json.format[EncryptedCISSource]
}