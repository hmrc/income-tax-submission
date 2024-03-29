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

package models.mongo

import models._
import models.cis.{AllCISDeductions, EncryptedAllCISDeductions}
import models.employment.{AllEmploymentData, EncryptedAllEmploymentData}
import models.gains.{EncryptedInsurancePoliciesModel, InsurancePoliciesModel}
import models.gifts.{EncryptedGiftAid, GiftAid}
import models.pensions.{EncryptedPensions, Pensions}
import models.statebenefits.{AllStateBenefitsData, EncryptedAllStateBenefitsData}
import play.api.libs.json.{Json, OFormat}

import java.time.Instant

case class UserData(sessionId: String,
                    mtdItId: String,
                    nino: String,
                    taxYear: Int,
                    dividends: Option[Dividends] = None,
                    interest: Option[Seq[Interest]] = None,
                    giftAid: Option[GiftAid] = None,
                    employment: Option[AllEmploymentData] = None,
                    pensions: Option[Pensions] = None,
                    cis: Option[AllCISDeductions] = None,
                    stateBenefits: Option[AllStateBenefitsData] = None,
                    interestSavings: Option[SavingsIncomeDataModel] = None,
                    gains: Option[InsurancePoliciesModel] = None,
                    stockDividends: Option[StockDividends] = None,
                    lastUpdated: Instant = Instant.now()) {

  def toIncomeSourcesResponseModel: IncomeSources = {
    IncomeSources(None, dividends, interest, giftAid, employment, pensions, cis, stateBenefits, interestSavings, gains, stockDividends)
  }
}

object UserData {
  implicit lazy val formats: OFormat[UserData] = Json.format[UserData]
}

case class EncryptedUserData(sessionId: String,
                             mtdItId: String,
                             nino: String,
                             taxYear: Int,
                             dividends: Option[EncryptedDividends] = None,
                             interest: Option[Seq[EncryptedInterest]] = None,
                             giftAid: Option[EncryptedGiftAid] = None,
                             employment: Option[EncryptedAllEmploymentData] = None,
                             pensions: Option[EncryptedPensions] = None,
                             cis: Option[EncryptedAllCISDeductions] = None,
                             stateBenefits: Option[EncryptedAllStateBenefitsData] = None,
                             interestSavings: Option[EncryptedSavingsIncomeDataModel] = None,
                             gains: Option[EncryptedInsurancePoliciesModel] = None,
                             stockDividends: Option[EncryptedStockDividends] = None,
                             lastUpdated: Instant = Instant.now())

object EncryptedUserData {
  implicit lazy val formats: OFormat[EncryptedUserData] = Json.format[EncryptedUserData]
}
