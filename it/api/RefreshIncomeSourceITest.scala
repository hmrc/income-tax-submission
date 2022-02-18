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

package api

import builders.models.DividendsBuilder.aDividends
import builders.models.InterestBuilder.anInterest
import builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import builders.models.gifts.GiftAidBuilder.aGiftAid
import builders.models.pensions.PensionsBuilder.aPensions
import com.github.tomakehurst.wiremock.http.HttpHeader
import helpers.IntegrationSpec
import models.mongo.{DatabaseError, UserData}
import models.{APIErrorBodyModel, IncomeSources, RefreshIncomeSource}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import repositories.IncomeTaxUserDataRepositoryImpl

class RefreshIncomeSourceITest extends IntegrationSpec with ScalaFutures {

  private val repo: IncomeTaxUserDataRepositoryImpl = app.injector.instanceOf[IncomeTaxUserDataRepositoryImpl]

  private def count = await(repo.collection.countDocuments().toFuture())

  private val userDataTaxYear = 2022
  private val userData: UserData = UserData(
    "sessionId-1618a1e8-4979-41d8-a32e-5ffbe69fac81",
    "555555555",
    "AA123123A",
    userDataTaxYear,
    Some(aDividends),
    Some(Seq(anInterest)),
    Some(aGiftAid),
    Some(anAllEmploymentData),
    Some(aPensions)
  )

  val requestHeaders: Seq[HttpHeader] = Seq(new HttpHeader("mtditid", "555555555"))

  trait Setup {
    implicit val patienceConfig: PatienceConfig = PatienceConfig(Span(5, Seconds))
    val taxYear: String = userData.taxYear.toString
    val successNino: String = userData.nino
    val mtditidHeader: (String, String) = ("mtditid", userData.mtdItId)
    val sessionIdHeader: (String, String) = ("sessionId", userData.sessionId)
    val xSessionIdHeader: (String, String) = ("X-Session-ID", userData.sessionId)
    auditStubs()
    await(repo.collection.drop().toFuture())
    await(repo.ensureIndexes)
    count mustBe 0
  }

  "refresh income source" when {

    "the user is an individual" must {
      "refresh the dividends income source for a user" in new Setup {
        val res: Either[DatabaseError, Unit] = await(repo.update(userData))
        res mustBe Right()
        count mustBe 1

        stubGetWithResponseBody(
          url = s"/income-tax-dividends/income-tax/nino/AA123123A/sources\\?taxYear=2022",
          status = OK,
          response = """{"ukDividends": 444.99,"otherUkDividends": 333.99}""",
          requestHeaders
        )

        authorised()
        whenReady(buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources/session")
          .withQueryStringParameters("taxYear" -> taxYear)
          .withHttpHeaders(mtditidHeader, sessionIdHeader, xSessionIdHeader)
          .put[JsValue](Json.toJson(RefreshIncomeSource("dividends")))) {
          result =>
            result.status mustBe NO_CONTENT
        }

        whenReady(buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources/session")
          .withQueryStringParameters("taxYear" -> taxYear)
          .withHttpHeaders(mtditidHeader, sessionIdHeader, xSessionIdHeader)
          .get) {
          result =>
            result.status mustBe OK

            Json.parse(result.body).as[IncomeSources].dividends.get.ukDividends mustBe Some(444.99)
            Json.parse(result.body).as[IncomeSources].dividends.get.otherUkDividends mustBe Some(333.99)
        }
      }

      "refresh the dividends income source for a user when there was no dividends to start with" in new Setup {
        val res: Either[DatabaseError, Unit] = await(repo.update(userData.copy(dividends = None)))
        res mustBe Right()
        count mustBe 1

        stubGetWithResponseBody(
          url = s"/income-tax-dividends/income-tax/nino/AA123123A/sources\\?taxYear=2022",
          status = OK,
          response = """{"ukDividends": 444.99,"otherUkDividends": 333.99}""",
          requestHeaders
        )

        authorised()
        whenReady(buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources/session")
          .withQueryStringParameters("taxYear" -> taxYear)
          .withHttpHeaders(mtditidHeader, sessionIdHeader, xSessionIdHeader)
          .put[JsValue](Json.toJson(RefreshIncomeSource("dividends")))) {
          result =>
            result.status mustBe NO_CONTENT
        }

        whenReady(buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources/session")
          .withQueryStringParameters("taxYear" -> taxYear)
          .withHttpHeaders(mtditidHeader, sessionIdHeader, xSessionIdHeader)
          .get) {
          result =>
            result.status mustBe OK

            Json.parse(result.body).as[IncomeSources].dividends.get.ukDividends mustBe Some(444.99)
            Json.parse(result.body).as[IncomeSources].dividends.get.otherUkDividends mustBe Some(333.99)
        }
      }

      "refresh the dividends income source for a user when there was no income source data to start with" in new Setup {
        val res: Either[DatabaseError, Unit] = await(repo.update(userData.copy(dividends = None, interest = None, giftAid = None, employment = None)))
        res mustBe Right()
        count mustBe 1

        stubGetWithResponseBody(
          url = s"/income-tax-dividends/income-tax/nino/AA123123A/sources\\?taxYear=2022",
          status = OK,
          response = """{"ukDividends": 444.99,"otherUkDividends": 333.99}""",
          requestHeaders
        )

        authorised()
        whenReady(buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources/session")
          .withQueryStringParameters("taxYear" -> taxYear)
          .withHttpHeaders(mtditidHeader, sessionIdHeader, xSessionIdHeader)
          .put[JsValue](Json.toJson(RefreshIncomeSource("dividends")))) {
          result =>
            result.status mustBe NO_CONTENT
        }

        whenReady(buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources/session")
          .withQueryStringParameters("taxYear" -> taxYear)
          .withHttpHeaders(mtditidHeader, sessionIdHeader, xSessionIdHeader)
          .get) {
          result =>
            result.status mustBe OK

            Json.parse(result.body).as[IncomeSources].dividends.get.ukDividends mustBe Some(444.99)
            Json.parse(result.body).as[IncomeSources].dividends.get.otherUkDividends mustBe Some(333.99)
        }
      }

      "refresh the dividends income source for a user when there was no data to start with" in new Setup {
        count mustBe 0

        stubGetWithResponseBody(
          url = s"/income-tax-dividends/income-tax/nino/AA123123A/sources\\?taxYear=2022",
          status = OK,
          response = """{"ukDividends": 444.99,"otherUkDividends": 333.99}""",
          requestHeaders
        )

        authorised()
        whenReady(buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources/session")
          .withQueryStringParameters("taxYear" -> taxYear)
          .withHttpHeaders(mtditidHeader, sessionIdHeader, xSessionIdHeader)
          .put[JsValue](Json.toJson(RefreshIncomeSource("dividends")))) {
          result =>
            result.status mustBe NO_CONTENT
        }

        whenReady(buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources/session")
          .withQueryStringParameters("taxYear" -> taxYear)
          .withHttpHeaders(mtditidHeader, sessionIdHeader, xSessionIdHeader)
          .get) {
          result =>
            result.status mustBe OK

            Json.parse(result.body).as[IncomeSources].dividends.get.ukDividends mustBe Some(444.99)
            Json.parse(result.body).as[IncomeSources].dividends.get.otherUkDividends mustBe Some(333.99)
        }
      }

      "return a not found when the user has no income source data and no dividends retrieved" in new Setup {
        val res: Either[DatabaseError, Unit] = await(repo.update(userData.copy(dividends = None, interest = None,
          employment = None, giftAid = None, pensions = None)))
        res mustBe Right()
        count mustBe 1

        stubGetWithResponseBody(
          url = s"/income-tax-dividends/income-tax/nino/AA123123A/sources\\?taxYear=2022",
          status = NOT_FOUND,
          response = """{}""",
          requestHeaders
        )

        authorised()
        whenReady(buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources/session")
          .withQueryStringParameters("taxYear" -> taxYear)
          .withHttpHeaders(mtditidHeader, sessionIdHeader, xSessionIdHeader)
          .put[JsValue](Json.toJson(RefreshIncomeSource("dividends")))) {
          result =>
            result.status mustBe NOT_FOUND
        }

        whenReady(buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources/session")
          .withQueryStringParameters("taxYear" -> taxYear)
          .withHttpHeaders(mtditidHeader, sessionIdHeader, xSessionIdHeader)
          .get) {
          result =>
            result.status mustBe NO_CONTENT
        }
      }

      "return a bad request invalid parameter" in new Setup {
        authorised()
        whenReady(buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources/session")
          .withQueryStringParameters("taxYear" -> taxYear)
          .withHttpHeaders(mtditidHeader, sessionIdHeader, xSessionIdHeader)
          .put[JsValue](Json.toJson(RefreshIncomeSource("invalid-parameter")))) {
          result =>
            result.status mustBe BAD_REQUEST
            result.json mustBe Json.toJson(APIErrorBodyModel("INVALID_INCOME_SOURCE_PARAMETER", "Invalid income source value."))
        }
      }

      "refresh the dividends income source for a user when there are no dividends after the update" in new Setup {
        val res: Either[DatabaseError, Unit] = await(repo.update(userData))
        res mustBe Right()
        count mustBe 1

        stubGetWithResponseBody(
          url = s"/income-tax-dividends/income-tax/nino/AA123123A/sources\\?taxYear=2022",
          status = NOT_FOUND,
          response = """{}""",
          requestHeaders
        )

        authorised()
        whenReady(buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources/session")
          .withQueryStringParameters("taxYear" -> taxYear)
          .withHttpHeaders(mtditidHeader, sessionIdHeader, xSessionIdHeader)
          .put[JsValue](Json.toJson(RefreshIncomeSource("dividends")))) {
          result =>
            result.status mustBe NO_CONTENT
        }

        whenReady(buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources/session")
          .withQueryStringParameters("taxYear" -> taxYear)
          .withHttpHeaders(mtditidHeader, sessionIdHeader, xSessionIdHeader)
          .get) {
          result =>
            result.status mustBe OK

            Json.parse(result.body).as[IncomeSources].dividends mustBe None
        }
      }

      "return UNAUTHORIZED if the user has no HMRC-MTD-IT enrolment" in new Setup {
        unauthorisedOtherEnrolment()

        whenReady(buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources/session")
          .withQueryStringParameters("taxYear" -> taxYear)
          .withHttpHeaders(mtditidHeader, sessionIdHeader, xSessionIdHeader)
          .put[JsValue](Json.toJson(RefreshIncomeSource("dividends")))) {
          result =>
            result.status mustBe UNAUTHORIZED
            result.body mustBe ""
        }
      }

      "return UNAUTHORIZED if the request has no MTDITID header present" in new Setup {
        authorised()

        whenReady(buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources/session")
          .withQueryStringParameters("taxYear" -> taxYear)
          .put[JsValue](Json.toJson(RefreshIncomeSource("dividends")))) {
          result =>
            result.status mustBe UNAUTHORIZED
            result.body mustBe ""
        }
      }
    }

    "the user is an agent" must {
      "refresh the dividends income source for a user" in new Setup {
        val res: Either[DatabaseError, Unit] = await(repo.update(userData))
        res mustBe Right()
        count mustBe 1

        agentAuthorised()

        stubGetWithResponseBody(
          url = s"/income-tax-dividends/income-tax/nino/AA123123A/sources\\?taxYear=2022",
          status = OK,
          response = """{"ukDividends": 444.99,"otherUkDividends": 333.99}""",
          requestHeaders
        )

        whenReady(buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources/session")
          .withQueryStringParameters("taxYear" -> taxYear)
          .withHttpHeaders(mtditidHeader, sessionIdHeader, xSessionIdHeader)
          .put[JsValue](Json.toJson(RefreshIncomeSource("dividends")))) {
          result =>
            result.status mustBe NO_CONTENT
        }

        whenReady(buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources/session")
          .withQueryStringParameters("taxYear" -> taxYear)
          .withHttpHeaders(mtditidHeader, sessionIdHeader, xSessionIdHeader)
          .get) {
          result =>
            result.status mustBe OK

            Json.parse(result.body).as[IncomeSources].dividends.get.ukDividends mustBe Some(444.99)
            Json.parse(result.body).as[IncomeSources].dividends.get.otherUkDividends mustBe Some(333.99)
        }
      }

      "return UNAUTHORIZED if the user has no HMRC-MTD-IT enrolment" in new Setup {
        unauthorisedOtherEnrolment()

        whenReady(
          buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources/session")
            .withQueryStringParameters("taxYear" -> taxYear)
            .withHttpHeaders(mtditidHeader, sessionIdHeader, xSessionIdHeader)
            .put[JsValue](Json.toJson(RefreshIncomeSource("dividends")))) {
          result =>
            result.status mustBe UNAUTHORIZED
            result.body mustBe ""
        }
      }
    }
  }
}
