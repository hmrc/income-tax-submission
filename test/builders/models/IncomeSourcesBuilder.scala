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

package builders.models

import builders.models.DividendsBuilder.aDividends
import builders.models.InterestBuilder.anInterest
import builders.models.cis.AllCISDeductionsBuilder.anAllCISDeductions
import builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import builders.models.gains.GainsBuilder.anGains
import builders.models.gifts.GiftAidBuilder.aGiftAid
import builders.models.pensions.PensionsBuilder.aPensionsAlternative
import builders.models.statebenefits.AllStateBenefitsDataBuilder.anAllStateBenefitsData
import models.IncomeSources

object IncomeSourcesBuilder {

  val anIncomeSources: IncomeSources = IncomeSources(
    dividends = Some(aDividends),
    interest = Some(Seq(anInterest)),
    giftAid = Some(aGiftAid),
    employment = Some(anAllEmploymentData),
    pensions = Some(aPensionsAlternative),
    cis = Some(anAllCISDeductions),
    stateBenefits = Some(anAllStateBenefitsData),
    gains = Some(anGains)
  )
}
