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

import models.mongo.TextAndKey
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{JsPath, OWrites, Reads}
import utils.JsonUtils.jsonObjNoNulls
import utils.SecureGCMCipher

case class StateBenefitsData(incapacityBenefits: Option[Set[StateBenefit]] = None,
                             statePension: Option[StateBenefit] = None,
                             statePensionLumpSum: Option[StateBenefit] = None,
                             employmentSupportAllowances: Option[Set[StateBenefit]] = None,
                             jobSeekersAllowances: Option[Set[StateBenefit]] = None,
                             bereavementAllowance: Option[StateBenefit] = None,
                             other: Option[StateBenefit] = None) {

  def encrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedStateBenefitsData = EncryptedStateBenefitsData(
    incapacityBenefits = incapacityBenefits.map(_.map(_.encrypted)),
    statePension = statePension.map(_.encrypted),
    statePensionLumpSum = statePensionLumpSum.map(_.encrypted),
    employmentSupportAllowances = employmentSupportAllowances.map(_.map(_.encrypted)),
    jobSeekersAllowances = jobSeekersAllowances.map(_.map(_.encrypted)),
    bereavementAllowance = bereavementAllowance.map(_.encrypted),
    other = other.map(_.encrypted)
  )
}

object StateBenefitsData {

  implicit val stateBenefitsDataWrites: OWrites[StateBenefitsData] = (stateBenefitsData: StateBenefitsData) => {
    jsonObjNoNulls(
      "incapacityBenefit" -> stateBenefitsData.incapacityBenefits,
      "statePension" -> stateBenefitsData.statePension,
      "statePensionLumpSum" -> stateBenefitsData.statePensionLumpSum,
      "employmentSupportAllowance" -> stateBenefitsData.employmentSupportAllowances,
      "jobSeekersAllowance" -> stateBenefitsData.jobSeekersAllowances,
      "bereavementAllowance" -> stateBenefitsData.bereavementAllowance,
      "otherStateBenefits" -> stateBenefitsData.other
    )
  }

  implicit val stateBenefitsDataReads: Reads[StateBenefitsData] = (
    (JsPath \ "incapacityBenefit").readNullable[Set[StateBenefit]] and
      (JsPath \ "statePension").readNullable[StateBenefit] and
      (JsPath \ "statePensionLumpSum").readNullable[StateBenefit] and
      (JsPath \ "employmentSupportAllowance").readNullable[Set[StateBenefit]] and
      (JsPath \ "jobSeekersAllowance").readNullable[Set[StateBenefit]] and
      (JsPath \ "bereavementAllowance").readNullable[StateBenefit] and
      (JsPath \ "otherStateBenefits").readNullable[StateBenefit]
    ) (StateBenefitsData.apply _)
}

case class EncryptedStateBenefitsData(incapacityBenefits: Option[Set[EncryptedStateBenefit]] = None,
                                      statePension: Option[EncryptedStateBenefit] = None,
                                      statePensionLumpSum: Option[EncryptedStateBenefit] = None,
                                      employmentSupportAllowances: Option[Set[EncryptedStateBenefit]] = None,
                                      jobSeekersAllowances: Option[Set[EncryptedStateBenefit]] = None,
                                      bereavementAllowance: Option[EncryptedStateBenefit] = None,
                                      other: Option[EncryptedStateBenefit] = None) {

  def decrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): StateBenefitsData = StateBenefitsData(
    incapacityBenefits = incapacityBenefits.map(_.map(_.decrypted)),
    statePension = statePension.map(_.decrypted),
    statePensionLumpSum = statePensionLumpSum.map(_.decrypted),
    employmentSupportAllowances = employmentSupportAllowances.map(_.map(_.decrypted)),
    jobSeekersAllowances = jobSeekersAllowances.map(_.map(_.decrypted)),
    bereavementAllowance = bereavementAllowance.map(_.decrypted),
    other = other.map(_.decrypted)
  )
}

object EncryptedStateBenefitsData {
  implicit val encryptedStateBenefitsDataWrites: OWrites[EncryptedStateBenefitsData] = (stateBenefitsData: EncryptedStateBenefitsData) => {
    jsonObjNoNulls(
      "incapacityBenefit" -> stateBenefitsData.incapacityBenefits,
      "statePension" -> stateBenefitsData.statePension,
      "statePensionLumpSum" -> stateBenefitsData.statePensionLumpSum,
      "employmentSupportAllowance" -> stateBenefitsData.employmentSupportAllowances,
      "jobSeekersAllowance" -> stateBenefitsData.jobSeekersAllowances,
      "bereavementAllowance" -> stateBenefitsData.bereavementAllowance,
      "otherStateBenefits" -> stateBenefitsData.other
    )
  }

  implicit val encryptedStateBenefitsDataReads: Reads[EncryptedStateBenefitsData] = (
    (JsPath \ "incapacityBenefit").readNullable[Set[EncryptedStateBenefit]] and
      (JsPath \ "statePension").readNullable[EncryptedStateBenefit] and
      (JsPath \ "statePensionLumpSum").readNullable[EncryptedStateBenefit] and
      (JsPath \ "employmentSupportAllowance").readNullable[Set[EncryptedStateBenefit]] and
      (JsPath \ "jobSeekersAllowance").readNullable[Set[EncryptedStateBenefit]] and
      (JsPath \ "bereavementAllowance").readNullable[EncryptedStateBenefit] and
      (JsPath \ "otherStateBenefits").readNullable[EncryptedStateBenefit]
    ) (EncryptedStateBenefitsData.apply _)
}