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

package models.pensions

import play.api.libs.json.{Json, OFormat}
import utils.EncryptedValue

case class Charge(amount: BigDecimal, foreignTaxPaid: BigDecimal)

object Charge {
  implicit val format: OFormat[Charge] = Json.format[Charge]
}

case class EncryptedCharge(amount: EncryptedValue, foreignTaxPaid: EncryptedValue)

object EncryptedCharge {
  implicit val format: OFormat[EncryptedCharge] = Json.format[EncryptedCharge]
}

case class PensionSchemeOverseasTransfers(overseasSchemeProvider: Seq[OverseasSchemeProvider],
                                          transferCharge: BigDecimal,
                                          transferChargeTaxPaid: BigDecimal)

object PensionSchemeOverseasTransfers {
  implicit val format: OFormat[PensionSchemeOverseasTransfers] = Json.format[PensionSchemeOverseasTransfers]
}

case class EncryptedPensionSchemeOverseasTransfers(overseasSchemeProvider: Seq[EncryptedOverseasSchemeProvider],
                                          transferCharge: EncryptedValue,
                                          transferChargeTaxPaid: EncryptedValue)

object EncryptedPensionSchemeOverseasTransfers {
  implicit val format: OFormat[EncryptedPensionSchemeOverseasTransfers] = Json.format[EncryptedPensionSchemeOverseasTransfers]
}

case class PensionContributions(pensionSchemeTaxReference: Seq[String],
                                inExcessOfTheAnnualAllowance: BigDecimal,
                                annualAllowanceTaxPaid: BigDecimal)

object PensionContributions {
  implicit val format: OFormat[PensionContributions] = Json.format[PensionContributions]
}

case class EncryptedPensionContributions(pensionSchemeTaxReference: Seq[EncryptedValue],
                                inExcessOfTheAnnualAllowance: EncryptedValue,
                                annualAllowanceTaxPaid: EncryptedValue)

object EncryptedPensionContributions {
  implicit val format: OFormat[EncryptedPensionContributions] = Json.format[EncryptedPensionContributions]
}

case class OverseasSchemeProvider(providerName: String,
                                  providerAddress: String,
                                  providerCountryCode: String,
                                  qualifyingRecognisedOverseasPensionScheme: Option[Seq[String]],
                                  pensionSchemeTaxReference: Option[Seq[String]]
                                 )

object OverseasSchemeProvider {
  implicit val format: OFormat[OverseasSchemeProvider] = Json.format[OverseasSchemeProvider]
}

case class EncryptedOverseasSchemeProvider(providerName: EncryptedValue,
                                  providerAddress: EncryptedValue,
                                  providerCountryCode: EncryptedValue,
                                  qualifyingRecognisedOverseasPensionScheme: Option[Seq[EncryptedValue]],
                                  pensionSchemeTaxReference: Option[Seq[EncryptedValue]]
                                 )

object EncryptedOverseasSchemeProvider {
  implicit val format: OFormat[EncryptedOverseasSchemeProvider] = Json.format[EncryptedOverseasSchemeProvider]
}

case class LifetimeAllowance(amount: BigDecimal, taxPaid: BigDecimal)

object LifetimeAllowance {
  implicit val format: OFormat[LifetimeAllowance] = Json.format[LifetimeAllowance]
}

case class EncryptedLifetimeAllowance(amount: EncryptedValue, taxPaid: EncryptedValue)

object EncryptedLifetimeAllowance {
  implicit val format: OFormat[EncryptedLifetimeAllowance] = Json.format[EncryptedLifetimeAllowance]
}

case class OverseasPensionContributions(overseasSchemeProvider: Seq[OverseasSchemeProvider],
                                        shortServiceRefund: BigDecimal,
                                        shortServiceRefundTaxPaid: BigDecimal)

object OverseasPensionContributions {
  implicit val format: OFormat[OverseasPensionContributions] = Json.format[OverseasPensionContributions]
}

case class EncryptedOverseasPensionContributions(overseasSchemeProvider: Seq[EncryptedOverseasSchemeProvider],
                                        shortServiceRefund: EncryptedValue,
                                        shortServiceRefundTaxPaid: EncryptedValue)

object EncryptedOverseasPensionContributions {
  implicit val format: OFormat[EncryptedOverseasPensionContributions] = Json.format[EncryptedOverseasPensionContributions]
}

case class PensionSavingsTaxCharges(pensionSchemeTaxReference: Seq[String],
                                    lumpSumBenefitTakenInExcessOfLifetimeAllowance: Option[LifetimeAllowance],
                                    benefitInExcessOfLifetimeAllowance: Option[LifetimeAllowance],
                                    isAnnualAllowanceReduced: Boolean,
                                    taperedAnnualAllowance: Option[Boolean],
                                    moneyPurchasedAllowance: Option[Boolean])
object PensionSavingsTaxCharges {
  implicit val format: OFormat[PensionSavingsTaxCharges] = Json.format[PensionSavingsTaxCharges]
}

case class EncryptedPensionSavingsTaxCharges(pensionSchemeTaxReference: Seq[EncryptedValue],
                                    lumpSumBenefitTakenInExcessOfLifetimeAllowance: Option[EncryptedLifetimeAllowance],
                                    benefitInExcessOfLifetimeAllowance: Option[EncryptedLifetimeAllowance],
                                    isAnnualAllowanceReduced: EncryptedValue,
                                    taperedAnnualAllowance: Option[EncryptedValue],
                                    moneyPurchasedAllowance: Option[EncryptedValue])
object EncryptedPensionSavingsTaxCharges {
  implicit val format: OFormat[EncryptedPensionSavingsTaxCharges] = Json.format[EncryptedPensionSavingsTaxCharges]
}

case class PensionSchemeUnauthorisedPayments(pensionSchemeTaxReference: Seq[String],
                                             surcharge: Option[Charge],
                                             noSurcharge: Option[Charge])

object PensionSchemeUnauthorisedPayments {
  implicit val format: OFormat[PensionSchemeUnauthorisedPayments] = Json.format[PensionSchemeUnauthorisedPayments]
}

case class EncryptedPensionSchemeUnauthorisedPayments(pensionSchemeTaxReference: Seq[EncryptedValue],
                                             surcharge: Option[EncryptedCharge],
                                             noSurcharge: Option[EncryptedCharge])

object EncryptedPensionSchemeUnauthorisedPayments {
  implicit val format: OFormat[EncryptedPensionSchemeUnauthorisedPayments] = Json.format[EncryptedPensionSchemeUnauthorisedPayments]
}

case class PensionCharges(
                           submittedOn: String,
                           pensionSavingsTaxCharges: Option[PensionSavingsTaxCharges],
                           pensionSchemeOverseasTransfers: Option[PensionSchemeOverseasTransfers],
                           pensionSchemeUnauthorisedPayments: Option[PensionSchemeUnauthorisedPayments],
                           pensionContributions: Option[PensionContributions],
                           overseasPensionContributions: Option[OverseasPensionContributions]) {

}

object PensionCharges {
  implicit val format: OFormat[PensionCharges] = Json.format[PensionCharges]
}

case class EncryptedPensionCharges(
                           submittedOn: EncryptedValue,
                           pensionSavingsTaxCharges: Option[EncryptedPensionSavingsTaxCharges],
                           pensionSchemeOverseasTransfers: Option[EncryptedPensionSchemeOverseasTransfers],
                           pensionSchemeUnauthorisedPayments: Option[EncryptedPensionSchemeUnauthorisedPayments],
                           pensionContributions: Option[EncryptedPensionContributions],
                           overseasPensionContributions: Option[EncryptedOverseasPensionContributions]) {

}

object EncryptedPensionCharges {
  implicit val format: OFormat[EncryptedPensionCharges] = Json.format[EncryptedPensionCharges]
}
