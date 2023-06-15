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

import models.otheremployment.ShareAwardedShareSchemePlanType.SIP
import models.otheremployment.SharesAwardedOrReceived

import java.time.LocalDate

object ShareAwardedOrReceivedBuilder {

  val aSharesAwardedOrReceived: SharesAwardedOrReceived = SharesAwardedOrReceived(
    employerName = "ABC Ltd",
    employerRef = Some("321/AB156"),
    schemePlanType = SIP,
    dateSharesCeasedToBeSubjectToPlan = LocalDate.parse("2019-10-20"),
    noOfShareSecuritiesAwarded = 2,
    classOfShareAwarded = "FIRST",
    dateSharesAwarded = LocalDate.parse("2019-09-20"),
    sharesSubjectToRestrictions = true,
    electionEnteredIgnoreRestrictions = true,
    actualMarketValueOfSharesOnAward = BigDecimal(35345.67),
    unrestrictedMarketValueOfSharesOnAward = BigDecimal(5643.34),
    amountPaidForSharesOnAward = BigDecimal(4656.45),
    marketValueAfterRestrictionsLifted = BigDecimal(4654.34),
    taxableAmount = BigDecimal(45646.56),
  )
}
