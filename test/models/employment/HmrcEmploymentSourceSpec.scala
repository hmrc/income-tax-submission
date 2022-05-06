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

package models.employment

import builders.models.employment.EmploymentDataBuilder.anEmploymentData
import builders.models.employment.EmploymentFinancialDataBuilder.anEmploymentFinancialData
import builders.models.employment.HmrcEmploymentSourceBuilder.aHmrcEmploymentSource
import utils.TestUtils

class HmrcEmploymentSourceSpec extends TestUtils {

  ".hasOccupationalPension" should {
    "return true" when {
      "occupationalPension is true" in {
        val underTest = aHmrcEmploymentSource.copy(occupationalPension = Some(true), hmrcEmploymentFinancialData = None)

        underTest.hasOccupationalPension mustBe true
      }

      "hmrcEmploymentFinancialData hasOccPen is true" in {
        val employmentDataWithOccPen = anEmploymentFinancialData.copy(employmentData = Some(anEmploymentData.copy(occPen = Some(true))))
        val underTest = aHmrcEmploymentSource.copy(occupationalPension = None, hmrcEmploymentFinancialData = Some(employmentDataWithOccPen))

        underTest.hasOccupationalPension mustBe true
      }
    }

    "return false " when {
      "occupationalPension is false and hmrcEmploymentFinancialData hasOccPen is false" in {
        val employmentDataWithoutOccPen = anEmploymentFinancialData.copy(employmentData = Some(anEmploymentData.copy(occPen = Some(false))))
        val underTest = aHmrcEmploymentSource.copy(occupationalPension = Some(false), hmrcEmploymentFinancialData = Some(employmentDataWithoutOccPen))

        underTest.hasOccupationalPension mustBe false
      }

      "occupationalPension is None and hmrcEmploymentFinancialData hasOccPen is false" in {
        val employmentDataWithoutOccPen = anEmploymentFinancialData.copy(employmentData = Some(anEmploymentData.copy(occPen = Some(false))))
        val underTest = aHmrcEmploymentSource.copy(occupationalPension = None, hmrcEmploymentFinancialData = Some(employmentDataWithoutOccPen))

        underTest.hasOccupationalPension mustBe false
      }
    }
  }
}
