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

package models.otheremployment

import models.otheremployment.ShareOptionSchemePlanType.SchemePlanType
import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.crypto.EncryptedValue

import java.time.LocalDate

case class ShareOption(employerName: String,
                       employerRef: Option[String] = None,
                       schemePlanType: SchemePlanType,
                       dateOfOptionGrant: LocalDate,
                       dateOfEvent: LocalDate,
                       optionNotExercisedButConsiderationReceived: Option[Boolean] = None,
                       amountOfConsiderationReceived: BigDecimal,
                       noOfSharesAcquired: Int,
                       classOfSharesAcquired: Option[String] = None,
                       exercisePrice: BigDecimal,
                       amountPaidForOption: BigDecimal,
                       marketValueOfSharesOnExcise: BigDecimal,
                       profitOnOptionExercised: BigDecimal,
                       employersNicPaid: BigDecimal,
                       taxableAmount: BigDecimal)


object ShareOption {
  implicit val format: OFormat[ShareOption] = Json.format[ShareOption]
}

case class EncryptedShareOption(employerName: EncryptedValue,
                                employerRef: Option[EncryptedValue] = None,
                                schemePlanType: EncryptedValue,
                                dateOfOptionGrant: EncryptedValue,
                                dateOfEvent: EncryptedValue,
                                optionNotExercisedButConsiderationReceived: Option[EncryptedValue] = None,
                                amountOfConsiderationReceived: EncryptedValue,
                                noOfSharesAcquired: EncryptedValue,
                                classOfSharesAcquired: Option[EncryptedValue] = None,
                                exercisePrice: EncryptedValue,
                                amountPaidForOption: EncryptedValue,
                                marketValueOfSharesOnExcise: EncryptedValue,
                                profitOnOptionExercised: EncryptedValue,
                                employersNicPaid: EncryptedValue,
                                taxableAmount: EncryptedValue)

object EncryptedShareOption {
  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]

  implicit val format: Format[EncryptedShareOption] = Json.format[EncryptedShareOption]
}