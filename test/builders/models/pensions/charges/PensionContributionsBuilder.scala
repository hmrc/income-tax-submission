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

package builders.models.pensions.charges

import models.pensions.charges._

object PensionContributionsBuilder {

  val aPensionContributions: PensionContributions = PensionContributions(
    pensionSchemeTaxReference = Seq("00123456RA", "00123456RB"),
    inExcessOfTheAnnualAllowance = 100.0,
    annualAllowanceTaxPaid = 200.0,
    isAnnualAllowanceReduced = Some(false),
    taperedAnnualAllowance = Some(false),
    moneyPurchasedAllowance = Some(false)
  )
}
