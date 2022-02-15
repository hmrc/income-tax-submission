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
import play.api.libs.json.{JsObject, Json}
import utils.TestUtils

class PensionsSpec extends TestUtils {

  val fullJson: JsObject = Json.obj(
    "pensionReliefs" -> Json.obj(
      "submittedOn" -> "2020-01-04T05:01:01Z",
      "deletedOn" -> "2020-01-04T05:01:01Z",
      "pensionReliefs" -> Json.obj(
        "regularPensionContributions" -> 100.01,
        "oneOffPensionContributionsPaid" -> 100.01,
        "retirementAnnuityPayments" -> 100.01,
        "paymentToEmployersSchemeNoTaxRelief" -> 100.01,
        "overseasPensionSchemeContributions" -> 100.01
      )),
    "pensionCharges" -> Json.obj(
      "submittedOn" -> "2020-07-27T17:00:19Z",
      "pensionSavingsTaxCharges" -> Json.obj(
        "pensionSchemeTaxReference" -> Json.arr("00123456RA", "00123456RB"),
        "lumpSumBenefitTakenInExcessOfLifetimeAllowance" -> Json.obj(
          "amount" -> 800.02,
          "taxPaid" -> 200.02
        ),
        "benefitInExcessOfLifetimeAllowance" -> Json.obj(
          "amount" -> 800.02,
          "taxPaid" -> 200.02
        ),
        "isAnnualAllowanceReduced" -> false,
        "taperedAnnualAllowance" -> false,
        "moneyPurchasedAllowance" -> false
      ),
      "pensionSchemeOverseasTransfers" -> Json.obj(
        "overseasSchemeProvider" -> Json.arr(Json.obj(
          "providerName" -> "overseas providerName 1 qualifying scheme",
          "providerAddress" -> "overseas address 1",
          "providerCountryCode" -> "ESP",
          "qualifyingRecognisedOverseasPensionScheme" -> Json.arr("Q100000", "Q100002")
        )),
        "transferCharge" -> 22.77,
        "transferChargeTaxPaid" -> 33.88
      ),
      "pensionSchemeUnauthorisedPayments" -> Json.obj(
        "pensionSchemeTaxReference" -> Json.arr("00123456RA", "00123456RB"),
        "surcharge" -> Json.obj(
          "amount" -> 124.44,
          "foreignTaxPaid" -> 123.33
        ),
        "noSurcharge" -> Json.obj(
          "amount" -> 222.44,
          "foreignTaxPaid" -> 223.33
        )
      ),
      "pensionContributions" -> Json.obj(
        "pensionSchemeTaxReference" -> Json.arr("00123456RA", "00123456RB"),
        "inExcessOfTheAnnualAllowance" -> 150.67,
        "annualAllowanceTaxPaid" -> 178.65
      ),
      "overseasPensionContributions" -> Json.obj(
        "overseasSchemeProvider" -> Json.arr(Json.obj(
          "providerName" -> "overseas providerName 1 tax ref",
          "providerAddress" -> "overseas address 1",
          "providerCountryCode" -> "ESP",
          "pensionSchemeTaxReference" -> Json.arr("00123456RA", "00123456RB")
        )),
        "shortServiceRefund" -> 1.11,
        "shortServiceRefundTaxPaid" -> 2.22
      )
    ),
    "stateBenefits" -> Json.obj(
      "stateBenefits" -> Json.obj(
        "incapacityBenefit" -> Json.arr(Json.obj(
          "benefitId" -> "a1e8057e-fbbc-47a8-a8b4-78d9f015c934",
          "startDate" -> "2019-11-13",
          "dateIgnored" -> "2019-04-11T16:22:00Z",
          "submittedOn" -> "2020-09-11T17:23:00Z",
          "endDate" -> "2020-08-23",
          "amount" -> 1212.34,
          "taxPaid" -> 22323.23
        )),
        "statePension" -> Json.obj(
          "benefitId" -> "a1e8057e-fbbc-47a8-a8b4-78d9f015c935",
          "startDate" -> "2018-06-03",
          "dateIgnored" -> "2018-09-09T19:23:00Z",
          "submittedOn" -> "2020-08-07T12:23:00Z",
          "endDate" -> "2020-09-13",
          "amount" -> 42323.23,
          "taxPaid" -> 2323.44
        ),
        "statePensionLumpSum" -> Json.obj(
          "benefitId" -> "a1e8057e-fbbc-47a8-a8b4-78d9f015c936",
          "startDate" -> "2019-04-23",
          "dateIgnored" -> "2019-07-08T05:23:00Z",
          "submittedOn" -> "2020-03-13T19:23:00Z",
          "endDate" -> "2020-08-13",
          "amount" -> 45454.23,
          "taxPaid" -> 45432.56
        ),
        "employmentSupportAllowance" -> Json.arr(Json.obj(
          "benefitId" -> "a1e8057e-fbbc-47a8-a8b4-78d9f015c937",
          "startDate" -> "2019-09-23",
          "dateIgnored" -> "2019-09-28T10:23:00Z",
          "submittedOn" -> "2020-11-13T19:23:00Z",
          "endDate" -> "2020-08-23",
          "amount" -> 44545.43,
          "taxPaid" -> 35343.23
        )),
        "jobSeekersAllowance" -> Json.arr(Json.obj(
          "benefitId" -> "a1e8057e-fbbc-47a8-a8b4-78d9f015c938",
          "startDate" -> "2019-09-19",
          "dateIgnored" -> "2019-08-18T13:23:00Z",
          "submittedOn" -> "2020-07-10T18:23:00Z",
          "endDate" -> "2020-09-23",
          "amount" -> 33223.12,
          "taxPaid" -> 44224.56
        )),
        "bereavementAllowance" -> Json.obj(
          "benefitId" -> "a1e8057e-fbbc-47a8-a8b4-78d9f015c939",
          "startDate" -> "2019-05-22",
          "dateIgnored" -> "2020-08-10T12:23:00Z",
          "submittedOn" -> "2020-09-19T19:23:00Z",
          "endDate" -> "2020-09-26",
          "amount" -> 56534.23,
          "taxPaid" -> 34343.57
        ),
        "otherStateBenefits" -> Json.obj(
          "benefitId" -> "a1e8057e-fbbc-47a8-a8b4-78d9f015c940",
          "startDate" -> "2018-09-03",
          "dateIgnored" -> "2020-01-11T15:23:00Z",
          "submittedOn" -> "2020-09-13T15:23:00Z",
          "endDate" -> "2020-06-03",
          "amount" -> 56532.45,
          "taxPaid" -> 5656.89
        )
      ),
      "customerAddedStateBenefits" -> Json.obj(
        "incapacityBenefit" -> Json.arr(Json.obj(
          "benefitId" -> "a1e8057e-fbbc-47a8-a8b4-78d9f015c941",
          "startDate" -> "2018-07-17",
          "submittedOn" -> "2020-11-17T19:23:00Z",
          "endDate" -> "2020-09-23",
          "amount" -> 45646.78,
          "taxPaid" -> 4544.34
        )),
        "statePension" -> Json.obj(
          "benefitId" -> "a1e8057e-fbbc-47a8-a8b4-78d9f015c943",
          "startDate" -> "2018-04-03",
          "submittedOn" -> "2020-06-11T10:23:00Z",
          "endDate" -> "2020-09-13",
          "amount" -> 45642.45,
          "taxPaid" -> 6764.34
        ),
        "statePensionLumpSum" -> Json.obj(
          "benefitId" -> "a1e8057e-fbbc-47a8-a8b4-78d9f015c956",
          "startDate" -> "2019-09-23",
          "submittedOn" -> "2020-06-13T05:29:00Z",
          "endDate" -> "2020-09-26",
          "amount" -> 34322.34,
          "taxPaid" -> 4564.45
        ),
        "employmentSupportAllowance" -> Json.arr(Json.obj(
          "benefitId" -> "a1e8057e-fbbc-47a8-a8b4-78d9f015c988",
          "startDate" -> "2019-09-11",
          "submittedOn" -> "2020-02-10T11:20:00Z",
          "endDate" -> "2020-06-13",
          "amount" -> 45424.23,
          "taxPaid" -> 23232.34
        )),
        "jobSeekersAllowance" -> Json.arr(Json.obj(
          "benefitId" -> "a1e8057e-fbbc-47a8-a8b4-78d9f015c990",
          "startDate" -> "2019-07-10",
          "submittedOn" -> "2020-05-13T14:23:00Z",
          "endDate" -> "2020-05-11",
          "amount" -> 34343.78,
          "taxPaid" -> 3433.56
        )),
        "bereavementAllowance" -> Json.obj(
          "benefitId" -> "a1e8057e-fbbc-47a8-a8b4-78d9f015c997",
          "startDate" -> "2018-08-12",
          "submittedOn" -> "2020-02-13T11:23:00Z",
          "endDate" -> "2020-07-13",
          "amount" -> 45423.45,
          "taxPaid" -> 4543.64
        ),
        "otherStateBenefits" -> Json.obj(
          "benefitId" -> "a1e8057e-fbbc-47a8-a8b4-78d9f015c957",
          "startDate" -> "2018-01-13",
          "submittedOn" -> "2020-09-12T12:23:00Z",
          "endDate" -> "2020-08-13",
          "amount" -> 63333.33,
          "taxPaid" -> 4644.45
        )
      )))

  "PensionsModel" should {
    "parse from Json" in {
      fullJson.as[Pensions] mustBe fullPensionsModel
    }

    "parse to Json" in {
      Json.toJson(fullPensionsModel) mustBe fullJson
    }
  }
}

