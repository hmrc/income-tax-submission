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

package builders.models

import builders.models.SavingsIncomeBuilder.anSavingIncome
import models.{Dividend, StockDividends}

object StockDividendsBuilder {

  val anStockDividends: StockDividends = StockDividends(
    submittedOn = Some(""),
    foreignDividend = anSavingIncome.foreignInterest,
    dividendIncomeReceivedWhilstAbroad = anSavingIncome.foreignInterest,
    stockDividend = Some(Dividend(customerReference = Some("reference"), grossAmount = Some(123.45))),
    redeemableShares = Some(Dividend(customerReference = Some("reference"), grossAmount = Some(123.45))),
    bonusIssuesOfSecurities = Some(Dividend(customerReference = Some("reference"), grossAmount = Some(123.45))),
    closeCompanyLoansWrittenOff = Some(Dividend(customerReference = Some("reference"), grossAmount = Some(123.45))))
}
