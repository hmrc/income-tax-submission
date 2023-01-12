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
import builders.models.employment.EmploymentFinancialDataBuilder.anEmploymentFinancialData
import utils.TestUtils

class EmploymentFinancialDataSpec extends TestUtils {

  "hasOccPen" should {
    "return true when employmentData has occPen" in {
      val underTest = anEmploymentFinancialData.copy(employmentData = Some(anEmploymentData.copy(occPen = Some(true))))

      underTest.hasOccPen mustBe true
    }

    "return false" when {
      "employmentData has no occPen" in {
        val underTest = anEmploymentFinancialData.copy(employmentData = Some(anEmploymentData.copy(occPen = None)))

        underTest.hasOccPen mustBe false
      }

      "employmentData is None" in {
        val underTest = anEmploymentFinancialData.copy(employmentData = None)

        underTest.hasOccPen mustBe false
      }
    }
  }
}
