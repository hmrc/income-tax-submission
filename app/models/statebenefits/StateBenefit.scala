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

package models.statebenefits

import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.crypto.EncryptedValue
import utils.CypherSyntax.{DecryptableOps, EncryptableOps}
import utils.AesGcmAdCrypto
import java.time.{Instant, LocalDate}

import java.util.UUID

case class StateBenefit(benefitId: UUID,
                        startDate: LocalDate,
                        endDate: Option[LocalDate] = None,
                        dateIgnored: Option[Instant] = None,
                        submittedOn: Option[Instant] = None,
                        amount: Option[BigDecimal] = None,
                        taxPaid: Option[BigDecimal] = None) {

  def encrypted()(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): EncryptedStateBenefit = EncryptedStateBenefit(
    benefitId = benefitId.toString.encrypted,
    startDate = startDate.toString.encrypted,
    endDate = endDate.map(_.toString.encrypted),
    dateIgnored = dateIgnored.map(_.toString.encrypted),
    submittedOn = submittedOn.map(_.toString.encrypted),
    amount = amount.map(_.encrypted),
    taxPaid = taxPaid.map(_.encrypted)
  )
}

object StateBenefit {
  implicit val format: OFormat[StateBenefit] = Json.format[StateBenefit]
}

case class EncryptedStateBenefit(benefitId: EncryptedValue,
                                 startDate: EncryptedValue,
                                 endDate: Option[EncryptedValue] = None,
                                 dateIgnored: Option[EncryptedValue] = None,
                                 submittedOn: Option[EncryptedValue] = None,
                                 amount: Option[EncryptedValue] = None,
                                 taxPaid: Option[EncryptedValue] = None) {

  def decrypted()(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): StateBenefit = StateBenefit(
    benefitId = benefitId.decrypted[UUID],
    startDate = startDate.decrypted[LocalDate],
    endDate = endDate.map(_.decrypted[LocalDate]),
    dateIgnored = dateIgnored.map(_.decrypted[Instant]),
    submittedOn = submittedOn.map(_.decrypted[Instant]),
    amount = amount.map(_.decrypted[BigDecimal]),
    taxPaid = taxPaid.map(_.decrypted[BigDecimal])
  )
}

object EncryptedStateBenefit {
  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]

  implicit val format: Format[EncryptedStateBenefit] = Json.format[EncryptedStateBenefit]
}