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

package builders.models.pensions.charges

import builders.models.pensions.charges.OverseasPensionContributionsBuilder.anOverseasPensionContributions
import builders.models.pensions.charges.PensionContributionsBuilder.aPensionContributions
import builders.models.pensions.charges.PensionSavingsTaxChargesBuilder.aPensionSavingsTaxCharges
import builders.models.pensions.charges.PensionSchemeOverseasTransfersBuilder.aPensionSchemeOverseasTransfers
import builders.models.pensions.charges.PensionSchemeUnauthorisedPaymentsBuilder.aPensionSchemeUnauthorisedPayments
import models.pensions.charges._

object PensionChargesBuilder {

  val aPensionCharges: PensionCharges = PensionCharges(
    submittedOn = "2020-07-27T17:00:19Z",
    pensionSavingsTaxCharges = Some(aPensionSavingsTaxCharges),
    pensionSchemeOverseasTransfers = Some(aPensionSchemeOverseasTransfers),
    pensionSchemeUnauthorisedPayments = Some(aPensionSchemeUnauthorisedPayments),
    pensionContributions = Some(aPensionContributions),
    overseasPensionContributions = Some(anOverseasPensionContributions)
  )
}
