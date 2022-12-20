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

package builders.models

import models.{ForeignInterestModel, SavingsIncomeDataModel, SecuritiesModel}

object SavingsIncomeBuilder {

  val anSavingIncome: SavingsIncomeDataModel = SavingsIncomeDataModel(
    submittedOn = Some("2020-02-04T05:01:01Z"),
    securities = Some(SecuritiesModel(
      Some(800.67),
      7455.99,
      Some(6123.2)
    )),
    foreignInterest = Some(Seq(ForeignInterestModel(
      "BES",
      Some(1232.56),
      Some(3422.22),
      Some(5622.67),
      Some(true),
      2821.92)
    ))
  )
}
