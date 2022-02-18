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

package models.cis

import models.mongo.TextAndKey
import play.api.libs.json.{Json, OFormat}
import utils.EncryptableInstances._
import utils.EncryptableSyntax._
import utils.{EncryptedValue, SecureGCMCipher}

case class PeriodData(deductionFromDate: String,
                      deductionToDate: String,
                      deductionAmount: Option[BigDecimal],
                      costOfMaterials: Option[BigDecimal],
                      grossAmountPaid: Option[BigDecimal],
                      submissionDate: String,
                      submissionId: Option[String],
                      source: String) {

  def encrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedGetPeriodData = EncryptedGetPeriodData(
    deductionFromDate = deductionFromDate.encrypted,
    deductionToDate = deductionToDate.encrypted,
    deductionAmount = deductionAmount.map(_.encrypted),
    costOfMaterials = costOfMaterials.map(_.encrypted),
    grossAmountPaid = grossAmountPaid.map(_.encrypted),
    submissionDate = submissionDate.encrypted,
    submissionId = submissionId.map(_.encrypted),
    source = source.encrypted
  )
}

object PeriodData {
  implicit val format: OFormat[PeriodData] = Json.format[PeriodData]
}

case class EncryptedGetPeriodData(deductionFromDate: EncryptedValue,
                                  deductionToDate: EncryptedValue,
                                  deductionAmount: Option[EncryptedValue],
                                  costOfMaterials: Option[EncryptedValue],
                                  grossAmountPaid: Option[EncryptedValue],
                                  submissionDate: EncryptedValue,
                                  submissionId: Option[EncryptedValue],
                                  source: EncryptedValue) {

  def decrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): PeriodData = PeriodData(
    deductionFromDate = deductionFromDate.decrypted[String],
    deductionToDate = deductionToDate.decrypted[String],
    deductionAmount = deductionAmount.map(ev => ev.decrypted[BigDecimal]),
    costOfMaterials = costOfMaterials.map(ev => ev.decrypted[BigDecimal]),
    grossAmountPaid = grossAmountPaid.map(ev => ev.decrypted[BigDecimal]),
    submissionDate = submissionDate.decrypted[String],
    submissionId = submissionId.map(ev => ev.decrypted[String]),
    source = source.decrypted[String]
  )
}

object EncryptedGetPeriodData {
  implicit val format: OFormat[EncryptedGetPeriodData] = Json.format[EncryptedGetPeriodData]
}