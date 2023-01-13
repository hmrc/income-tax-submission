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

package builders.models.gifts

import models.gifts.GiftAidPayments

object GiftAidPaymentsBuilder {

  val aGiftAidPayments: GiftAidPayments = GiftAidPayments(
    nonUkCharitiesCharityNames = Some(List("default-non-uk-charity")),
    currentYear = Some(100.0),
    oneOffCurrentYear = Some(200.0),
    currentYearTreatedAsPreviousYear = Some(300.0),
    nextYearTreatedAsCurrentYear = Some(400.0),
    nonUkCharities = Some(500.0)
  )
}
