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

package models.cis

import models.mongo.TextAndKey
import play.api.libs.json.{Json, OFormat}
import utils.EncryptableInstances._
import utils.EncryptableSyntax._
import utils.{EncryptedValue, SecureGCMCipher}

case class CISSource(totalDeductionAmount: Option[BigDecimal],
                     totalCostOfMaterials: Option[BigDecimal],
                     totalGrossAmountPaid: Option[BigDecimal],
                     cisDeductions: Seq[CISDeductions]) {

  def encrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedCISSource = EncryptedCISSource(
    totalDeductionAmount = totalDeductionAmount.map(_.encrypted),
    totalCostOfMaterials = totalCostOfMaterials.map(_.encrypted),
    totalGrossAmountPaid = totalGrossAmountPaid.map(_.encrypted),
    cisDeductions = cisDeductions.map(_.encrypted)
  )
}

object CISSource {
  implicit val format: OFormat[CISSource] = Json.format[CISSource]
}

case class EncryptedCISSource(totalDeductionAmount: Option[EncryptedValue],
                              totalCostOfMaterials: Option[EncryptedValue],
                              totalGrossAmountPaid: Option[EncryptedValue],
                              cisDeductions: Seq[EncryptedCISDeductions]) {

  def decrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): CISSource = CISSource(
    totalDeductionAmount = totalDeductionAmount.map(_.decrypted[BigDecimal]),
    totalCostOfMaterials = totalCostOfMaterials.map(_.decrypted[BigDecimal]),
    totalGrossAmountPaid = totalGrossAmountPaid.map(_.decrypted[BigDecimal]),
    cisDeductions = cisDeductions.map(_.decrypted)
  )
}

object EncryptedCISSource {
  implicit val format: OFormat[EncryptedCISSource] = Json.format[EncryptedCISSource]
}