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

import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.crypto.EncryptedValue
import utils.AesGcmAdCrypto
import utils.CypherSyntax.{DecryptableOps, EncryptableOps}

import java.time.LocalDate

case class ShareOption(employerName: String,
                       employerRef: Option[String] = None,
                       schemePlanType: ShareOptionSchemePlanType,
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
                       taxableAmount: BigDecimal){

  def encrypted()(implicit secureGCMCipher: AesGcmAdCrypto, associatedText: String): EncryptedShareOption = EncryptedShareOption(
    employerName = employerName.encrypted,
    employerRef = employerRef.map(_.encrypted),
    schemePlanType = schemePlanType.toString.encrypted,
    dateOfOptionGrant = dateOfOptionGrant.encrypted,
    dateOfEvent = dateOfEvent.encrypted,
    optionNotExercisedButConsiderationReceived = optionNotExercisedButConsiderationReceived.map(_.encrypted),
    amountOfConsiderationReceived = amountOfConsiderationReceived.encrypted,
    noOfSharesAcquired = noOfSharesAcquired.encrypted,
    classOfSharesAcquired = classOfSharesAcquired.map(_.encrypted),
    exercisePrice = exercisePrice.encrypted,
    amountPaidForOption = amountPaidForOption.encrypted,
    marketValueOfSharesOnExcise = marketValueOfSharesOnExcise.encrypted,
    profitOnOptionExercised = profitOnOptionExercised.encrypted,
    employersNicPaid = employersNicPaid.encrypted,
    taxableAmount = taxableAmount.encrypted
  )
}


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
                                taxableAmount: EncryptedValue) {

  def decrypted()(implicit secureGCMCipher: AesGcmAdCrypto, associatedText: String): ShareOption = ShareOption(
    employerName = employerName.decrypted[String],
    employerRef = employerRef.map(_.decrypted[String]),
    schemePlanType = ShareOptionSchemePlanType.fromString(schemePlanType.decrypted[String]).get,
    dateOfOptionGrant = dateOfOptionGrant.decrypted[LocalDate],
    dateOfEvent = dateOfEvent.decrypted[LocalDate],
    optionNotExercisedButConsiderationReceived = optionNotExercisedButConsiderationReceived.map(_.decrypted[Boolean]),
    amountOfConsiderationReceived = amountOfConsiderationReceived.decrypted[BigDecimal],
    noOfSharesAcquired = noOfSharesAcquired.decrypted[Int],
    classOfSharesAcquired = classOfSharesAcquired.map(_.decrypted[String]),
    exercisePrice = exercisePrice.decrypted[BigDecimal],
    amountPaidForOption = amountPaidForOption.decrypted[BigDecimal],
    marketValueOfSharesOnExcise = marketValueOfSharesOnExcise.decrypted[BigDecimal],
    profitOnOptionExercised = profitOnOptionExercised.decrypted[BigDecimal],
    employersNicPaid = employersNicPaid.decrypted[BigDecimal],
    taxableAmount = taxableAmount.decrypted[BigDecimal]
  )
}

object EncryptedShareOption {
  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]

  implicit val format: Format[EncryptedShareOption] = Json.format[EncryptedShareOption]
}
