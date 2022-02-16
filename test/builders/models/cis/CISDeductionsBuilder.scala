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

package builders.models.cis

import builders.models.cis.PeriodDataBuilder.aPeriodData
import models.cis.CISDeductions

object CISDeductionsBuilder {

  val aCISDeductions: CISDeductions = CISDeductions(
    fromDate = "2019-04-06",
    toDate = "2019-05-05",
    contractorName = Some("default-contractor"),
    employerRef = "default-employer-ref",
    totalDeductionAmount = Some(100.0),
    totalCostOfMaterials = Some(200.0),
    totalGrossAmountPaid = Some(300.0),
    periodData = Seq(aPeriodData)
  )
}
