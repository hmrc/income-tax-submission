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

package builders.models.pensions.income

import models.pensions.income.{ForeignPension, OverseasPensionContribution, PensionIncomeModel}

object PensionIncome {

  val aPensionIncome: PensionIncomeModel =
    PensionIncomeModel(
      submittedOn = "2022-07-28T07:59:39.041Z",
      deletedOn = Some("2022-07-28T07:59:39.041Z"),
      foreignPension = Some(Seq(
        ForeignPension(
          countryCode = "FRA",
          taxableAmount = 1999.99,
          amountBeforeTax = Some(1999.99),
          taxTakenOff = Some(1999.99),
          specialWithholdingTax = Some(1999.99),
          foreignTaxCreditRelief = Some(false)
        )
      )),
      overseasPensionContribution = Some(Seq(
        OverseasPensionContribution(
          customerReference = Some("PENSIONINCOME245"),
          exemptEmployersPensionContribs = 1999.99,
          migrantMemReliefQopsRefNo = Some("QOPS000000"),
          dblTaxationRelief = Some(1999.99),
          dblTaxationCountry = Some("FRA"),
          dblTaxationArticle = Some("AB3211-1"),
          dblTaxationTreaty = Some("Munich"),
          sf74Reference = Some("SF74-123456")
        ))
      )
    )
}
