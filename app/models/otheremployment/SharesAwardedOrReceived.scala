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

case class SharesAwardedOrReceived(employerName: String,
                                   employerRef: Option[String] = None,
                                   schemePlanType: ShareAwardedShareSchemePlanType,
                                   dateSharesCeasedToBeSubjectToPlan: LocalDate,
                                   noOfShareSecuritiesAwarded: Int,
                                   classOfShareAwarded: String,
                                   dateSharesAwarded: LocalDate,
                                   sharesSubjectToRestrictions: Boolean,
                                   electionEnteredIgnoreRestrictions: Boolean,
                                   actualMarketValueOfSharesOnAward: BigDecimal,
                                   unrestrictedMarketValueOfSharesOnAward: BigDecimal,
                                   amountPaidForSharesOnAward: BigDecimal,
                                   marketValueAfterRestrictionsLifted: BigDecimal,
                                   taxableAmount: BigDecimal){

  def encrypted()(implicit secureGCMCipher: AesGcmAdCrypto, associatedText: String): EncryptedSharesAwardedOrReceived = EncryptedSharesAwardedOrReceived(
    employerName = employerName.encrypted,
    employerRef = employerRef.map(_.encrypted),
    schemePlanType = schemePlanType.toString.encrypted,
    dateSharesCeasedToBeSubjectToPlan = dateSharesCeasedToBeSubjectToPlan.encrypted,
    noOfShareSecuritiesAwarded = noOfShareSecuritiesAwarded.encrypted,
    classOfShareAwarded = classOfShareAwarded.encrypted,
    dateSharesAwarded = dateSharesAwarded.encrypted,
    sharesSubjectToRestrictions = sharesSubjectToRestrictions.encrypted,
    electionEnteredIgnoreRestrictions = electionEnteredIgnoreRestrictions.encrypted,
    actualMarketValueOfSharesOnAward = actualMarketValueOfSharesOnAward.encrypted,
    unrestrictedMarketValueOfSharesOnAward = unrestrictedMarketValueOfSharesOnAward.encrypted,
    amountPaidForSharesOnAward = amountPaidForSharesOnAward.encrypted,
    marketValueAfterRestrictionsLifted = marketValueAfterRestrictionsLifted.encrypted,
    taxableAmount = taxableAmount.encrypted
  )
}

object SharesAwardedOrReceived {
  implicit val format: OFormat[SharesAwardedOrReceived] = Json.format[SharesAwardedOrReceived]
}

case class EncryptedSharesAwardedOrReceived(employerName: EncryptedValue,
                                            employerRef: Option[EncryptedValue] = None,
                                            schemePlanType: EncryptedValue,
                                            dateSharesCeasedToBeSubjectToPlan: EncryptedValue,
                                            noOfShareSecuritiesAwarded: EncryptedValue,
                                            classOfShareAwarded: EncryptedValue,
                                            dateSharesAwarded: EncryptedValue,
                                            sharesSubjectToRestrictions: EncryptedValue,
                                            electionEnteredIgnoreRestrictions: EncryptedValue,
                                            actualMarketValueOfSharesOnAward: EncryptedValue,
                                            unrestrictedMarketValueOfSharesOnAward: EncryptedValue,
                                            amountPaidForSharesOnAward: EncryptedValue,
                                            marketValueAfterRestrictionsLifted: EncryptedValue,
                                            taxableAmount: EncryptedValue) {

  def decrypted()(implicit secureGCMCipher: AesGcmAdCrypto, associatedText: String): SharesAwardedOrReceived = SharesAwardedOrReceived(
    employerName = employerName.decrypted[String],
    employerRef = employerRef.map(_.decrypted[String]),
    schemePlanType = ShareAwardedShareSchemePlanType.fromString(schemePlanType.decrypted[String]).get,
    dateSharesCeasedToBeSubjectToPlan = dateSharesCeasedToBeSubjectToPlan.decrypted[LocalDate],
    noOfShareSecuritiesAwarded = noOfShareSecuritiesAwarded.decrypted[Int],
    classOfShareAwarded = classOfShareAwarded.decrypted[String],
    dateSharesAwarded = dateSharesAwarded.decrypted[LocalDate],
    sharesSubjectToRestrictions = sharesSubjectToRestrictions.decrypted[Boolean],
    electionEnteredIgnoreRestrictions = electionEnteredIgnoreRestrictions.decrypted[Boolean],
    actualMarketValueOfSharesOnAward = actualMarketValueOfSharesOnAward.decrypted[BigDecimal],
    unrestrictedMarketValueOfSharesOnAward = unrestrictedMarketValueOfSharesOnAward.decrypted[BigDecimal],
    amountPaidForSharesOnAward = amountPaidForSharesOnAward.decrypted[BigDecimal],
    marketValueAfterRestrictionsLifted = marketValueAfterRestrictionsLifted.decrypted[BigDecimal],
    taxableAmount = taxableAmount.decrypted[BigDecimal]
  )
}

object EncryptedSharesAwardedOrReceived {
  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]

  implicit val format: Format[EncryptedSharesAwardedOrReceived] = Json.format[EncryptedSharesAwardedOrReceived]
}
