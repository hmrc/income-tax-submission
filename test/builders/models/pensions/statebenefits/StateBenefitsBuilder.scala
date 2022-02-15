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

package builders.models.pensions.statebenefits

import builders.models.pensions.statebenefits.StateBenefitBuilder.aStateBenefit
import models.pensions.statebenefits.StateBenefits

object StateBenefitsBuilder {

  val aStateBenefits: StateBenefits = StateBenefits(
    incapacityBenefit = Some(Seq(aStateBenefit)),
    statePension = Some(aStateBenefit),
    statePensionLumpSum = Some(aStateBenefit),
    employmentSupportAllowance = Option(Seq(aStateBenefit)),
    jobSeekersAllowance = Option(Seq(aStateBenefit)),
    bereavementAllowance = Option(aStateBenefit),
    otherStateBenefits = Option(aStateBenefit)
  )
}
