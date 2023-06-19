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

import builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import builders.models.mongo.UserDataBuilder.aUserData
import helpers.IntegrationSpec
import models.mongo.EncryptedUserData

class EncryptionServiceISpec extends IntegrationSpec {

  private val underTest: EncryptionService = app.injector.instanceOf[EncryptionService]

  "encryptUserData" should {
    val data = aUserData.copy(employment = Some(anAllEmploymentData))

    "encrypt all the user data apart from the look up ids and timestamp" in {
      val result = underTest.encryptUserData(data)
      result mustBe EncryptedUserData(
        sessionId = data.sessionId,
        mtdItId = data.mtdItId,
        nino = data.nino,
        taxYear = data.taxYear,
        dividends = result.dividends,
        interest = result.interest,
        giftAid = result.giftAid,
        employment = result.employment,
        pensions = result.pensions,
        cis = result.cis,
        stateBenefits = result.stateBenefits,
        interestSavings = result.interestSavings,
        gains = result.gains,
        otherEmploymentIncome = result.otherEmploymentIncome,
        lastUpdated = data.lastUpdated
      )
    }

    "encrypt the data and decrypt it back to the initial model" in {
      val encryptResult = underTest.encryptUserData(data)
      val decryptResult = underTest.decryptUserData(encryptResult)

      decryptResult mustBe data
    }
  }
}
