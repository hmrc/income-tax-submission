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

import builders.models.otheremployment.DisabilityBuilder.aDisability
import builders.models.otheremployment.ForeignServiceBuilder.aForeignService
import builders.models.otheremployment.LumpSumBuilder.aLumpSum
import builders.models.otheremployment.ShareAwardedOrReceivedBuilder.aSharesAwardedOrReceived
import builders.models.otheremployment.ShareOptionBuilder.aShareOption
import models.otheremployment.OtherEmploymentIncome
import utils.TaxYearUtils.taxYearEOY

import java.time.{LocalDateTime, ZoneOffset}

object OtherEmploymentIncomeBuilder {

  val month = 1
  val dayOfMonth = 4
  val hour = 5
  val minute = 1
  val second = 1

  val instant = LocalDateTime.of(taxYearEOY, month, dayOfMonth, hour, minute, second).toInstant(ZoneOffset.UTC)

  val anOtherEmploymentIncome: OtherEmploymentIncome = OtherEmploymentIncome(
    submittedOn = Some(instant),
    shareOptions = Some(Set(aShareOption)),
    sharesAwardedOrReceived = Some(Set(aSharesAwardedOrReceived)),
    lumpSums = Some(Set(aLumpSum)),
    disability = Some(aDisability),
    foreignService = Some(aForeignService)
  )

}
