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

package models.employment

import models.pensions.employmentPensions.{EmploymentPensionModel, EmploymentPensions}
import play.api.libs.json.{Json, OFormat}

case class AllEmploymentData(hmrcEmploymentData: Seq[HmrcEmploymentSource],
                             hmrcExpenses: Option[EmploymentExpenses],
                             customerEmploymentData: Seq[EmploymentSource],
                             customerExpenses: Option[EmploymentExpenses]) {

  def excludePensionIncome(): AllEmploymentData = this.copy(
    hmrcEmploymentData = hmrcEmploymentData.filterNot(_.hasOccupationalPension),
    customerEmploymentData = customerEmploymentData.filterNot(_.hasOccupationalPension)
  )

  def onlyPensionIncome(): AllEmploymentData = this.copy(
    hmrcEmploymentData = hmrcEmploymentData.filter(_.hasOccupationalPension),
    customerEmploymentData = customerEmploymentData.filter(_.hasOccupationalPension)
  )

  def buildEmploymentPensions(): EmploymentPensions = {

    val onlyOccPen = this.onlyPensionIncome()

    val hmrc: Seq[EmploymentPensionModel] = onlyOccPen.hmrcEmploymentData.map(
      x =>
        EmploymentPensionModel(
          employmentId = x.employmentId,
          pensionSchemeName = x.employerName,
          pensionSchemeRef = x.employerRef,
          pensionId = x.payrollId,
          startDate = x.startDate,
          endDate = x.cessationDate,
          amount = x.getLatestEmploymentFinancialData.flatMap(_.employmentData.flatMap(_.pay.flatMap(_.taxablePayToDate))),
          taxPaid = x.getLatestEmploymentFinancialData.flatMap(_.employmentData.flatMap(_.pay.flatMap(_.totalTaxToDate))),
          isCustomerEmploymentData = Some(false)
        )
    )

    val customer: Seq[EmploymentPensionModel] = onlyOccPen.customerEmploymentData.map(
      x =>
        EmploymentPensionModel(
          employmentId = x.employmentId,
          pensionSchemeName = x.employerName,
          pensionSchemeRef = x.employerRef,
          pensionId = x.payrollId,
          startDate = x.startDate,
          endDate = x.cessationDate,
          amount = x.employmentData.flatMap(_.pay.flatMap(_.taxablePayToDate)),
          taxPaid = x.employmentData.flatMap(_.pay.flatMap(_.totalTaxToDate)),
          isCustomerEmploymentData = Some(true)
        )
    )

    EmploymentPensions(hmrc ++ customer)
  }
}

object AllEmploymentData {
  implicit val format: OFormat[AllEmploymentData] = Json.format[AllEmploymentData]
}

case class EncryptedAllEmploymentData(hmrcEmploymentData: Seq[EncryptedHmrcEmploymentSource],
                                      hmrcExpenses: Option[EncryptedEmploymentExpenses],
                                      customerEmploymentData: Seq[EncryptedEmploymentSource],
                                      customerExpenses: Option[EncryptedEmploymentExpenses])

object EncryptedAllEmploymentData {
  implicit val format: OFormat[EncryptedAllEmploymentData] = Json.format[EncryptedAllEmploymentData]
}