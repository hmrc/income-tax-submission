/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package api

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
    auditStubs()
  }

  "get income sources" when {

    "the user is an individual" must {
      "return the income sources for a user" in new Setup {
        stubGetWithResponseBody(s"/income-tax-dividends/income-tax/nino/AA123123A/sources\\?taxYear=2019&mtditid=123123123", OK,
          """{"ukDividends": 29320682007.99,"otherUkDividends": 17060389570.99}""")

        authorised()

        whenReady(buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources")
          .withQueryStringParameters("taxYear" -> "2019", "mtditid" -> "123123123").get) {
          result =>
            result.status mustBe 200
            result.body mustBe """{"dividends":{"ukDividends":29320682007.99,"otherUkDividends":17060389570.99}}"""
        }
      }

      "return 204 if a user has no recorded income sources" in new Setup {
        stubGetWithoutResponseBody(s"/income-tax-dividends/income-tax/nino/AA123123A/sources\\?taxYear=2019&mtditid=123123123", NOT_FOUND)

        authorised()

        whenReady(buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources")
          .withQueryStringParameters("taxYear" -> "2019", "mtditid" -> "123123123").get) {
          result =>
            result.status mustBe 204
            result.body mustBe ""
        }
      }

      "return 503 if a downstream error occurs" in new Setup {
        stubGetWithoutResponseBody(s"/income-tax-dividends/income-tax/nino/AA123123A/sources\\?taxYear=2019&mtditid=123123123", SERVICE_UNAVAILABLE)

        authorised()

        whenReady(buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources")
          .withQueryStringParameters("taxYear" -> "2019", "mtditid" -> "123123123").get) {
          result =>
            result.status mustBe 503
            result.body mustBe "{\"code\":\"SERVICE_UNAVAILABLE\",\"message\":\"The service is temporarily unavailable\"}"
        }
      }

      "return 401 if the user has no HMRC-MTD-IT enrolment" in new Setup {
        stubGetWithoutResponseBody(s"/income-tax-dividends/income-tax/nino/AA123123A/sources\\?taxYear=2019&mtditid=123123123", SERVICE_UNAVAILABLE)

        unauthorisedOtherEnrolment()

        whenReady(buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources")
          .withQueryStringParameters("taxYear" -> "2019", "mtditid" -> "123123123").get) {
          result =>
            result.status mustBe 401
            result.body mustBe ""
        }
      }
    }


    "the user is an agent" must {
      "return the income sources for a user" in new Setup {
        stubGetWithResponseBody(s"/income-tax-dividends/income-tax/nino/AA123123A/sources\\?taxYear=2019&mtditid=123123123", OK,
          """{"ukDividends": 29320682007.99,"otherUkDividends": 17060389570.99}""")

        agentAuthorised()

        whenReady(
          buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources", additionalCookies = agentClientCookie)
          .withQueryStringParameters("taxYear" -> "2019", "mtditid" -> "123123123").get
        ) {
          result =>
            result.status mustBe 200
            result.body mustBe """{"dividends":{"ukDividends":29320682007.99,"otherUkDividends":17060389570.99}}"""
        }
      }

      "return 204 if a user has no recorded income sources" in new Setup {
        stubGetWithoutResponseBody(s"/income-tax-dividends/income-tax/nino/AA123123A/sources\\?taxYear=2019&mtditid=123123123", NOT_FOUND)

        agentAuthorised()

        whenReady(
          buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources", additionalCookies = agentClientCookie)
            .withQueryStringParameters("taxYear" -> "2019", "mtditid" -> "123123123").get
        ) {
          result =>
            result.status mustBe 204
            result.body mustBe ""
        }
      }

      "return 503 if a downstream error occurs" in new Setup {
        stubGetWithoutResponseBody(s"/income-tax-dividends/income-tax/nino/AA123123A/sources\\?taxYear=2019&mtditid=123123123", SERVICE_UNAVAILABLE)

        agentAuthorised()

        whenReady(
          buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources", additionalCookies = agentClientCookie)
            .withQueryStringParameters("taxYear" -> "2019", "mtditid" -> "123123123").get
        ) {
          result =>
            result.status mustBe 503
            result.body mustBe "{\"code\":\"SERVICE_UNAVAILABLE\",\"message\":\"The service is temporarily unavailable\"}"
        }
      }

      "return 401 if the user has no HMRC-MTD-IT enrolment" in new Setup {
        stubGetWithoutResponseBody(s"/income-tax-dividends/income-tax/nino/AA123123A/sources\\?taxYear=2019&mtditid=123123123", SERVICE_UNAVAILABLE)

        unauthorisedOtherEnrolment()

        whenReady(
          buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources", additionalCookies = agentClientCookie)
          .withQueryStringParameters("taxYear" -> "2019", "mtditid" -> "123123123").get
        ) {
          result =>
            result.status mustBe 401
            result.body mustBe ""
        }
      }
    }
  }
}
