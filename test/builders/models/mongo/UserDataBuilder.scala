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

package builders.models.mongo

import builders.models.DividendsBuilder.aDividends
import builders.models.InterestsBuilder.anInterest
import builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import builders.models.gifts.GiftAidBuilder.aGiftAid
import builders.models.pensions.PensionsBuilder.aPensions
import models.mongo.UserData

object UserDataBuilder {

  val aUserData: UserData = UserData(
    sessionId = "sessionId-1618a1e8-4979-41d8-a32e-5ffbe69fac81",
    mtdItId = "1234567890",
    nino = "AA123456A",
    taxYear = 2022,
    dividends = Some(aDividends),
    interest = Some(Seq(anInterest)),
    giftAid = Some(aGiftAid),
    employment = Some(anAllEmploymentData),
    pensions = Some(aPensions)
  )
}
