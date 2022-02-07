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

package services

import helpers.IntegrationSpec
import models.mongo.EncryptedUserData
import utils.SecureGCMCipher

class EncryptionServiceISpec extends IntegrationSpec{

  val service: EncryptionService = app.injector.instanceOf[EncryptionService]
  val encryption: SecureGCMCipher = app.injector.instanceOf[SecureGCMCipher]

  "encryptUserData" should {

    val data = userData.copy(employment = Some(allEmploymentData))

    "encrypt all the user data apart from the look up ids and timestamp" in {
      val result = service.encryptUserData(data)
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
        lastUpdated = data.lastUpdated
      )
    }

    "encrypt the data and decrypt it back to the initial model" in {
      val encryptResult = service.encryptUserData(data)
      val decryptResult = service.decryptUserData(encryptResult)

      decryptResult mustBe data

      decryptResult.employment.flatMap(_.hmrcEmploymentData.head.employmentData.flatMap(_.pay.flatMap(_.totalTaxToDate))) mustBe Some(6782.92)
      decryptResult.employment.flatMap(_.hmrcEmploymentData.head.employmentData.flatMap(_.pay.flatMap(_.taxMonthNo))) mustBe Some(2)
      decryptResult.employment.flatMap(_.hmrcEmploymentData.head.employmentData.flatMap(_.pay.flatMap(_.paymentDate))) mustBe Some("2020-04-23")
      decryptResult.employment.flatMap(_.hmrcEmploymentData.head.employmentData.flatMap(_.closeCompany)) mustBe Some(true)
      decryptResult.pensions.flatMap(_.pensionReliefs.flatMap(_.deletedOn)) mustBe Some("2020-01-04T05:01:01Z")
      decryptResult.dividends.flatMap(_.otherUkDividends) mustBe Some(100.00)
      decryptResult.interest.flatMap(_.head.taxedUkInterest) mustBe Some(100.00)
    }
  }

}
