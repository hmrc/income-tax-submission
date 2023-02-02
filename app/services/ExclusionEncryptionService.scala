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

package services

import models.mongo._
import models.{EncryptedExcludeJourneyModel, ExcludeJourneyModel}
import utils.AesGcmAdCrypto
import utils.CypherSyntax.{DecryptableOps, EncryptableOps}

import javax.inject.Inject

class ExclusionEncryptionService @Inject()(implicit encryptionService: AesGcmAdCrypto) {

  def encryptExclusionUserData(exclusionUserDataModel: ExclusionUserDataModel): EncryptedExclusionUserDataModel = {
    implicit val associatedText: String = exclusionUserDataModel.nino

    EncryptedExclusionUserDataModel(
      nino = exclusionUserDataModel.nino,
      taxYear = exclusionUserDataModel.taxYear,
      exclusionModel = exclusionUserDataModel.exclusionModel.map(encryptExcludeJourneyModel),
      lastUpdated = exclusionUserDataModel.lastUpdated
    )
  }

  def decryptExclusionUserData(encryptedExclusionUserDataModel: EncryptedExclusionUserDataModel): ExclusionUserDataModel = {
    implicit val associatedText: String = encryptedExclusionUserDataModel.nino

    ExclusionUserDataModel(
      nino = encryptedExclusionUserDataModel.nino,
      taxYear = encryptedExclusionUserDataModel.taxYear,
      exclusionModel = encryptedExclusionUserDataModel.exclusionModel.map(decryptExcludeJourneyModel),
      lastUpdated = encryptedExclusionUserDataModel.lastUpdated
    )
  }

  private def encryptExcludeJourneyModel(model: ExcludeJourneyModel)
                                     (implicit associatedText: String): EncryptedExcludeJourneyModel ={
    EncryptedExcludeJourneyModel(
      model.journey.encrypted,
      model.hash.map(_.encrypted)
    )
  }
  private def decryptExcludeJourneyModel(model: EncryptedExcludeJourneyModel)
                                     (implicit associatedText: String): ExcludeJourneyModel ={
    ExcludeJourneyModel(
      model.journey.decrypted[String],
      model.hash.map(_.decrypted[String])
    )
  }
}
