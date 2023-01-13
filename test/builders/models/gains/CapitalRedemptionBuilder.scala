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

import models.CapitalRedemptionModel

object CapitalRedemptionBuilder {

  val aCapitalRedemption: CapitalRedemptionModel = CapitalRedemptionModel(
    customerReference = Some("RefNo13254688"),
    event = Some("capital"),
    gainAmount = 123.45,
    taxPaid = Some(true),
    yearsHeld = Some(3),
    yearsHeldSinceLastGain = Some(2),
    deficiencyRelief = Some(0)
  )
}
