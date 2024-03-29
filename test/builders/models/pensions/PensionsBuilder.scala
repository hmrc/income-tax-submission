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

package builders.models.pensions

import builders.models.pensions.charges.PensionChargesBuilder.aPensionCharges
import builders.models.pensions.reliefs.PensionReliefsBuilder.aPensionReliefs
import builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentDataWithOccPen
import builders.models.pensions.income.PensionIncome.aPensionIncome
import builders.models.statebenefits.AllStateBenefitsDataBuilder.anAllStateBenefitsData
import models.pensions.Pensions
import models.pensions.employmentPensions.EmploymentPensions

object PensionsBuilder {

  val aPensions: Pensions = Pensions(
    pensionReliefs = Some(aPensionReliefs),
    pensionCharges = Some(aPensionCharges),
    stateBenefits = Some(anAllStateBenefitsData),
    employmentPensions = None,
    pensionIncome = Some(aPensionIncome)
  )

  val aPensionsAlternative: Pensions =
    aPensions.copy(employmentPensions = Some(EmploymentPensions(Seq())))

  val aPensionsWithEmployments: Pensions = Pensions(
    pensionReliefs = Some(aPensionReliefs),
    pensionCharges = Some(aPensionCharges),
    stateBenefits = Some(anAllStateBenefitsData),
    employmentPensions = Some(anAllEmploymentDataWithOccPen.buildEmploymentPensions()),
    pensionIncome = Some(aPensionIncome)
  )
}
