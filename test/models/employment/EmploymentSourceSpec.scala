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

package models.employment

import builders.models.employment.EmploymentDataBuilder.anEmploymentData
import builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import utils.TestUtils

class EmploymentSourceSpec extends TestUtils {

  ".hasOccupationalPension" should {
    "return true" when {
      "occupationalPension is true" in {
        val underTest = anEmploymentSource.copy(occupationalPension = Some(true), employmentData = None)

        underTest.hasOccupationalPension mustBe true
      }

      "employmentData hasOccPen is true" in {
        val underTest = anEmploymentSource.copy(occupationalPension = None, employmentData = Some(anEmploymentData.copy(occPen = Some(true))))

        underTest.hasOccupationalPension mustBe true
      }
    }

    "return false " when {
      "occupationalPension is false and employmentData hasOccPen is false" in {
        val underTest = anEmploymentSource.copy(occupationalPension = Some(false), employmentData = Some(anEmploymentData.copy(occPen = Some(false))))

        underTest.hasOccupationalPension mustBe false
      }

      "occupationalPension is None and employmentData hasOccPen is false" in {
        val underTest = anEmploymentSource.copy(occupationalPension = None, employmentData = Some(anEmploymentData.copy(occPen = Some(false))))

        underTest.hasOccupationalPension mustBe false
      }
    }
  }
}
