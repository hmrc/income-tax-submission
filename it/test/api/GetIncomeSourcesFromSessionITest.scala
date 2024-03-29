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
import builders.models.InterestBuilder.anInterest
import builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import builders.models.gifts.GiftAidBuilder.aGiftAid
import builders.models.pensions.PensionsBuilder.aPensions
import helpers.IntegrationSpec
import models.mongo.{DatabaseError, UserData}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import play.api.http.HeaderNames
import play.api.libs.json.Json
import repositories.IncomeTaxUserDataRepositoryImpl

class GetIncomeSourcesFromSessionITest extends IntegrationSpec with ScalaFutures {

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

  trait Setup {
    implicit val patienceConfig: PatienceConfig = PatienceConfig(Span(5, Seconds))
    val taxYear: String = userData.taxYear.toString
    val successNino: String = userData.nino
    val authorizationHeader: (String, String) = HeaderNames.AUTHORIZATION -> "mock-bearer-token"
    val mtditidHeader: (String, String) = ("mtditid", userData.mtdItId)
    val sessionIdHeader: (String, String) = ("sessionId", userData.sessionId)
    val xSessionIdHeader: (String, String) = ("X-Session-ID", userData.sessionId)
    auditStubs()
    await(repo.collection.drop().toFuture())
    await(repo.ensureIndexes)
    count mustBe 0
  }

  "get income sources from session" when {
    "the user is an individual" must {
      "return the income sources for a user" in new Setup {
        val res: Either[DatabaseError, Unit] = await(repo.update(userData))
        res mustBe Right(())
        count mustBe 1

        authorised()
        whenReady(buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources/session")
          .withQueryStringParameters("taxYear" -> taxYear)
          .withHttpHeaders(mtditidHeader, sessionIdHeader, xSessionIdHeader, authorizationHeader)
          .get()) {
          result =>
            result.status mustBe 200
            Json.parse(result.body) mustBe Json.toJson(userData.toIncomeSourcesResponseModel)
        }
      }

      "return the income sources for a user when the backup session header is available" in new Setup {
        val res: Either[DatabaseError, Unit] = await(repo.update(userData))
        res mustBe Right(())
        count mustBe 1

        authorised()
        whenReady(buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources/session")
          .withQueryStringParameters("taxYear" -> taxYear)
          .withHttpHeaders(mtditidHeader, sessionIdHeader, authorizationHeader)
          .get()) {
          result =>
            result.status mustBe 200
            Json.parse(result.body) mustBe Json.toJson(userData.toIncomeSourcesResponseModel)
        }
      }

      "return 204 if a user has no recorded income sources" in new Setup {
        val res: Either[DatabaseError, Unit] = await(repo.update(userData.copy(dividends = None, interest = None,
          giftAid = None, employment = None, pensions = None)))
        res mustBe Right(())
        count mustBe 1

        authorised()

        whenReady(buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources/session")
          .withQueryStringParameters("taxYear" -> taxYear)
          .withHttpHeaders(mtditidHeader, sessionIdHeader, xSessionIdHeader, authorizationHeader)
          .get()) {
          result =>
            result.status mustBe 204
            result.body mustBe ""
        }
      }

      "return 204 if user does not exist" in new Setup {
        authorised()

        whenReady(buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources/session")
          .withQueryStringParameters("taxYear" -> taxYear)
          .withHttpHeaders(mtditidHeader, sessionIdHeader, xSessionIdHeader, authorizationHeader)
          .get()) {
          result =>
            result.status mustBe 204
            result.body mustBe ""
        }
      }

      "return 401 if the user has no HMRC-MTD-IT enrolment" in new Setup {
        unauthorisedOtherEnrolment()

        whenReady(buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources/session")
          .withQueryStringParameters("taxYear" -> taxYear)
          .withHttpHeaders(mtditidHeader, sessionIdHeader, xSessionIdHeader, authorizationHeader)
          .get()) {
          result =>
            result.status mustBe 401
            result.body mustBe ""
        }
      }

      "return 401 if the request has no MTDITID header present" in new Setup {
        authorised()

        whenReady(buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources/session")
          .withQueryStringParameters("taxYear" -> taxYear)
          .withHttpHeaders(authorizationHeader)
          .get()) {
          result =>
            result.status mustBe 401
            result.body mustBe ""
        }
      }
    }

    "the user is an agent" must {
      "return the income sources for a user" in new Setup {
        val res: Either[DatabaseError, Unit] = await(repo.update(userData))
        res mustBe Right(())
        count mustBe 1

        agentAuthorised()

        whenReady(
          buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources/session")
            .withQueryStringParameters("taxYear" -> taxYear)
            .withHttpHeaders(mtditidHeader, sessionIdHeader, xSessionIdHeader, authorizationHeader)
            .get()
        ) {
          result =>
            result.status mustBe 200
            Json.parse(result.body) mustBe Json.toJson(userData.toIncomeSourcesResponseModel)
        }
      }

      "return 204 if a user has no recorded income sources" in new Setup {
        val res: Either[DatabaseError, Unit] = await(repo.update(userData.copy(dividends = None, interest = None,
          giftAid = None, employment = None, pensions = None)))
        res mustBe Right(())
        count mustBe 1

        agentAuthorised()

        whenReady(
          buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources/session")
            .withQueryStringParameters("taxYear" -> taxYear)
            .withHttpHeaders(mtditidHeader, sessionIdHeader, xSessionIdHeader, authorizationHeader)
            .get()
        ) {
          result =>
            result.status mustBe 204
            result.body mustBe ""
        }
      }

      "return 204 if a user does not exist" in new Setup {

        agentAuthorised()

        whenReady(
          buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources/session")
            .withQueryStringParameters("taxYear" -> taxYear)
            .withHttpHeaders(mtditidHeader, sessionIdHeader, xSessionIdHeader, authorizationHeader)
            .get()
        ) {
          result =>
            result.status mustBe 204
            result.body mustBe ""
        }
      }

      "return 401 if the user has no HMRC-MTD-IT enrolment" in new Setup {
        unauthorisedOtherEnrolment()

        whenReady(
          buildClient(s"/income-tax-submission-service/income-tax/nino/$successNino/sources/session")
            .withQueryStringParameters("taxYear" -> taxYear)
            .withHttpHeaders(mtditidHeader, sessionIdHeader, xSessionIdHeader, authorizationHeader)
            .get()
        ) {
          result =>
            result.status mustBe 401
            result.body mustBe ""
        }
      }
    }
  }
}
