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

import builders.models.pensions.PensionsBuilder.aPensions
import models.pensions._
import play.api.libs.json.{JsObject, JsValue, Json}
import utils.TaxYearUtils.taxYearEOY
import utils.TestUtils

class PensionsSpec extends TestUtils {


  val aStateBenefitsDataJsValue: JsValue = Json.parse(
    s"""
       |{
       |  "incapacityBenefit": [
       |    {
       |      "dateIgnored": "${taxYearEOY - 1}-07-08T05:23:00Z",
       |      "submittedOn": "$taxYearEOY-03-13T19:23:00Z",
       |      "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c936",
       |      "startDate": "${taxYearEOY - 1}-04-23",
       |      "endDate": "${taxYearEOY}-08-13",
       |      "amount": 300.00,
       |      "taxPaid": 400.00
       |    }
       |  ],
       |  "statePension": {
       |      "dateIgnored": "${taxYearEOY - 1}-07-08T05:23:00Z",
       |      "submittedOn": "$taxYearEOY-03-13T19:23:00Z",
       |      "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c936",
       |      "startDate": "${taxYearEOY - 1}-04-23",
       |      "endDate": "${taxYearEOY}-08-13",
       |      "amount": 300.00,
       |      "taxPaid": 400.00
       |    },
       |  "statePensionLumpSum": {
       |      "dateIgnored": "${taxYearEOY - 1}-07-08T05:23:00Z",
       |      "submittedOn": "$taxYearEOY-03-13T19:23:00Z",
       |      "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c936",
       |      "startDate": "${taxYearEOY - 1}-04-23",
       |      "endDate": "${taxYearEOY}-08-13",
       |      "amount": 300.00,
       |      "taxPaid": 400.00
       |    },
       |  "employmentSupportAllowance": [
       |{
       |      "dateIgnored": "${taxYearEOY - 1}-07-08T05:23:00Z",
       |      "submittedOn": "$taxYearEOY-03-13T19:23:00Z",
       |      "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c936",
       |      "startDate": "${taxYearEOY - 1}-04-23",
       |      "endDate": "${taxYearEOY}-08-13",
       |      "amount": 300.00,
       |      "taxPaid": 400.00
       |    }
       |  ],
       |  "jobSeekersAllowance": [
       |{
       |      "dateIgnored": "${taxYearEOY - 1}-07-08T05:23:00Z",
       |      "submittedOn": "$taxYearEOY-03-13T19:23:00Z",
       |      "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c936",
       |      "startDate": "${taxYearEOY - 1}-04-23",
       |      "endDate": "${taxYearEOY}-08-13",
       |      "amount": 300.00,
       |      "taxPaid": 400.00
       |    }
       |  ],
       |  "bereavementAllowance": {
       |      "dateIgnored": "${taxYearEOY - 1}-07-08T05:23:00Z",
       |      "submittedOn": "$taxYearEOY-03-13T19:23:00Z",
       |      "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c936",
       |      "startDate": "${taxYearEOY - 1}-04-23",
       |      "endDate": "${taxYearEOY}-08-13",
       |      "amount": 300.00,
       |      "taxPaid": 400.00
       |    },
       |  "otherStateBenefits": {
       |      "dateIgnored": "${taxYearEOY - 1}-07-08T05:23:00Z",
       |      "submittedOn": "$taxYearEOY-03-13T19:23:00Z",
       |      "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c936",
       |      "startDate": "${taxYearEOY - 1}-04-23",
       |      "endDate": "${taxYearEOY}-08-13",
       |      "amount": 300.00,
       |      "taxPaid": 400.00
       |    }
       |}
       |""".stripMargin)

  val aCustomerAddedStateBenefitsDataJsValue: JsValue = Json.parse(
    s"""
      |{
      |  "incapacityBenefit": [
      |      {
      |        "submittedOn": "${taxYearEOY}-03-13T19:23:00Z",
      |        "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c941",
      |        "startDate": "${taxYearEOY - 1}-04-23",
      |        "endDate": "${taxYearEOY}-08-13",
      |        "amount": 100.00,
      |        "taxPaid": 200.00
      |      }
      |  ],
      |  "statePension": [
      |      {
      |        "submittedOn": "${taxYearEOY}-03-13T19:23:00Z",
      |        "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c941",
      |        "startDate": "${taxYearEOY - 1}-04-23",
      |        "endDate": "${taxYearEOY}-08-13",
      |        "amount": 100.00,
      |        "taxPaid": 200.00
      |      }
      |  ],
      |  "statePensionLumpSum": [
      |      {
      |        "submittedOn": "${taxYearEOY}-03-13T19:23:00Z",
      |        "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c941",
      |        "startDate": "${taxYearEOY - 1}-04-23",
      |        "endDate": "${taxYearEOY}-08-13",
      |        "amount": 100.00,
      |        "taxPaid": 200.00
      |      }
      |  ],
      |  "employmentSupportAllowance": [
      |      {
      |        "submittedOn": "${taxYearEOY}-03-13T19:23:00Z",
      |        "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c941",
      |        "startDate": "${taxYearEOY - 1}-04-23",
      |        "endDate": "${taxYearEOY}-08-13",
      |        "amount": 100.00,
      |        "taxPaid": 200.00
      |      }
      |  ],
      |  "jobSeekersAllowance": [
      |      {
      |        "submittedOn": "${taxYearEOY}-03-13T19:23:00Z",
      |        "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c941",
      |        "startDate": "${taxYearEOY - 1}-04-23",
      |        "endDate": "${taxYearEOY}-08-13",
      |        "amount": 100.00,
      |        "taxPaid": 200.00
      |      }
      |  ],
      |  "bereavementAllowance": [
      |      {
      |        "submittedOn": "${taxYearEOY}-03-13T19:23:00Z",
      |        "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c941",
      |        "startDate": "${taxYearEOY - 1}-04-23",
      |        "endDate": "${taxYearEOY}-08-13",
      |        "amount": 100.00,
      |        "taxPaid": 200.00
      |      }
      |  ],
      |  "otherStateBenefits": [
      |      {
      |        "submittedOn": "${taxYearEOY}-03-13T19:23:00Z",
      |        "benefitId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c941",
      |        "startDate": "${taxYearEOY - 1}-04-23",
      |        "endDate": "${taxYearEOY}-08-13",
      |        "amount": 100.00,
      |        "taxPaid": 200.00
      |      }
      |  ]
      |}
      |""".stripMargin)


  val fullJson: JsObject = Json.obj(
    "pensionReliefs" -> Json.obj(
      "submittedOn" -> "2020-01-04T05:01:01Z",
      "deletedOn" -> "2020-01-04T05:01:01Z",
      "pensionReliefs" -> Json.obj(
        "regularPensionContributions" -> 100.0,
        "oneOffPensionContributionsPaid" -> 200.0,
        "retirementAnnuityPayments" -> 300.0,
        "paymentToEmployersSchemeNoTaxRelief" -> 400.0,
        "overseasPensionSchemeContributions" -> 500.0
      )),
    "pensionCharges" -> Json.obj(
      "submittedOn" -> "2020-07-27T17:00:19Z",
      "pensionSavingsTaxCharges" -> Json.obj(
        "pensionSchemeTaxReference" -> Json.arr("00123456RA", "00123456RB"),
        "lumpSumBenefitTakenInExcessOfLifetimeAllowance" -> Json.obj(
          "amount" -> 100.0,
          "taxPaid" -> 200.0
        ),
        "benefitInExcessOfLifetimeAllowance" -> Json.obj(
          "amount" -> 100.0,
          "taxPaid" -> 200.0
        )
      ),
      "pensionSchemeOverseasTransfers" -> Json.obj(
        "overseasSchemeProvider" -> Json.arr(Json.obj(
          "providerName" -> "default-provider-name",
          "providerAddress" -> "default-address",
          "providerCountryCode" -> "ESP",
          "qualifyingRecognisedOverseasPensionScheme" -> Json.arr("Q100000", "Q100002")
        )),
        "transferCharge" -> 100.0,
        "transferChargeTaxPaid" -> 200.0
      ),
      "pensionSchemeUnauthorisedPayments" -> Json.obj(
        "pensionSchemeTaxReference" -> Json.arr("00123456RA"),
        "surcharge" -> Json.obj(
          "amount" -> 100.0,
          "foreignTaxPaid" -> 200.0
        ),
        "noSurcharge" -> Json.obj(
          "amount" -> 100.0,
          "foreignTaxPaid" -> 200.0
        )
      ),
      "pensionContributions" -> Json.obj(
        "pensionSchemeTaxReference" -> Json.arr("00123456RA", "00123456RB"),
        "inExcessOfTheAnnualAllowance" -> 100.0,
        "annualAllowanceTaxPaid" -> 200.0,
        "isAnnualAllowanceReduced" -> false,
        "taperedAnnualAllowance" -> false,
        "moneyPurchasedAllowance" -> false
      ),
      "overseasPensionContributions" -> Json.obj(
        "overseasSchemeProvider" -> Json.arr(Json.obj(
          "providerName" -> "default-provider-name",
          "providerAddress" -> "default-address",
          "providerCountryCode" -> "ESP",
          "qualifyingRecognisedOverseasPensionScheme" -> Json.arr("Q100000", "Q100002")
        )),
        "shortServiceRefund" -> 100.0,
        "shortServiceRefundTaxPaid" -> 200.0
      )
    ),
    "stateBenefits" -> Json.obj(
      "stateBenefits" -> aStateBenefitsDataJsValue,
      "customerAddedStateBenefits" -> aCustomerAddedStateBenefitsDataJsValue
    ),
    "pensionIncome" -> Json.obj(
      "submittedOn" -> "2022-07-28T07:59:39.041Z",
      "deletedOn" -> "2022-07-28T07:59:39.041Z",
      "foreignPension" -> Json.arr(Json.obj(
        "countryCode" -> "FRA",
        "amountBeforeTax" -> 1999.99,
        "taxTakenOff" -> 1999.99,
        "specialWithholdingTax" -> 1999.99,
        "foreignTaxCreditRelief" -> false,
        "taxableAmount" -> 1999.99)),
      "overseasPensionContribution" -> Json.arr(Json.obj(
        "customerReference" -> "PENSIONINCOME245",
        "exemptEmployersPensionContribs" -> 1999.99,
        "migrantMemReliefQopsRefNo" -> "QOPS000000",
        "dblTaxationRelief" -> 1999.99,
        "dblTaxationCountry" -> "FRA",
        "dblTaxationArticle" -> "AB3211-1",
        "dblTaxationTreaty" -> "Munich",
        "sf74Reference" -> "SF74-123456"
      )
      )
    )
  )

  "PensionsModel" should {
    "parse from Json" in {
      fullJson.as[Pensions] mustBe aPensions
    }

    "parse to Json" in {
      Json.toJson(aPensions) mustBe fullJson
    }
  }
}

