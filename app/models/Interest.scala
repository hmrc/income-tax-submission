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

package models

import play.api.libs.json.{Json, OFormat}
import utils.EncryptedValue

case class Interest(accountName: String,
                    incomeSourceId: String,
                    taxedUkInterest: Option[BigDecimal],
                    untaxedUkInterest: Option[BigDecimal])


object Interest {
  implicit val formats: OFormat[Interest] = Json.format[Interest]
}

case class EncryptedInterest(accountName: EncryptedValue,
                             incomeSourceId: EncryptedValue,
                             taxedUkInterest: Option[EncryptedValue],
                             untaxedUkInterest: Option[EncryptedValue])

object EncryptedInterest {
  implicit val formats: OFormat[EncryptedInterest] = Json.format[EncryptedInterest]
}