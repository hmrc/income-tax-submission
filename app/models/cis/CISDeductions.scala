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
import utils.CypherSyntax.{DecryptableOps, EncryptableOps}
import utils.AesGcmAdCrypto

case class CISDeductions(fromDate: String,
                         toDate: String,
                         contractorName: Option[String],
                         employerRef: String,
                         totalDeductionAmount: Option[BigDecimal],
                         totalCostOfMaterials: Option[BigDecimal],
                         totalGrossAmountPaid: Option[BigDecimal],
                         periodData: Seq[PeriodData]) {

  def encrypted()(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): EncryptedCISDeductions = EncryptedCISDeductions(
    fromDate = fromDate.encrypted(aesGcmAdCrypto, associatedText),
    toDate = toDate.encrypted,
    contractorName = contractorName.map(_.encrypted),
    employerRef = employerRef.encrypted,
    totalDeductionAmount = totalDeductionAmount.map(_.encrypted),
    totalCostOfMaterials = totalCostOfMaterials.map(_.encrypted),
    totalGrossAmountPaid = totalGrossAmountPaid.map(_.encrypted),
    periodData = periodData.map(_.encrypted())
  )
}

object CISDeductions {
  implicit val format: OFormat[CISDeductions] = Json.format[CISDeductions]
}

case class EncryptedCISDeductions(fromDate: EncryptedValue,
                                  toDate: EncryptedValue,
                                  contractorName: Option[EncryptedValue],
                                  employerRef: EncryptedValue,
                                  totalDeductionAmount: Option[EncryptedValue],
                                  totalCostOfMaterials: Option[EncryptedValue],
                                  totalGrossAmountPaid: Option[EncryptedValue],
                                  periodData: Seq[EncryptedGetPeriodData]) {

  def decrypted()(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): CISDeductions = CISDeductions(
    fromDate = fromDate.decrypted[String],
    toDate = toDate.decrypted[String],
    contractorName = contractorName.map(ev => ev.decrypted[String]),
    employerRef = employerRef.decrypted[String],
    totalDeductionAmount = totalDeductionAmount.map(ev => ev.decrypted[BigDecimal]),
    totalCostOfMaterials = totalCostOfMaterials.map(ev => ev.decrypted[BigDecimal]),
    totalGrossAmountPaid = totalGrossAmountPaid.map(ev => ev.decrypted[BigDecimal]),
    periodData = periodData.map(_.decrypted())
  )
}

object EncryptedCISDeductions {
  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]

  implicit val format: Format[EncryptedCISDeductions] = Json.format[EncryptedCISDeductions]
}