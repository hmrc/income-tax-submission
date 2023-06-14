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

package models

import models.employment.ShareOptionSchemePlanType.SchemePlanType
import play.api.libs.json.{Format, Json, OFormat}

import java.time.LocalDate

package object employment {

  case class ShareOption(employerName: String,
                         employerRef: Option[String] = None,
                         schemePlanType: SchemePlanType,
                         dateOfOptionGrant: LocalDate,
                         dateOfEvent: LocalDate,
                         optionNotExercisedButConsiderationReceived: Option[Boolean] = None,
                         amountOfConsiderationReceived: BigDecimal,
                         noOfSharesAcquired: Int,
                         classOfSharesAcquired: Option[String] = None,
                         exercisePrice: BigDecimal,
                         amountPaidForOption: BigDecimal,
                         marketValueOfSharesOnExcise: BigDecimal,
                         profitOnOptionExercised: BigDecimal,
                         employersNicPaid: BigDecimal,
                         taxableAmount: BigDecimal)


  object ShareOption {
    implicit val format: OFormat[ShareOption] = Json.format[ShareOption]
  }

  case class SharesAwardedOrReceived(employerName: String,
                                     employerRef: Option[String] = None,
                                     schemePlanType: SchemePlanType,
                                     dateSharesCeasedToBeSubjectToPlan: LocalDate,
                                     noOfShareSecuritiesAwarded: Int,
                                     classOfShareAwarded: String,
                                     dateSharesAwarded: LocalDate,
                                     sharesSubjectToRestrictions: Boolean,
                                     electionEnteredIgnoreRestrictions: Boolean,
                                     actualMarketValueOfSharesOnAward: BigDecimal,
                                     unrestrictedMarketValueOfSharesOnAward: BigDecimal,
                                     amountPaidForSharesOnAward: BigDecimal,
                                     marketValueAfterRestrictionsLifted: BigDecimal,
                                     taxableAmount: BigDecimal)

  object SharesAwardedOrReceived {
    implicit val format: OFormat[SharesAwardedOrReceived] = Json.format[SharesAwardedOrReceived]
  }

  object ShareOptionSchemePlanType extends Enumeration {
    type SchemePlanType = Value

    val EMI = Value("EMI")
    val CSOP = Value("CSOP")
    val SAYE = Value("SAYE")
    val Other = Value("Other")

    implicit val format: Format[SchemePlanType] = Json.formatEnum(this)
  }

  case class LumpSum(employerName: String,
                     employerRef: String,
                     taxableLumpSumsAndCertainIncome: Option[TaxableLumpSumsAndCertainIncome],
                     benefitFromEmployerFinancedRetirementScheme: Option[BenefitFromEmployerFinancedRetirementScheme],
                     redundancyCompensationPaymentsOverExemption: Option[RedundancyCompensationPaymentsOverExemption],
                     redundancyCompensationPaymentsUnderExemption: Option[RedundancyCompensationPaymentsUnderExemption])

  object LumpSum {
    implicit val format: OFormat[LumpSum] = Json.format[LumpSum]
  }


  case class TaxableLumpSumsAndCertainIncome(amount: BigDecimal,
                                             taxPaid: Option[BigDecimal] = None,
                                             taxTakenOffInEmployment: Option[Boolean] = None)

  object TaxableLumpSumsAndCertainIncome {
    implicit val format: OFormat[TaxableLumpSumsAndCertainIncome] = Json.format[TaxableLumpSumsAndCertainIncome]
  }

  case class BenefitFromEmployerFinancedRetirementScheme(amount: BigDecimal,
                                                         exemptAmount: Option[BigDecimal] = None,
                                                         taxPaid: Option[BigDecimal] = None,
                                                         taxTakenOffInEmployment: Option[Boolean] = None)

  object BenefitFromEmployerFinancedRetirementScheme {
    implicit val format: OFormat[BenefitFromEmployerFinancedRetirementScheme] = Json.format[BenefitFromEmployerFinancedRetirementScheme]
  }


  case class RedundancyCompensationPaymentsOverExemption(amount: BigDecimal,
                                                         taxPaid: Option[BigDecimal] = None,
                                                         taxTakenOffInEmployment: Option[Boolean] = None)

  object RedundancyCompensationPaymentsOverExemption {
    implicit val format: OFormat[RedundancyCompensationPaymentsOverExemption] = Json.format[RedundancyCompensationPaymentsOverExemption]
  }

  case class RedundancyCompensationPaymentsUnderExemption(amount: BigDecimal)

  object RedundancyCompensationPaymentsUnderExemption {
    implicit val format: OFormat[RedundancyCompensationPaymentsUnderExemption] = Json.format[RedundancyCompensationPaymentsUnderExemption]
  }

  case class Disability(customerReference: Option[String] = None,
                        amountDeducted: BigDecimal)

  object Disability {
    implicit val format: OFormat[Disability] = Json.format[Disability]
  }

  case class ForeignService(customerReference: Option[String] = None,
                            amountDeducted: BigDecimal)

  object ForeignService {
    implicit val format: OFormat[ForeignService] = Json.format[ForeignService]
  }

}
