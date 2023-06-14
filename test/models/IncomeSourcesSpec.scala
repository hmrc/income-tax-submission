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

import builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import com.codahale.metrics.SharedMetricRegistries
import models.gifts.{GiftAid, GiftAidPayments, Gifts}
import play.api.libs.json.{JsObject, Json}
import utils.TestUtils

class IncomeSourcesSpec extends TestUtils {
  SharedMetricRegistries.clear()

  val giftAidPayments: GiftAidPayments = GiftAidPayments(Some(List("non uk charity name", "non uk charity name 2")), Some(12345.67), Some(12345.67), Some(12345.67), Some(12345.67), Some(12345.67))
  val gifts: Gifts = Gifts(Some(List("charity name")), Some(12345.67), Some(12345.67), Some(12345.67))

  private val underTest = IncomeSources(None, Some(Dividends(Some(123456.78), Some(123456.78))), Some(Seq(Interest("someName", "12345", Some(12345.67), Some(12345.67)))), Some(GiftAid(Some(giftAidPayments), Some(gifts))), Some(anAllEmploymentData), otherEmploymentIncome = None)

  val jsonModel: JsObject = Json.obj("dividends" ->
    Json.obj(
      "ukDividends" -> 123456.78,
      "otherUkDividends" -> 123456.78
    ),
    "interest" ->
      Seq(Json.obj(
        "accountName" -> "someName",
        "incomeSourceId" -> "12345",
        "taxedUkInterest" -> 12345.67,
        "untaxedUkInterest" -> 12345.67
      )
      ), "giftAid" -> Json.obj(
      "giftAidPayments" -> Json.obj(
        "nonUkCharitiesCharityNames" -> Json.arr(
          "non uk charity name",
          "non uk charity name 2"
        ),
        "currentYear" -> 12345.67,
        "oneOffCurrentYear" -> 12345.67,
        "currentYearTreatedAsPreviousYear" -> 12345.67,
        "nextYearTreatedAsCurrentYear" -> 12345.67,
        "nonUkCharities" -> 12345.67
      ),
      "gifts" -> Json.obj(
        "investmentsNonUkCharitiesCharityNames" -> Json.arr(
          "charity name"
        ),
        "landAndBuildings" -> 12345.67,
        "sharesOrSecurities" -> 12345.67,
        "investmentsNonUkCharities" -> 12345.67
      )),
    "employment" -> Json.obj(
      "hmrcEmploymentData" -> Json.arr(
        Json.obj(
          "employmentId" -> "00000000-0000-0000-1111-000000000000",
          "employerName" -> "default-employer",
          "employerRef" -> "666/66666",
          "payrollId" -> "1234567890",
          "startDate" -> "2020-01-01",
          "cessationDate" -> "2020-01-01",
          "submittedOn" -> "2020-01-04T05:01:01Z",
          "hmrcEmploymentFinancialData" -> Json.obj(
          "employmentData" -> Json.obj(
            "submittedOn" -> "2020-01-04T05:01:01Z",
            "employmentSequenceNumber" -> "1002",
            "companyDirector" -> false,
            "closeCompany" -> true,
            "directorshipCeasedDate" -> "2020-02-12",
            "occPen" -> false,
            "disguisedRemuneration" -> false,
            "pay" -> Json.obj(
              "taxablePayToDate" -> 100.00,
              "totalTaxToDate" -> 200.00,
              "payFrequency" -> "CALENDAR MONTHLY",
              "paymentDate" -> "2020-04-23",
              "taxWeekNo" -> 1,
              "taxMonthNo" -> 1
            ),
            "deductions" -> Json.obj(
              "studentLoans" -> Json.obj(
                "uglDeductionAmount" -> 100,
                "pglDeductionAmount" -> 200
              )
            )
          ),
          "employmentBenefits" -> Json.obj(
            "submittedOn" -> "2020-01-04T05:01:01Z",
            "benefits" -> Json.obj(
              "accommodation" -> 6.00,
              "assets" -> 27.00,
              "assetTransfer" -> 280000.00,
              "beneficialLoan" -> 18.00,
              "car" -> 1.23,
              "carFuel" -> 2.00,
              "educationalServices" -> 19.00,
              "entertaining" -> 11.00,
              "expenses" -> 22.00,
              "medicalInsurance" -> 16.00,
              "telephone" -> 12.00,
              "service" -> 15.00,
              "taxableExpenses" -> 23.00,
              "van" -> 3.00,
              "vanFuel" -> 4.00,
              "mileage" -> 5.00,
              "nonQualifyingRelocationExpenses" -> 8.00,
              "nurseryPlaces" -> 17.00,
              "otherItems" -> 26.00,
              "paymentsOnEmployeesBehalf" -> 21.00,
              "personalIncidentalExpenses" -> 10.00,
              "qualifyingRelocationExpenses" -> 7.00,
              "employerProvidedProfessionalSubscriptions" -> 14.00,
              "employerProvidedServices" -> 13.00,
              "incomeTaxPaidByDirector" -> 20.00,
              "travelAndSubsistence" -> 9.00,
              "vouchersAndCreditCards" -> 24.00,
              "nonCash" -> 25.00
            )
          )
          ),
          "customerEmploymentFinancialData" -> Json.obj(
            "employmentData" -> Json.obj(
              "submittedOn" -> "2020-01-04T05:01:01Z",
              "employmentSequenceNumber" -> "1002",
              "companyDirector" -> false,
              "closeCompany" -> true,
              "directorshipCeasedDate" -> "2020-02-12",
              "occPen" -> false,
              "disguisedRemuneration" -> false,
              "pay" -> Json.obj(
                "taxablePayToDate" -> 100.00,
                "totalTaxToDate" -> 200.00,
                "payFrequency" -> "CALENDAR MONTHLY",
                "paymentDate" -> "2020-04-23",
                "taxWeekNo" -> 1,
                "taxMonthNo" -> 1
              ),
              "deductions" -> Json.obj(
                "studentLoans" -> Json.obj(
                  "uglDeductionAmount" -> 100,
                  "pglDeductionAmount" -> 200
                )
              )
            ),
            "employmentBenefits" -> Json.obj(
              "submittedOn" -> "2020-01-04T05:01:01Z",
              "benefits" -> Json.obj(
                "accommodation" -> 6.00,
                "assets" -> 27.00,
                "assetTransfer" -> 280000.00,
                "beneficialLoan" -> 18.00,
                "car" -> 1.23,
                "carFuel" -> 2.00,
                "educationalServices" -> 19.00,
                "entertaining" -> 11.00,
                "expenses" -> 22.00,
                "medicalInsurance" -> 16.00,
                "telephone" -> 12.00,
                "service" -> 15.00,
                "taxableExpenses" -> 23.00,
                "van" -> 3.00,
                "vanFuel" -> 4.00,
                "mileage" -> 5.00,
                "nonQualifyingRelocationExpenses" -> 8.00,
                "nurseryPlaces" -> 17.00,
                "otherItems" -> 26.00,
                "paymentsOnEmployeesBehalf" -> 21.00,
                "personalIncidentalExpenses" -> 10.00,
                "qualifyingRelocationExpenses" -> 7.00,
                "employerProvidedProfessionalSubscriptions" -> 14.00,
                "employerProvidedServices" -> 13.00,
                "incomeTaxPaidByDirector" -> 20.00,
                "travelAndSubsistence" -> 9.00,
                "vouchersAndCreditCards" -> 24.00,
                "nonCash" -> 25.00
              )
            )
          )
        )),
      "hmrcExpenses" -> Json.obj(
        "submittedOn" -> "2020-01-04T05:01:01Z",
        "dateIgnored" -> "2020-01-04T05:01:01Z",
        "totalExpenses" -> 800,
        "expenses" -> Json.obj(
          "businessTravelCosts" -> 100,
          "jobExpenses" -> 200,
          "flatRateJobExpenses" -> 300,
          "professionalSubscriptions" -> 400,
          "hotelAndMealExpenses" -> 500,
          "otherAndCapitalAllowances" -> 600,
          "vehicleExpenses" -> 700,
          "mileageAllowanceRelief" -> 800
        )
      ),
      "customerEmploymentData" -> Json.arr(
        Json.obj(
          "employmentId" -> "00000000-0000-0000-1111-000000000000",
          "employerName" -> "default-employer",
          "employerRef" -> "666/66666",
          "payrollId" -> "1234567890",
          "startDate" -> "2020-01-01",
          "cessationDate" -> "2020-01-01",
          "submittedOn" -> "2020-01-04T05:01:01Z",
          "employmentData" -> Json.obj(
            "submittedOn" -> "2020-01-04T05:01:01Z",
            "employmentSequenceNumber" -> "1002",
            "companyDirector" -> false,
            "closeCompany" -> true,
            "directorshipCeasedDate" -> "2020-02-12",
            "occPen" -> false,
            "disguisedRemuneration" -> false,
            "pay" -> Json.obj(
              "taxablePayToDate" -> 100.0,
              "totalTaxToDate" -> 200.0,
              "payFrequency" -> "CALENDAR MONTHLY",
              "paymentDate" -> "2020-04-23",
              "taxWeekNo" -> 1,
              "taxMonthNo" -> 1
            ),
            "deductions" -> Json.obj(
              "studentLoans" -> Json.obj(
                "uglDeductionAmount" -> 100,
                "pglDeductionAmount" -> 200
              )
            )
          ),
          "employmentBenefits" -> Json.obj(
            "submittedOn" -> "2020-01-04T05:01:01Z",
            "benefits" -> Json.obj(
              "accommodation" -> 6.00,
              "assets" -> 27.00,
              "assetTransfer" -> 280000.00,
              "beneficialLoan" -> 18.00,
              "car" -> 1.23,
              "carFuel" -> 2.00,
              "educationalServices" -> 19.00,
              "entertaining" -> 11.00,
              "expenses" -> 22.00,
              "medicalInsurance" -> 16.00,
              "telephone" -> 12.00,
              "service" -> 15.00,
              "taxableExpenses" -> 23.00,
              "van" -> 3.00,
              "vanFuel" -> 4.00,
              "mileage" -> 5.00,
              "nonQualifyingRelocationExpenses" -> 8.00,
              "nurseryPlaces" -> 17.00,
              "otherItems" -> 26.00,
              "paymentsOnEmployeesBehalf" -> 21.00,
              "personalIncidentalExpenses" -> 10.00,
              "qualifyingRelocationExpenses" -> 7.00,
              "employerProvidedProfessionalSubscriptions" -> 14.00,
              "employerProvidedServices" -> 13.00,
              "incomeTaxPaidByDirector" -> 20.00,
              "travelAndSubsistence" -> 9.00,
              "vouchersAndCreditCards" -> 24.00,
              "nonCash" -> 25.00
            )
          )
        )),
      "customerExpenses" -> Json.obj(
        "submittedOn" -> "2020-01-04T05:01:01Z",
        "dateIgnored" -> "2020-01-04T05:01:01Z",
        "totalExpenses" -> 800,
        "expenses" -> Json.obj(
          "businessTravelCosts" -> 100,
          "jobExpenses" -> 200,
          "flatRateJobExpenses" -> 300,
          "professionalSubscriptions" -> 400,
          "hotelAndMealExpenses" -> 500,
          "otherAndCapitalAllowances" -> 600,
          "vehicleExpenses" -> 700,
          "mileageAllowanceRelief" -> 800
        )
      )))

  "IncomeSourcesResponseModel" should {
    "parse to Json" in {
      Json.toJson(underTest) mustBe jsonModel
    }

    "parse from Json" in {
      jsonModel.as[IncomeSources]
    }
  }

}
