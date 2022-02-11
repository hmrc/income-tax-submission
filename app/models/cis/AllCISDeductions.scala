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

import play.api.libs.json.{Json, OFormat}
import utils.EncryptedValue

case class AllCISDeductions(customerCISDeductions: Option[CISSource],
                            contractorCISDeductions: Option[CISSource])

object AllCISDeductions {
  implicit val format: OFormat[AllCISDeductions] = Json.format[AllCISDeductions]
}

case class EncryptedAllCISDeductions(customerCISDeductions: Option[EncryptedCISSource],
                                     contractorCISDeductions: Option[EncryptedCISSource])

object EncryptedAllCISDeductions {
  implicit val format: OFormat[EncryptedAllCISDeductions] = Json.format[EncryptedAllCISDeductions]
}

case class CISSource(totalDeductionAmount: Option[BigDecimal],
                     totalCostOfMaterials: Option[BigDecimal],
                     totalGrossAmountPaid: Option[BigDecimal],
                     cisDeductions: Seq[CISDeductions])

object CISSource {
  implicit val format: OFormat[CISSource] = Json.format[CISSource]
}

case class EncryptedCISSource(totalDeductionAmount: Option[EncryptedValue],
                              totalCostOfMaterials: Option[EncryptedValue],
                              totalGrossAmountPaid: Option[EncryptedValue],
                              cisDeductions: Seq[EncryptedCISDeductions])

object EncryptedCISSource {
  implicit val format: OFormat[EncryptedCISSource] = Json.format[EncryptedCISSource]
}

case class CISDeductions(fromDate: String,
                         toDate: String,
                         contractorName: Option[String],
                         employerRef: String,
                         totalDeductionAmount: Option[BigDecimal],
                         totalCostOfMaterials: Option[BigDecimal],
                         totalGrossAmountPaid: Option[BigDecimal],
                         periodData: Seq[GetPeriodData]
                        )

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
                                  periodData: Seq[EncryptedGetPeriodData]
                                 )

object EncryptedCISDeductions {
  implicit val format: OFormat[EncryptedCISDeductions] = Json.format[EncryptedCISDeductions]
}

case class GetPeriodData(deductionFromDate: String,
                         deductionToDate: String,
                         deductionAmount: Option[BigDecimal],
                         costOfMaterials: Option[BigDecimal],
                         grossAmountPaid: Option[BigDecimal],
                         submissionDate: String,
                         submissionId: Option[String],
                         source: String)

object GetPeriodData {
  implicit val format: OFormat[GetPeriodData] = Json.format[GetPeriodData]
}

case class EncryptedGetPeriodData(deductionFromDate: EncryptedValue,
                                  deductionToDate: EncryptedValue,
                                  deductionAmount: Option[EncryptedValue],
                                  costOfMaterials: Option[EncryptedValue],
                                  grossAmountPaid: Option[EncryptedValue],
                                  submissionDate: EncryptedValue,
                                  submissionId: Option[EncryptedValue],
                                  source: EncryptedValue)

object EncryptedGetPeriodData {
  implicit val format: OFormat[EncryptedGetPeriodData] = Json.format[EncryptedGetPeriodData]
}
