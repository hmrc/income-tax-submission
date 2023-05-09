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

package models

import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.crypto.EncryptedValue

case class StockDividends(
                           submittedOn: Option[String] = None,
                           foreignDividend: Option[Seq[ForeignInterestModel]] = None,
                           dividendIncomeReceivedWhilstAbroad: Option[Seq[ForeignInterestModel]] = None,
                           stockDividend: Option[Dividend] = None,
                           redeemableShares: Option[Dividend] = None,
                           bonusIssuesOfSecurities: Option[Dividend] = None,
                           closeCompanyLoansWrittenOff: Option[Dividend] = None
                         )

object StockDividends {
  implicit val formats: OFormat[StockDividends] = Json.format[StockDividends]
}

case class EncryptedStockDividends(
                                    submittedOn: Option[EncryptedValue] = None,
                                    foreignDividends: Option[Seq[EncryptedForeignInterestModel]] = None,
                                    dividendIncomeReceivedWhilstAbroad: Option[Seq[EncryptedForeignInterestModel]] = None,
                                    stockDividends: Option[EncryptedDividend] = None,
                                    redeemableShares: Option[EncryptedDividend] = None,
                                    bonusIssuesOfSecurities: Option[EncryptedDividend] = None,
                                    closeCompanyLoansWrittenOff: Option[EncryptedDividend] = None
                                  )

object EncryptedStockDividends {
  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]
  implicit val formats: Format[EncryptedStockDividends] = Json.format[EncryptedStockDividends]
}

case class Dividend(customerReference: Option[String] = None, grossAmount: Option[BigDecimal] = None)

object Dividend {
  implicit val formats: OFormat[Dividend] = Json.format[Dividend]
}

case class EncryptedDividend(customerReference: Option[EncryptedValue] = None, grossAmount: Option[EncryptedValue] = None)

object EncryptedDividend {
  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]
  implicit val formats: Format[EncryptedDividend] = Json.format[EncryptedDividend]
}