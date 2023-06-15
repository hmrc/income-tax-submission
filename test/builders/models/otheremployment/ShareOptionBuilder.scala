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

package builders.models.otheremployment

import models.otheremployment.ShareOption
import models.otheremployment.ShareOptionSchemePlanType.EMI

import java.time.LocalDate

object ShareOptionBuilder {

  val aShareOption: ShareOption = ShareOption(
    employerName = "Company Ltd",
    employerRef = Some("321/AB123"),
    schemePlanType = EMI,
    dateOfOptionGrant = LocalDate.parse("2019-11-20"),
    dateOfEvent = LocalDate.parse("2019-10-20"),
    optionNotExercisedButConsiderationReceived = Some(true),
    amountOfConsiderationReceived = BigDecimal(23445.78),
    noOfSharesAcquired = 1,
    classOfSharesAcquired = Some("FIRST"),
    exercisePrice = BigDecimal(456.56),
    amountPaidForOption = BigDecimal(3555.45),
    marketValueOfSharesOnExcise = BigDecimal(3323.45),
    profitOnOptionExercised = BigDecimal(4532.45),
    employersNicPaid = BigDecimal(234.78),
    taxableAmount = BigDecimal(35345.56)
  )
}
