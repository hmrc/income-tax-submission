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

package models.employment

import models.otheremployment.LumpSum
import play.api.libs.json.Json
import utils.TestUtils

class OtherEmploymentIncomeSpec extends TestUtils {

  val otherEmploymentJson =
    """
      |{
      |    "submittedOn": "2023-01-04T05:01:01Z",
      |    "shareOption":
      |    [
      |        {
      |            "employerName": "Company Ltd",
      |            "employerRef": "321/AB123",
      |            "schemePlanType": "EMI",
      |            "dateOfOptionGrant": "2019-11-20",
      |            "dateOfEvent": "2019-10-20",
      |            "optionNotExercisedButConsiderationReceived": true,
      |            "amountOfConsiderationReceived": 23445.78,
      |            "noOfSharesAcquired": 1,
      |            "classOfSharesAcquired": "FIRST",
      |            "exercisePrice": 456.56,
      |            "amountPaidForOption": 3555.45,
      |            "marketValueOfSharesOnExcise": 3323.45,
      |            "profitOnOptionExercised": 4532.45,
      |            "employersNicPaid": 234.78,
      |            "taxableAmount": 35345.56
      |        }
      |    ],
      |    "sharesAwardedOrReceived":
      |    [
      |        {
      |            "employerName": "ABC Ltd",
      |            "employerRef": "321/AB156",
      |            "schemePlanType": "SIP",
      |            "dateSharesCeasedToBeSubjectToPlan": "2019-10-20",
      |            "noOfShareSecuritiesAwarded": 2,
      |            "classOfShareAwarded": "FIRST",
      |            "dateSharesAwarded": "2019-09-20",
      |            "sharesSubjectToRestrictions": true,
      |            "electionEnteredIgnoreRestrictions": true,
      |            "actualMarketValueOfSharesOnAward": 35345.67,
      |            "unrestrictedMarketValueOfSharesOnAward": 5643.34,
      |            "amountPaidForSharesOnAward": 4656.45,
      |            "marketValueAfterRestrictionsLifted": 4654.34,
      |            "taxableAmount": 45646.56
      |        }
      |    ],
      |    "lumpSums":
      |    [
      |        {
      |            "employerName": "ABC Ltd",
      |            "employerRef": "321/AB156",
      |            "taxableLumpSumsAndCertainIncome":
      |            {
      |                "amount": 100,
      |                "taxPaid": 23,
      |                "taxTakenOffInEmployment": true
      |            },
      |            "benefitFromEmployerFinancedRetirementScheme":
      |            {
      |                "amount": 300,
      |                "exemptAmount": 100,
      |                "taxPaid": 30,
      |                "taxTakenOffInEmployment": true
      |            },
      |            "redundancyCompensationPaymentsOverExemption":
      |            {
      |                "amount": 200,
      |                "taxPaid": 20,
      |                "taxTakenOffInEmployment": true
      |            },
      |            "redundancyCompensationPaymentsUnderExemption":
      |            {
      |                "amount": 2345.78
      |            }
      |        }
      |    ],
      |    "disability":
      |    {
      |        "customerReference": "Customer Reference",
      |        "amountDeducted": 3455.56
      |    },
      |    "foreignService":
      |    {
      |        "customerReference": "Foreign Customer Reference",
      |        "amountDeducted": 4232.45
      |    }
      |}
      |""".stripMargin

  "OtherEmployments" should {
    "return all the lumpSums from DES" in {
      val employmentDataJsValue = Json.parse(otherEmploymentJson)
      val lumpSumsOpt = (employmentDataJsValue \ "lumpSums").asOpt[List[LumpSum]]
      lumpSumsOpt match {
        case Some(lumpSums) =>
          val lumpSum = lumpSums.head
          lumpSum.employerName mustBe "ABC Ltd"
          lumpSum.employerRef mustBe "321/AB156"
          lumpSum.taxableLumpSumsAndCertainIncome.head.amount mustBe 100
          lumpSum.taxableLumpSumsAndCertainIncome.head.taxPaid.get mustBe 23
          lumpSum.taxableLumpSumsAndCertainIncome.head.taxTakenOffInEmployment.get mustBe true
        case None           => fail("Failed to parse OtherEmploymentIncome")
      }
    }
  }
}
