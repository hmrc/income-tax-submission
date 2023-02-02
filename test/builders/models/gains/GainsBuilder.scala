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

package builders.models.gains

import builders.models.gains.CapitalRedemptionBuilder.aCapitalRedemption
import builders.models.gains.ForeignBuilder.aForeign
import builders.models.gains.LifeAnnuityBuilder.aLifeAnnuity
import builders.models.gains.LifeInsuranceBuilder.aLifeInsurance
import builders.models.gains.VoidedIsaBuilder.aVoidedIsa
import models.gains.InsurancePoliciesModel


object GainsBuilder {

  val anGains: InsurancePoliciesModel = InsurancePoliciesModel(
    submittedOn = "2023-01-04T05:01:01Z",
    lifeInsurance = Seq(aLifeInsurance),
    capitalRedemption = Some(Seq(aCapitalRedemption)),
    foreign = Some(Seq(aForeign)),
    lifeAnnuity = Some(Seq(aLifeAnnuity)),
    voidedIsa = Some(Seq(aVoidedIsa))
  )
}
