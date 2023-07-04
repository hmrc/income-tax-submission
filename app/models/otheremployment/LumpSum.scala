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

case class LumpSum(employerName: String,
                   employerRef: String,
                   taxableLumpSumsAndCertainIncome: Option[TaxableLumpSumsAndCertainIncome],
                   benefitFromEmployerFinancedRetirementScheme: Option[BenefitFromEmployerFinancedRetirementScheme],
                   redundancyCompensationPaymentsOverExemption: Option[RedundancyCompensationPaymentsOverExemption],
                   redundancyCompensationPaymentsUnderExemption: Option[RedundancyCompensationPaymentsUnderExemption]) {

  def encrypted()(implicit secureGCMCipher: AesGcmAdCrypto, associatedText: String): EncryptedLumpSum = EncryptedLumpSum(
    employerName = employerName.encrypted,
    employerRef = employerRef.encrypted,
    taxableLumpSumsAndCertainIncome = taxableLumpSumsAndCertainIncome.map(_.encrypted()),
    benefitFromEmployerFinancedRetirementScheme = benefitFromEmployerFinancedRetirementScheme.map(_.encrypted()),
    redundancyCompensationPaymentsOverExemption = redundancyCompensationPaymentsOverExemption.map(_.encrypted()),
    redundancyCompensationPaymentsUnderExemption = redundancyCompensationPaymentsUnderExemption.map(_.encrypted())
  )
}

object LumpSum {
  implicit val format: OFormat[LumpSum] = Json.format[LumpSum]
}

case class EncryptedLumpSum(employerName: EncryptedValue,
                            employerRef: EncryptedValue,
                            taxableLumpSumsAndCertainIncome: Option[EncryptedTaxableLumpSumsAndCertainIncome],
                            benefitFromEmployerFinancedRetirementScheme: Option[EncryptedBenefitFromEmployerFinancedRetirementScheme],
                            redundancyCompensationPaymentsOverExemption: Option[EncryptedRedundancyCompensationPaymentsOverExemption],
                            redundancyCompensationPaymentsUnderExemption: Option[EncryptedRedundancyCompensationPaymentsUnderExemption]) {

  def decrypted()(implicit secureGCMCipher: AesGcmAdCrypto, associatedText: String): LumpSum = LumpSum(
    employerName = employerName.decrypted[String],
    employerRef = employerRef.decrypted[String],
    taxableLumpSumsAndCertainIncome = taxableLumpSumsAndCertainIncome.map(_.decrypted()),
    benefitFromEmployerFinancedRetirementScheme = benefitFromEmployerFinancedRetirementScheme.map(_.decrypted()),
    redundancyCompensationPaymentsOverExemption = redundancyCompensationPaymentsOverExemption.map(_.decrypted()),
    redundancyCompensationPaymentsUnderExemption = redundancyCompensationPaymentsUnderExemption.map(_.decrypted())
  )
}

object EncryptedLumpSum {
  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]

  implicit val format: Format[EncryptedLumpSum] = Json.format[EncryptedLumpSum]
}
