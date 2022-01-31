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

package models

import models.pensions._
import play.api.libs.json.{JsValue, Json}
import utils.TestUtils

class PensionsModelSpec extends TestUtils {

  val fullJson: JsValue = Json.parse(
    """{
      |"taxYear": 2022,
      |"pensionReliefs": {
      |"submittedOn": "2020-01-04T05:01:01Z",
      |    "deletedOn": "2020-01-04T05:01:01Z",
      |    "pensionReliefs": {
      |      "regularPensionContributions": 100.01,
      |      "oneOffPensionContributionsPaid": 100.01,
      |      "retirementAnnuityPayments": 100.01,
      |      "paymentToEmployersSchemeNoTaxRelief": 100.01,
      |      "overseasPensionSchemeContributions": 100.01
      |    }
      |},
      |"pensionCharges": {
      |	"submittedOn": "2020-07-27T17:00:19Z",
      |	"pensionSavingsTaxCharges": {
      |		"pensionSchemeTaxReference": ["00123456RA", "00123456RB"],
      |		"lumpSumBenefitTakenInExcessOfLifetimeAllowance": {
      |			"amount": 800.02,
      |			"taxPaid": 200.02
      |		},
      |		"benefitInExcessOfLifetimeAllowance": {
      |			"amount": 800.02,
      |			"taxPaid": 200.02
      |		},
      |		"isAnnualAllowanceReduced": false,
      |		"taperedAnnualAllowance": false,
      |		"moneyPurchasedAllowance": false
      |	},
      |	"pensionSchemeOverseasTransfers": {
      |		"overseasSchemeProvider": [{
      |			"providerName": "overseas providerName 1 qualifying scheme",
      |			"providerAddress": "overseas address 1",
      |			"providerCountryCode": "ESP",
      |			"qualifyingRecognisedOverseasPensionScheme": ["Q100000", "Q100002"]
      |		}],
      |		"transferCharge": 22.77,
      |		"transferChargeTaxPaid": 33.88
      |	},
      |	"pensionSchemeUnauthorisedPayments": {
      |		"pensionSchemeTaxReference": ["00123456RA", "00123456RB"],
      |		"surcharge": {
      |			"amount": 124.44,
      |			"foreignTaxPaid": 123.33
      |		},
      |		"noSurcharge": {
      |			"amount": 222.44,
      |			"foreignTaxPaid": 223.33
      |		}
      |	},
      |	"pensionContributions": {
      |		"pensionSchemeTaxReference": ["00123456RA", "00123456RB"],
      |		"inExcessOfTheAnnualAllowance": 150.67,
      |		"annualAllowanceTaxPaid": 178.65
      |	},
      |	"overseasPensionContributions": {
      |		"overseasSchemeProvider": [{
      |			"providerName": "overseas providerName 1 tax ref",
      |			"providerAddress": "overseas address 1",
      |			"providerCountryCode": "ESP",
      |			"pensionSchemeTaxReference": ["00123456RA", "00123456RB"]
      |		}],
      |		"shortServiceRefund": 1.11,
      |		"shortServiceRefundTaxPaid": 2.22
      |	}
      | }
      |}""".stripMargin)

  "PensionsModel" should {
    "parse from Json" in {
      fullJson.as[PensionsModel] mustBe fullPensionsModel
    }

    "parse to Json" in {
      Json.toJson(fullPensionsModel) mustBe fullJson
    }
  }

}

