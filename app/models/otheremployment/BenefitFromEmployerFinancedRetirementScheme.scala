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

case class BenefitFromEmployerFinancedRetirementScheme(amount: BigDecimal,
                                                       exemptAmount: Option[BigDecimal] = None,
                                                       taxPaid: Option[BigDecimal] = None,
                                                       taxTakenOffInEmployment: Option[Boolean] = None) {

  def encrypted()(implicit secureGCMCipher: AesGcmAdCrypto, associatedText: String): EncryptedBenefitFromEmployerFinancedRetirementScheme =
    EncryptedBenefitFromEmployerFinancedRetirementScheme(
      amount = amount.encrypted,
      exemptAmount = exemptAmount.map(_.encrypted),
      taxPaid = taxPaid.map(_.encrypted),
      taxTakenOffInEmployment = taxTakenOffInEmployment.map(_.encrypted)
    )
}

object BenefitFromEmployerFinancedRetirementScheme {
  implicit val format: OFormat[BenefitFromEmployerFinancedRetirementScheme] = Json.format[BenefitFromEmployerFinancedRetirementScheme]
}

case class EncryptedBenefitFromEmployerFinancedRetirementScheme(amount: EncryptedValue,
                                                                exemptAmount: Option[EncryptedValue] = None,
                                                                taxPaid: Option[EncryptedValue] = None,
                                                                taxTakenOffInEmployment: Option[EncryptedValue] = None) {

  def decrypted()(implicit secureGCMCipher: AesGcmAdCrypto, associatedText: String): BenefitFromEmployerFinancedRetirementScheme =
    BenefitFromEmployerFinancedRetirementScheme(
      amount = amount.decrypted[BigDecimal],
      exemptAmount = exemptAmount.map(_.decrypted[BigDecimal]),
      taxPaid = taxPaid.map(_.decrypted[BigDecimal]),
      taxTakenOffInEmployment = taxTakenOffInEmployment.map(_.decrypted[Boolean])
    )
}

object EncryptedBenefitFromEmployerFinancedRetirementScheme {
  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]

  implicit val format: Format[EncryptedBenefitFromEmployerFinancedRetirementScheme] = Json.format[EncryptedBenefitFromEmployerFinancedRetirementScheme]
}