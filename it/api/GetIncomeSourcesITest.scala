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

package api

import builders.models.DividendsBuilder.aDividends
import builders.models.IncomeSourcesBuilder.anIncomeSources
import builders.models.InterestBuilder.anInterest
import builders.models.SavingsIncomeBuilder.anSavingIncome
import builders.models.cis.AllCISDeductionsBuilder.anAllCISDeductions
import builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import builders.models.gains.GainsBuilder.anGains
import builders.models.gifts.GiftAidBuilder.aGiftAid
import builders.models.pensions.PensionsBuilder.aPensions
import builders.models.statebenefits.AllStateBenefitsDataBuilder.anAllStateBenefitsData
import com.github.tomakehurst.wiremock.http.HttpHeader
import helpers.IntegrationSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.json.Json

class GetIncomeSourcesITest extends IntegrationSpec with ScalaFutures {

  trait Setup {
    implicit val patienceConfig: PatienceConfig = PatienceConfig(Span(5, Seconds))
    val successNino: String = "AA123123A"
    val taxYear: String = "2019"
    val authorizationHeader: (String, String) = HeaderNames.AUTHORIZATION -> "mock-bearer-token"
    val mtditidHeader: (String, String) = ("mtditid", "555555555")
    val sessionIdHeader: (String, String) = ("sessionId", "555555555")
    val requestHeaders: Seq[HttpHeader] = Seq(new HttpHeader("mtditid", "555555555"))
    auditStubs()
  }

  "get income sources" when {
    "the user is an individual" must {
      "return the income sources for a user" in new Setup {
        stubGetWithResponseBody(
          url = s"/income-tax-dividends/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = OK,
          response = Json.toJson(aDividends).toString,
          requestHeaders
        )

        stubGetWithResponseBody(
          url = s"/income-tax-interest/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = OK,
          response = Json.toJson(Seq(anInterest)).toString,
          requestHeaders
        )

        stubGetWithResponseBody(
          url = s"/income-tax-interest/income-tax/nino/AA123123A/savings\\?taxYear=2019",
          status = OK,
          response = Json.toJson(anSavingIncome).toString,
          requestHeaders
        )

        stubGetWithResponseBody(
          url = s"/income-tax-gift-aid/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = OK,
          response = Json.toJson(aGiftAid).toString,
          requestHeaders
        )

        stubGetWithResponseBody(
          url = s"/income-tax-employment/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = OK,
          response = Json.toJson(anAllEmploymentData).toString,
          requestHeaders
        )

        stubGetWithResponseBody(
          url = s"/income-tax-pensions/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = OK,
          response = Json.toJson(aPensions).toString,
          requestHeaders
        )

        stubGetWithResponseBody(
          url = s"/income-tax-cis/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = OK,
          response = Json.toJson(Some(anAllCISDeductions)).toString,
          requestHeaders
        )

        stubGetWithResponseBody(
          url = s"/income-tax-state-benefits/income-tax/nino/AA123123A/tax-year/2019",
          status = OK,
          response = Json.toJson(Some(anAllStateBenefitsData)).toString,
          requestHeaders
        )

        stubGetWithResponseBody(
          url = s"/income-tax/insurance-policies/income/AA123123A/2018-19",
          status = OK,
          response = Json.toJson(anGains).toString,
          requestHeaders
        )

        authorised()

        whenReady(buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources")
          .withQueryStringParameters("taxYear" -> "2019")
          .withHttpHeaders(mtditidHeader, sessionIdHeader, authorizationHeader)
          .get) {
          result =>
            result.status mustBe 200
            result.body mustBe Json.toJson(anIncomeSources).toString
        }
      }

      "return 204 if a user has no recorded income sources" in new Setup {
        stubGetWithoutResponseBody(
          url = s"/income-tax-dividends/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = NOT_FOUND,
          requestHeaders
        )
        stubGetWithoutResponseBody(
          url = s"/income-tax-interest/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = NOT_FOUND,
          requestHeaders
        )
        stubGetWithoutResponseBody(
          url = s"/income-tax-gift-aid/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = NOT_FOUND,
          requestHeaders
        )

        stubGetWithoutResponseBody(
          url = s"/income-tax-employment/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = NO_CONTENT,
          requestHeaders
        )

        stubGetWithoutResponseBody(
          url = s"/income-tax-pensions/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = NO_CONTENT,
          requestHeaders
        )

        stubGetWithoutResponseBody(
          url = s"/income-tax-cis/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = NO_CONTENT,
          requestHeaders
        )

        stubGetWithoutResponseBody(
          url = s"/income-tax-state-benefits/income-tax/nino/AA123123A/tax-year/2019",
          status = NO_CONTENT,
          requestHeaders
        )

        authorised()

        whenReady(buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources")
          .withQueryStringParameters("taxYear" -> "2019")
          .withHttpHeaders(mtditidHeader, sessionIdHeader, authorizationHeader)
          .get) {
          result =>
            result.status mustBe 204
            result.body mustBe ""
        }
      }

      "return 503 if a downstream error occurs" in new Setup {
        val responseBody = "{\"code\":\"SERVICE_UNAVAILABLE\",\"reason\":\"The service is temporarily unavailable\"}"
        stubGetWithResponseBody(
          url = s"/income-tax-dividends/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = SERVICE_UNAVAILABLE,
          response = responseBody,
          requestHeaders
        )

        stubGetWithResponseBody(
          url = s"/income-tax-interest/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = SERVICE_UNAVAILABLE,
          response = responseBody,
          requestHeaders
        )
        authorised()

        whenReady(buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources")
          .withQueryStringParameters("taxYear" -> "2019")
          .withHttpHeaders(mtditidHeader, sessionIdHeader, authorizationHeader)
          .get) {
          result =>
            result.status mustBe 503
            result.body mustBe "{\"code\":\"SERVICE_UNAVAILABLE\",\"reason\":\"The service is temporarily unavailable\"}"
        }
      }

      "return 401 if the user has no HMRC-MTD-IT enrolment" in new Setup {
        unauthorisedOtherEnrolment()

        whenReady(buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources")
          .withQueryStringParameters("taxYear" -> "2019")
          .withHttpHeaders(mtditidHeader, sessionIdHeader, authorizationHeader)
          .get) {
          result =>
            result.status mustBe 401
            result.body mustBe ""
        }
      }

      "return 401 if the request has no MTDITID header present" in new Setup {
        stubGetWithoutResponseBody(
          url = s"/income-tax-dividends/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = SERVICE_UNAVAILABLE,
          requestHeaders
        )

        stubGetWithoutResponseBody(
          url = s"/income-tax-interest/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = SERVICE_UNAVAILABLE,
          requestHeaders
        )
        authorised()

        whenReady(buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources")
          .withQueryStringParameters("taxYear" -> "2019")
          .get) {
          result =>
            result.status mustBe 401
            result.body mustBe ""
        }
      }
    }

    "the user is an agent" must {
      "return the income sources for a user" in new Setup {
        stubGetWithResponseBody(
          url = s"/income-tax-dividends/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = OK,
          response = Json.toJson(aDividends).toString,
          requestHeaders
        )
        stubGetWithResponseBody(
          url = s"/income-tax-interest/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = OK,
          response = Json.toJson(Seq(anInterest)).toString,
          requestHeaders
        )
        stubGetWithResponseBody(
          url = s"/income-tax-interest/income-tax/nino/AA123123A/savings\\?taxYear=2019",
          status = OK,
          response = Json.toJson(anSavingIncome).toString,
          requestHeaders
        )
        stubGetWithResponseBody(
          url = s"/income-tax-gift-aid/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = OK,
          response = Json.toJson(aGiftAid).toString,
          requestHeaders
        )

        stubGetWithResponseBody(
          url = s"/income-tax-employment/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = OK,
          response = Json.toJson(anAllEmploymentData).toString,
          requestHeaders
        )

        stubGetWithResponseBody(
          url = s"/income-tax-pensions/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = OK,
          response = Json.toJson(aPensions).toString,
          requestHeaders
        )

        stubGetWithResponseBody(
          url = s"/income-tax-cis/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = OK,
          response = Json.toJson(Some(anAllCISDeductions)).toString,
          requestHeaders
        )

        stubGetWithResponseBody(
          url = s"/income-tax-state-benefits/income-tax/nino/AA123123A/tax-year/2019",
          status = OK,
          response = Json.toJson(Some(anAllStateBenefitsData)).toString,
          requestHeaders
        )

        stubGetWithResponseBody(
          url = s"/income-tax/insurance-policies/income/AA123123A/2018-19",
          status = OK,
          response = Json.toJson(anGains).toString,
          requestHeaders
        )

        agentAuthorised()

        whenReady(
          buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources")
            .withQueryStringParameters("taxYear" -> "2019")
            .withHttpHeaders(mtditidHeader, sessionIdHeader, authorizationHeader)
            .get
        ) {
          result =>
            result.status mustBe 200
            result.body mustBe Json.toJson(anIncomeSources).toString
        }
      }

      "return 204 if a user has no recorded income sources" in new Setup {
        stubGetWithoutResponseBody(
          url = s"/income-tax-dividends/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = NOT_FOUND,
          requestHeaders
        )
        stubGetWithoutResponseBody(
          url = s"/income-tax-interest/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = NOT_FOUND,
          requestHeaders
        )
        stubGetWithoutResponseBody(
          url = s"/income-tax-gift-aid/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = NOT_FOUND,
          requestHeaders
        )
        stubGetWithoutResponseBody(
          url = s"/income-tax-employment/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = NO_CONTENT,
          requestHeaders
        )
        stubGetWithoutResponseBody(
          url = s"/income-tax-pensions/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = NO_CONTENT,
          requestHeaders
        )
        stubGetWithoutResponseBody(
          url = s"/income-tax-cis/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = NO_CONTENT,
          requestHeaders
        )
        stubGetWithoutResponseBody(
          url = s"/income-tax-state-benefits/income-tax/nino/AA123123A/tax-year/2019",
          status = NO_CONTENT,
          requestHeaders
        )
        agentAuthorised()

        whenReady(
          buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources")
            .withHttpHeaders(mtditidHeader, sessionIdHeader, authorizationHeader)
            .withQueryStringParameters("taxYear" -> "2019")
            .get
        ) {
          result =>
            result.status mustBe 204
            result.body mustBe ""
        }
      }

      "return 503 if a downstream error occurs" in new Setup {
        val responseBody = "{\"code\":\"SERVICE_UNAVAILABLE\",\"reason\":\"The service is temporarily unavailable\"}"
        stubGetWithResponseBody(
          url = s"/income-tax-dividends/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = SERVICE_UNAVAILABLE,
          response = responseBody,
          requestHeaders
        )
        stubGetWithResponseBody(
          url = s"/income-tax-interest/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = SERVICE_UNAVAILABLE,
          response = responseBody,
          requestHeaders
        )
        agentAuthorised()

        whenReady(
          buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources")
            .withQueryStringParameters("taxYear" -> "2019")
            .withHttpHeaders(mtditidHeader, sessionIdHeader, authorizationHeader)
            .get
        ) {
          result =>
            result.status mustBe 503
            result.body mustBe "{\"code\":\"SERVICE_UNAVAILABLE\",\"reason\":\"The service is temporarily unavailable\"}"
        }
      }

      "return 401 if the user has no HMRC-MTD-IT enrolment" in new Setup {
        stubGetWithoutResponseBody(
          url = s"/income-tax-dividends/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = SERVICE_UNAVAILABLE,
          requestHeaders
        )
        stubGetWithoutResponseBody(
          url = s"/income-tax-interest/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = SERVICE_UNAVAILABLE,
          requestHeaders
        )
        unauthorisedOtherEnrolment()

        whenReady(
          buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources")
            .withQueryStringParameters("taxYear" -> "2019")
            .withHttpHeaders(mtditidHeader, sessionIdHeader, authorizationHeader)
            .get
        ) {
          result =>
            result.status mustBe 401
            result.body mustBe ""
        }
      }
    }
  }
}
