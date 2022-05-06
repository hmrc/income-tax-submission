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

import builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import builders.models.employment.HmrcEmploymentSourceBuilder.aHmrcEmploymentSource
import utils.TestUtils

class AllEmploymentDataSpec extends TestUtils {

  ".excludePensionIncome" should {
    "return AllEmploymentData without pension income" in {
      val hmrcEmploymentSourceWithPensionIncome = aHmrcEmploymentSource.copy(occupationalPension = Some(true))
      val hmrcEmploymentSourceWithNoPensionIncome = aHmrcEmploymentSource.copy(occupationalPension = None, hmrcEmploymentFinancialData = None)
      val customerEmploymentSourceWithPensionIncome = anEmploymentSource.copy(occupationalPension = Some(true))
      val customerEmploymentSourceWithNoPensionIncome = anEmploymentSource.copy(occupationalPension = None, employmentData = None)

      val underTest = anAllEmploymentData.copy(
        hmrcEmploymentData = Seq(hmrcEmploymentSourceWithPensionIncome, hmrcEmploymentSourceWithNoPensionIncome),
        customerEmploymentData = Seq(customerEmploymentSourceWithPensionIncome, customerEmploymentSourceWithNoPensionIncome)
      )

      underTest.excludePensionIncome() mustBe anAllEmploymentData.copy(
        hmrcEmploymentData = Seq(hmrcEmploymentSourceWithNoPensionIncome),
        customerEmploymentData = Seq(customerEmploymentSourceWithNoPensionIncome)
      )
    }
  }
}
