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

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{JsPath, Json, OFormat, OWrites, Reads}
import uk.gov.hmrc.crypto.EncryptedValue
import utils.AesGcmAdCrypto
import utils.JsonUtils.jsonObjNoNulls

case class CustomerAddedStateBenefitsData(incapacityBenefits: Option[Set[CustomerAddedStateBenefit]] = None,
                                          statePensions: Option[Set[CustomerAddedStateBenefit]] = None,
                                          statePensionLumpSums: Option[Set[CustomerAddedStateBenefit]] = None,
                                          employmentSupportAllowances: Option[Set[CustomerAddedStateBenefit]] = None,
                                          jobSeekersAllowances: Option[Set[CustomerAddedStateBenefit]] = None,
                                          bereavementAllowances: Option[Set[CustomerAddedStateBenefit]] = None,
                                          otherStateBenefits: Option[Set[CustomerAddedStateBenefit]] = None) {

  def encrypted()(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): EncryptedCustomerAddedStateBenefitsData
  = EncryptedCustomerAddedStateBenefitsData(
    incapacityBenefits = incapacityBenefits.map(_.map(_.encrypted())),
    statePensions = statePensions.map(_.map(_.encrypted())),
    statePensionLumpSums = statePensionLumpSums.map(_.map(_.encrypted())),
    employmentSupportAllowances = employmentSupportAllowances.map(_.map(_.encrypted())),
    jobSeekersAllowances = jobSeekersAllowances.map(_.map(_.encrypted())),
    bereavementAllowances = bereavementAllowances.map(_.map(_.encrypted())),
    otherStateBenefits = otherStateBenefits.map(_.map(_.encrypted()))
  )
}

object CustomerAddedStateBenefitsData {

  implicit val customerAddedStateBenefitsDataWrites: OWrites[CustomerAddedStateBenefitsData] = (data: CustomerAddedStateBenefitsData) => {
    jsonObjNoNulls(
      "incapacityBenefit" -> data.incapacityBenefits,
      "statePension" -> data.statePensions,
      "statePensionLumpSum" -> data.statePensionLumpSums,
      "employmentSupportAllowance" -> data.employmentSupportAllowances,
      "jobSeekersAllowance" -> data.jobSeekersAllowances,
      "bereavementAllowance" -> data.bereavementAllowances,
      "otherStateBenefits" -> data.otherStateBenefits
    )
  }

  implicit val customerAddedStateBenefitsDataReads: Reads[CustomerAddedStateBenefitsData] = (
    (JsPath \ "incapacityBenefit").readNullable[Set[CustomerAddedStateBenefit]] and
      (JsPath \ "statePension").readNullable[Set[CustomerAddedStateBenefit]] and
      (JsPath \ "statePensionLumpSum").readNullable[Set[CustomerAddedStateBenefit]] and
      (JsPath \ "employmentSupportAllowance").readNullable[Set[CustomerAddedStateBenefit]] and
      (JsPath \ "jobSeekersAllowance").readNullable[Set[CustomerAddedStateBenefit]] and
      (JsPath \ "bereavementAllowance").readNullable[Set[CustomerAddedStateBenefit]] and
      (JsPath \ "otherStateBenefits").readNullable[Set[CustomerAddedStateBenefit]]
    ) (CustomerAddedStateBenefitsData.apply _)
}

case class EncryptedCustomerAddedStateBenefitsData(incapacityBenefits: Option[Set[EncryptedCustomerAddedStateBenefit]] = None,
                                                   statePensions: Option[Set[EncryptedCustomerAddedStateBenefit]] = None,
                                                   statePensionLumpSums: Option[Set[EncryptedCustomerAddedStateBenefit]] = None,
                                                   employmentSupportAllowances: Option[Set[EncryptedCustomerAddedStateBenefit]] = None,
                                                   jobSeekersAllowances: Option[Set[EncryptedCustomerAddedStateBenefit]] = None,
                                                   bereavementAllowances: Option[Set[EncryptedCustomerAddedStateBenefit]] = None,
                                                   otherStateBenefits: Option[Set[EncryptedCustomerAddedStateBenefit]] = None) {

  def decrypted()(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): CustomerAddedStateBenefitsData = CustomerAddedStateBenefitsData(
    incapacityBenefits = incapacityBenefits.map(_.map(_.decrypted())),
    statePensions = statePensions.map(_.map(_.decrypted())),
    statePensionLumpSums = statePensionLumpSums.map(_.map(_.decrypted())),
    employmentSupportAllowances = employmentSupportAllowances.map(_.map(_.decrypted())),
    jobSeekersAllowances = jobSeekersAllowances.map(_.map(_.decrypted())),
    bereavementAllowances = bereavementAllowances.map(_.map(_.decrypted())),
    otherStateBenefits = otherStateBenefits.map(_.map(_.decrypted()))
  )
}

object EncryptedCustomerAddedStateBenefitsData {
  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]


  implicit val encryptedCustomerAddedStateBenefitsDataWrites: OWrites[EncryptedCustomerAddedStateBenefitsData] =
    (data: EncryptedCustomerAddedStateBenefitsData) => {
      jsonObjNoNulls(
        "incapacityBenefit" -> data.incapacityBenefits,
        "statePension" -> data.statePensions,
        "statePensionLumpSum" -> data.statePensionLumpSums,
        "employmentSupportAllowance" -> data.employmentSupportAllowances,
        "jobSeekersAllowance" -> data.jobSeekersAllowances,
        "bereavementAllowance" -> data.bereavementAllowances,
        "otherStateBenefits" -> data.otherStateBenefits
      )
    }

  implicit val encryptedCustomerAddedStateBenefitsDataReads: Reads[EncryptedCustomerAddedStateBenefitsData] = (
    (JsPath \ "incapacityBenefit").readNullable[Set[EncryptedCustomerAddedStateBenefit]] and
      (JsPath \ "statePension").readNullable[Set[EncryptedCustomerAddedStateBenefit]] and
      (JsPath \ "statePensionLumpSum").readNullable[Set[EncryptedCustomerAddedStateBenefit]] and
      (JsPath \ "employmentSupportAllowance").readNullable[Set[EncryptedCustomerAddedStateBenefit]] and
      (JsPath \ "jobSeekersAllowance").readNullable[Set[EncryptedCustomerAddedStateBenefit]] and
      (JsPath \ "bereavementAllowance").readNullable[Set[EncryptedCustomerAddedStateBenefit]] and
      (JsPath \ "otherStateBenefits").readNullable[Set[EncryptedCustomerAddedStateBenefit]]
    ) (EncryptedCustomerAddedStateBenefitsData.apply _)
}
