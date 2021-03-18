/*
 * Copyright 2021 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.http.{HttpHeader, HttpHeaders}
import helpers.WiremockSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.play.PlaySpec
import play.api.http.Status._

class GetIncomeSourcesITest extends PlaySpec with WiremockSpec with ScalaFutures {

  trait Setup {
    implicit val patienceConfig: PatienceConfig = PatienceConfig(Span(5, Seconds))
    val successNino: String = "AA123123A"
    val taxYear: String = "2019"
    val agentClientCookie: Map[String, String] = Map("MTDITID" -> "123123123")
    val contentTypeHeader = new HttpHeader("Content-Type", "application/json; charset=utf-8")
    val mtditidHeader = ("mtditid", "123123123")
    auditStubs()
  }

  "get income sources" when {

    "the user is an individual" must {
      "return the income sources for a user" in new Setup {

        stubGetWithResponseBody(
          url = s"/income-tax-dividends/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = OK,
          response = """{"ukDividends": 29320682007.99,"otherUkDividends": 17060389570.99}""")

        stubGetWithResponseBody(
          url = s"/income-tax-dividends/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = OK,
          response = """{"ukDividends": 29320682007.99,"otherUkDividends": 17060389570.99}""")

        stubGetWithResponseBody(
          url = s"/income-tax-interest/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = OK,
          response = """[{"accountName": "someName", "incomeSourceId": "123", "taxedUkInterest": 29320682007.99,"untaxedUkInterest": 17060389570.99}]"""
        )

        authorised()

        whenReady(buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources")
          .withQueryStringParameters("taxYear" -> "2019")
          .withHttpHeaders(mtditidHeader)
          .get) {
          result =>
            result.status mustBe 200
            result.body mustBe
              """{"dividends":{"ukDividends":29320682007.99,"otherUkDividends":17060389570.99},"interest":[{"accountName":"someName","incomeSourceId":"123","taxedUkInterest":29320682007.99,"untaxedUkInterest":17060389570.99}]}"""
        }
      }


      "return 204 if a user has no recorded income sources" in new Setup {
        stubGetWithoutResponseBody(
          url = s"/income-tax-dividends/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = NOT_FOUND)
        stubGetWithoutResponseBody(
          url = s"/income-tax-interest/incom-tax/nino/A123123A/sources\\?taxYear=2019",
          status = NOT_FOUND)

        authorised()

        whenReady(buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources")
          .withQueryStringParameters("taxYear" -> "2019")
          .withHttpHeaders(mtditidHeader)
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
          response = responseBody)

        stubGetWithResponseBody(
          url = s"/income-tax-interest/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = SERVICE_UNAVAILABLE,
          response = responseBody)
        authorised()

        whenReady(buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources")
          .withQueryStringParameters("taxYear" -> "2019")
          .withHttpHeaders(mtditidHeader)
          .get) {
          result =>
            result.status mustBe 503
            result.body mustBe "{\"code\":\"SERVICE_UNAVAILABLE\",\"reason\":\"The service is temporarily unavailable\"}"
        }
      }

      "return 401 if the user has no HMRC-MTD-IT enrolment" in new Setup {
        stubGetWithoutResponseBody(
          url = s"/income-tax-dividends/income-tax/nino/AA123123A/sources\\?taxYear=2019&mtditid=123123123",
          status = SERVICE_UNAVAILABLE)

        stubGetWithoutResponseBody(
          url = s"/income-tax-interest/income-tax/nino/AA123123A/sources\\?taxYear=2019&mtditid=123123123",
          status = SERVICE_UNAVAILABLE)
        unauthorisedOtherEnrolment()

        whenReady(buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources")
          .withQueryStringParameters("taxYear" -> "2019")
          .withHttpHeaders(mtditidHeader)
          .get) {
          result =>
            result.status mustBe 401
            result.body mustBe ""
        }
      }

      "return 401 if the request has no mtditid header present" in new Setup {
        stubGetWithoutResponseBody(
          url = s"/income-tax-dividends/income-tax/nino/AA123123A/sources\\?taxYear=2019&mtditid=123123123",
          status = SERVICE_UNAVAILABLE)

        stubGetWithoutResponseBody(
          url = s"/income-tax-interest/income-tax/nino/AA123123A/sources\\?taxYear=2019&mtditid=123123123",
          status = SERVICE_UNAVAILABLE)
        unauthorisedOtherEnrolment()

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
          response = """{"ukDividends": 29320682007.99,"otherUkDividends": 17060389570.99}""")
        stubGetWithResponseBody(
          url = s"/income-tax-interest/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = OK,
          response = """[{"accountName": "someName", "incomeSourceId": "123", "taxedUkInterest": 29320682007.99,"untaxedUkInterest": 17060389570.99}]""")
        agentAuthorised()

        whenReady(
          buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources", additionalCookies = agentClientCookie)
            .withQueryStringParameters("taxYear" -> "2019")
            .withHttpHeaders(mtditidHeader)
            .get
        ) {
          result =>
            result.status mustBe 200
            result.body mustBe """{"dividends":{"ukDividends":29320682007.99,"otherUkDividends":17060389570.99},"interest":[{"accountName":"someName","incomeSourceId":"123","taxedUkInterest":29320682007.99,"untaxedUkInterest":17060389570.99}]}"""
        }
      }

      "return 204 if a user has no recorded income sources" in new Setup {
        stubGetWithoutResponseBody(
          url = s"/income-tax-dividends/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = NOT_FOUND)
        stubGetWithoutResponseBody(
          url = s"/income-tax-interest/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = NOT_FOUND)
        agentAuthorised()

        whenReady(
          buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources", additionalCookies = agentClientCookie)
            .withQueryStringParameters("taxYear" -> "2019")
            .withHttpHeaders(mtditidHeader)
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
          response = responseBody)
        stubGetWithResponseBody(
          url = s"/income-tax-interest/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = SERVICE_UNAVAILABLE,
          response = responseBody)
        agentAuthorised()

        whenReady(
          buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources", additionalCookies = agentClientCookie)
            .withQueryStringParameters("taxYear" -> "2019")
            .withHttpHeaders(mtditidHeader)
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
          status = SERVICE_UNAVAILABLE)
        stubGetWithoutResponseBody(
          url = s"/income-tax-interest/income-tax/nino/AA123123A/sources\\?taxYear=2019",
          status = SERVICE_UNAVAILABLE)
        unauthorisedOtherEnrolment()

        whenReady(
          buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources", additionalCookies = agentClientCookie)
            .withQueryStringParameters("taxYear" -> "2019", "mtditid" -> "123123123")
            .withHttpHeaders(mtditidHeader)
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
