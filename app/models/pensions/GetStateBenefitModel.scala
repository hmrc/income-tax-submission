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

case class StateBenefit(benefitId: String,
                        startDate: String,
                        dateIgnored: Option[String],
                        submittedOn: Option[String],
                        endDate: Option[String],
                        amount: Option[BigDecimal],
                        taxPaid: Option[BigDecimal]
                       )

object StateBenefit {
  implicit val format: OFormat[StateBenefit] = Json.format[StateBenefit]
}
case class EncryptedStateBenefit(benefitId: EncryptedValue,
                                 startDate: EncryptedValue,
                                 dateIgnored: Option[EncryptedValue],
                                 submittedOn: Option[EncryptedValue],
                                 endDate: Option[EncryptedValue],
                                 amount: Option[EncryptedValue],
                                 taxPaid: Option[EncryptedValue]
                       )

object EncryptedStateBenefit {
  implicit val format: OFormat[EncryptedStateBenefit] = Json.format[EncryptedStateBenefit]
}

case class CustomerStateBenefit(benefitId: EncryptedValue,
                                startDate: EncryptedValue,
                                submittedOn: Option[EncryptedValue],
                                endDate: Option[EncryptedValue],
                                amount: Option[EncryptedValue],
                                taxPaid: Option[EncryptedValue]
                               )

object CustomerStateBenefit {
  implicit val format: OFormat[CustomerStateBenefit] = Json.format[CustomerStateBenefit]
}

case class EncryptedCustomerStateBenefit(benefitId: EncryptedValue,
                                startDate: EncryptedValue,
                                submittedOn: Option[EncryptedValue],
                                endDate: Option[EncryptedValue],
                                amount: Option[EncryptedValue],
                                taxPaid: Option[EncryptedValue]
                               )

object EncryptedCustomerStateBenefit {
  implicit val format: OFormat[EncryptedCustomerStateBenefit] = Json.format[EncryptedCustomerStateBenefit]
}

case class StateBenefits(
                          incapacityBenefit: Option[Seq[StateBenefit]],
                          statePension: Option[StateBenefit],
                          statePensionLumpSum: Option[StateBenefit],
                          employmentSupportAllowance: Option[Seq[StateBenefit]],
                          jobSeekersAllowance: Option[Seq[StateBenefit]],
                          bereavementAllowance: Option[StateBenefit],
                          otherStateBenefits: Option[StateBenefit]
                        )

object StateBenefits {
  implicit val format: OFormat[StateBenefits] = Json.format[StateBenefits]
}

case class EncryptedStateBenefits(
                          incapacityBenefit: Option[Seq[EncryptedStateBenefit]],
                          statePension: Option[EncryptedStateBenefit],
                          statePensionLumpSum: Option[EncryptedStateBenefit],
                          employmentSupportAllowance: Option[Seq[EncryptedStateBenefit]],
                          jobSeekersAllowance: Option[Seq[EncryptedStateBenefit]],
                          bereavementAllowance: Option[EncryptedStateBenefit],
                          otherStateBenefits: Option[EncryptedStateBenefit]
                        )

object EncryptedStateBenefits {
  implicit val format: OFormat[EncryptedStateBenefits] = Json.format[EncryptedStateBenefits]
}

case class CustomerStateBenefits(
                                  incapacityBenefit: Option[Seq[CustomerStateBenefit]],
                                  statePension: Option[CustomerStateBenefit],
                                  statePensionLumpSum: Option[CustomerStateBenefit],
                                  employmentSupportAllowance: Option[Seq[CustomerStateBenefit]],
                                  jobSeekersAllowance: Option[Seq[CustomerStateBenefit]],
                                  bereavementAllowance: Option[CustomerStateBenefit],
                                  otherStateBenefits: Option[CustomerStateBenefit]

                                )

object CustomerStateBenefits {
  implicit val format: OFormat[CustomerStateBenefits] = Json.format[CustomerStateBenefits]
}

case class EncryptedCustomerStateBenefits(
                                  incapacityBenefit: Option[Seq[EncryptedCustomerStateBenefit]],
                                  statePension: Option[EncryptedCustomerStateBenefit],
                                  statePensionLumpSum: Option[EncryptedCustomerStateBenefit],
                                  employmentSupportAllowance: Option[Seq[EncryptedCustomerStateBenefit]],
                                  jobSeekersAllowance: Option[Seq[EncryptedCustomerStateBenefit]],
                                  bereavementAllowance: Option[EncryptedCustomerStateBenefit],
                                  otherStateBenefits: Option[EncryptedCustomerStateBenefit]

                                )

object EncryptedCustomerStateBenefits {
  implicit val format: OFormat[EncryptedCustomerStateBenefits] = Json.format[EncryptedCustomerStateBenefits]
}

case class StateBenefitsModel(stateBenefits: Option[StateBenefits],
                              customerAddedStateBenefits: Option[CustomerStateBenefits])

object StateBenefitsModel {
  implicit val format: OFormat[StateBenefitsModel] = Json.format[StateBenefitsModel]
}

case class EncryptedStateBenefitsModel(stateBenefits: Option[EncryptedStateBenefits],
                              customerAddedStateBenefits: Option[EncryptedCustomerStateBenefits])

object EncryptedStateBenefitsModel {
  implicit val format: OFormat[EncryptedStateBenefitsModel] = Json.format[EncryptedStateBenefitsModel]
}
