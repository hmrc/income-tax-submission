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

package builders.models.employment

import builders.models.employment.EmploymentExpensesBuilder.anEmploymentExpenses
import builders.models.employment.EmploymentSourceBuilder.anEmploymentSource
import builders.models.employment.HmrcEmploymentSourceBuilder.{aHmrcEmploymentSource, aHmrcEmploymentSourceOccPen}
import builders.models.otheremployment.OtherEmploymentIncomeBuilder.anOtherEmploymentIncome
import models.employment.AllEmploymentData

object AllEmploymentDataBuilder {

  val anAllEmploymentData: AllEmploymentData = AllEmploymentData(
    hmrcEmploymentData = Seq(aHmrcEmploymentSource),
    hmrcExpenses = Some(anEmploymentExpenses),
    customerEmploymentData = Seq(anEmploymentSource),
    customerExpenses = Some(anEmploymentExpenses),
    otherEmploymentIncome = Some(anOtherEmploymentIncome)
  )

  val anAllEmploymentDataWithOccPen: AllEmploymentData = AllEmploymentData(
    hmrcEmploymentData = Seq(aHmrcEmploymentSource, aHmrcEmploymentSourceOccPen),
    hmrcExpenses = Some(anEmploymentExpenses),
    customerEmploymentData = Seq(anEmploymentSource),
    customerExpenses = Some(anEmploymentExpenses),
    otherEmploymentIncome = Some(anOtherEmploymentIncome)
  )
}