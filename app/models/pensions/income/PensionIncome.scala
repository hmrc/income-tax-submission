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

package models.pensions.income

import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.crypto.EncryptedValue


case class ForeignPension (
                            countryCode: String,
                            taxableAmount: BigDecimal,
                            amountBeforeTax: Option[BigDecimal],
                            taxTakenOff: Option[BigDecimal],
                            specialWithholdingTax: Option[BigDecimal],
                            foreignTaxCreditRelief: Option[Boolean]
                          )

object ForeignPension {
  implicit val format: OFormat[ForeignPension] = Json.format[ForeignPension]
}

case class EncryptedForeignPension (
                            countryCode: EncryptedValue,
                            taxableAmount: EncryptedValue,
                            amountBeforeTax: Option[EncryptedValue],
                            taxTakenOff: Option[EncryptedValue],
                            specialWithholdingTax: Option[EncryptedValue],
                            foreignTaxCreditRelief: Option[EncryptedValue]
                          )

object EncryptedForeignPension {
  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]

  implicit val format: Format[EncryptedForeignPension] = Json.format[EncryptedForeignPension]
}

case class OverseasPensionContribution (
                                         customerReference: Option[String],
                                         exemptEmployersPensionContribs: BigDecimal,
                                         migrantMemReliefQopsRefNo: Option[String],
                                         dblTaxationRelief: Option[BigDecimal],
                                         dblTaxationCountry: Option[String],
                                         dblTaxationArticle: Option[String],
                                         dblTaxationTreaty: Option[String],
                                         sf74Reference: Option[String]
                                       )

object OverseasPensionContribution {
  implicit val format: OFormat[OverseasPensionContribution] = Json.format[OverseasPensionContribution]
}


case class EncryptedOverseasPensionContribution (
                                         customerReference: Option[EncryptedValue],
                                         exemptEmployersPensionContribs: EncryptedValue,
                                         migrantMemReliefQopsRefNo: Option[EncryptedValue],
                                         dblTaxationRelief: Option[EncryptedValue],
                                         dblTaxationCountry: Option[EncryptedValue],
                                         dblTaxationArticle: Option[EncryptedValue],
                                         dblTaxationTreaty: Option[EncryptedValue],
                                         sf74Reference: Option[EncryptedValue]
                                       )

object EncryptedOverseasPensionContribution {
  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]

  implicit val format: Format[EncryptedOverseasPensionContribution] = Json.format[EncryptedOverseasPensionContribution]
}

case class PensionIncomeModel (
                                   submittedOn: String,
                                   deletedOn: Option[String],
                                   foreignPension: Seq[ForeignPension],
                                   overseasPensionContribution: Seq[OverseasPensionContribution]
                                 )
object PensionIncomeModel {
  implicit val format: OFormat[PensionIncomeModel] = Json.format[PensionIncomeModel]
}

case class EncryptedPensionIncomeModel (
                                   submittedOn: EncryptedValue,
                                   deletedOn: Option[EncryptedValue],
                                   foreignPension: Seq[EncryptedForeignPension],
                                   overseasPensionContribution: Seq[EncryptedOverseasPensionContribution]
                                 )
object EncryptedPensionIncomeModel {
  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]

  implicit val format: Format[EncryptedPensionIncomeModel] = Json.format[EncryptedPensionIncomeModel]
}
