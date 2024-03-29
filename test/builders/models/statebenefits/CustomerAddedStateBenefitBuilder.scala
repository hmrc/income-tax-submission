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

package builders.models.statebenefits

import models.statebenefits.CustomerAddedStateBenefit
import utils.TaxYearUtils.taxYearEOY

import java.time.{Instant, LocalDate}
import java.util.UUID

object CustomerAddedStateBenefitBuilder {

  val aCustomerAddedStateBenefit: CustomerAddedStateBenefit = CustomerAddedStateBenefit(
    benefitId = UUID.fromString("a1e8057e-fbbc-47a8-a8b4-78d9f015c941"),
    startDate = LocalDate.parse(s"${taxYearEOY - 1}-04-23"),
    endDate = Some(LocalDate.parse(s"$taxYearEOY-08-13")),
    submittedOn = Some(Instant.parse(s"$taxYearEOY-03-13T19:23:00Z")),
    amount = Some(100.00),
    taxPaid = Some(200.00)
  )
}
