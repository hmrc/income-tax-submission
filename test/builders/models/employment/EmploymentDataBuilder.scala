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

package builders.models.employment

import builders.models.employment.DeductionsBuilder.aDeductions
import builders.models.employment.PayBuilder.aPay
import models.employment.EmploymentData

object EmploymentDataBuilder {

  val anEmploymentData: EmploymentData = EmploymentData(
    submittedOn = "2020-01-04T05:01:01Z",
    employmentSequenceNumber = Some("1002"),
    companyDirector = Some(false),
    closeCompany = Some(true),
    directorshipCeasedDate = Some("2020-02-12"),
    occPen = Some(false),
    disguisedRemuneration = Some(false),
    offPayrollWorker = Some(false),
    pay = Some(aPay),
    deductions = Some(aDeductions)
  )

  val anEmploymentDataOccPen: EmploymentData = EmploymentData(
    submittedOn = "2020-01-04T05:01:01Z",
    employmentSequenceNumber = Some("1002"),
    companyDirector = Some(false),
    closeCompany = Some(true),
    directorshipCeasedDate = Some("2020-02-12"),
    occPen = Some(true),
    disguisedRemuneration = Some(false),
    offPayrollWorker = Some(false),
    pay = Some(aPay),
    deductions = Some(aDeductions)
  )
}
