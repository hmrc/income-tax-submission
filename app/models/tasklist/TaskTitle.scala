/*
 * Copyright 2024 HM Revenue & Customs
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

package models.tasklist

import models.Enumerable
import models.tasklist.taskItemTitles._

trait TaskTitle extends Enumerable.Implicits

object TaskTitle extends TaskTitle {

  private val aboutYouItemTitles: AboutYouItemTitles.type = AboutYouItemTitles
  private val charitableDonationsTitles: CharitableDonationsTitles.type = CharitableDonationsTitles
  private val employmentTitles: EmploymentTitles.type = EmploymentTitles
  private val selfEmploymentTitles: SelfEmploymentTitles.type = SelfEmploymentTitles
  private val esaTitles: EsaTitles.type = EsaTitles
  private val jsaTitles: JsaTitles.type = JsaTitles
  private val pensionsTitles: PensionsTitles.type = PensionsTitles
  private val paymentsIntoPensionsTitles: PaymentsIntoPensionsTitles.type = PaymentsIntoPensionsTitles
  private val ukInterestTitles: UkInterestTitles.type = UkInterestTitles
  private val ukDividendsTitles: UkDividendsTitles.type = UkDividendsTitles

  val values: Seq[TaskTitle] = Seq(
    aboutYouItemTitles.UkResidenceStatus(),
    aboutYouItemTitles.FosterCarer(),
    charitableDonationsTitles.DonationsUsingGiftAid(),
    charitableDonationsTitles.GiftsOfLandOrProperty(),
    charitableDonationsTitles.GiftsOfShares(),
    employmentTitles.PayeEmployment(),
    selfEmploymentTitles.CIS(),
    esaTitles.ESA(),
    jsaTitles.JSA(),
    pensionsTitles.StatePension(),
    pensionsTitles.OtherUkPensions(),
    pensionsTitles.IncomeFromOverseas(),
    pensionsTitles.UnauthorisedPayments(),
    pensionsTitles.ShortServiceRefunds(),
    paymentsIntoPensionsTitles.PaymentsIntoUk(),
    paymentsIntoPensionsTitles.PaymentsIntoOverseas(),
    paymentsIntoPensionsTitles.OverseasTransfer(),
    ukInterestTitles.BanksAndBuilding(),
    ukInterestTitles.TrustFundBond(),
    ukInterestTitles.GiltEdged(),
    ukDividendsTitles.CashDividends(),
    ukDividendsTitles.StockDividends(),
    ukDividendsTitles.DividendsFromUnitTrusts(),
    ukDividendsTitles.FreeRedeemableShares(),
    ukDividendsTitles.CloseCompanyLoans()
  )

  implicit val enumerable: Enumerable[TaskTitle] =
    Enumerable(values.map(v => v.toString -> v): _*)

}