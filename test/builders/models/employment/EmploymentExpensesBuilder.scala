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

import builders.models.employment.ExpensesBuilder.anExpenses
import models.employment.EmploymentExpenses

object EmploymentExpensesBuilder {

  val anEmploymentExpenses: EmploymentExpenses = EmploymentExpenses(
    submittedOn = Some("2020-01-04T05:01:01Z"),
    dateIgnored = Some("2020-01-04T05:01:01Z"),
    totalExpenses = Some(800.0),
    expenses = Some(anExpenses)
  )
}
