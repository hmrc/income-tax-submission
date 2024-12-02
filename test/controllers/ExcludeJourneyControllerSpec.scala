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

package controllers

import common.IncomeSources._
import controllers.predicates.AuthorisedAction
import models.mongo.{DatabaseError, ExclusionUserDataModel, MongoError}
import models._
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NO_CONTENT, OK}
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import services.ExcludeJourneyService
import uk.gov.hmrc.http.HeaderCarrier
import utils.{MockAppConfig, TestUtils}

import java.time.Instant
import scala.concurrent.Future

class ExcludeJourneyControllerSpec extends TestUtils {

  private val mtditid = "1234567890"
  private val nino = "AA000000A"
  private val sessionId = "1234567890987654321"

  implicit val user: User[AnyContentAsEmpty.type] = User(mtditid, None, nino, sessionId)

  private val taxYear = 2023
  private lazy val mockService = mock[ExcludeJourneyService]

  private lazy val auth = new AuthorisedAction()(mockAuthConnector, defaultActionBuilder, new MockAppConfig(), mockControllerComponents)

  private lazy val controller = new ExcludeJourneyController(
    mockControllerComponents,
    auth,
    mockService
  )

  private def mockFind(result: Either[DatabaseError, Option[ExclusionUserDataModel]]) = {
    (mockService.findExclusionData(_: Int)(_: User[_]))
      .expects(*, *)
      .returning(Future.successful(result))
  }

  private def mockCreateOrUpdate(result: Either[DatabaseError, Boolean]) = {
    (mockService.createOrUpdate(_: ExcludeJourneyModel, _: ExclusionUserDataModel, _: Boolean)(_: User[_]))
      .expects(*, *, *, *)
      .returning(Future.successful(result))
  }

  private def mockCreateOrUpdateMultiple(expectedInput: ExclusionUserDataModel, result: Either[DatabaseError, Boolean]) = {
    (mockService.createOrUpdate(_: ExclusionUserDataModel, _: Boolean)(_: User[_]))
      .expects(expectedInput, *, *)
      .returning(Future.successful(result))
  }

  private def mockModelGeneration(journeyKey: String, result: Option[Either[APIErrorModel, ExcludeJourneyModel]] = None) = {
    val resultModel = result.getOrElse(Right(ExcludeJourneyModel(
      journeyKey, None
    )))

    (mockService.journeyKeyToModel(_: Int, _: String)(_: User[_], _: HeaderCarrier))
      .expects(*, *, *, *)
      .returning(Future.successful(resultModel))
  }

  ".getExclusions" must {

    "return a list of excluded journeys" which {
      lazy val request = FakeRequest("POST", s"/income-tax-submission-service/income-tax/nino/AA000000A/sources/exclude-journey/$taxYear")
        .withHeaders("mtditid" -> "1234567890", "sessionId" -> "sessionId")

      lazy val userData = ExclusionUserDataModel(nino, taxYear, Seq(
        ExcludeJourneyModel(INTEREST, None),
        ExcludeJourneyModel(CIS, None)
      ))

      lazy val result = {
        mockAuth()
        mockFind(Right(Some(userData)))
        controller.getExclusions(taxYear, nino)(request)
      }

      "has a status of OK(200)" in {
        status(result) mustBe OK
      }

      "contain the data in the body" in {
        bodyOf(result) mustBe GetExclusionsDataModel(userData.exclusionModel).toJson.toString
      }
    }

    "return an internal server error" when {

      "there is an error accessing the database" in {
        lazy val request = FakeRequest("POST", s"/income-tax-submission-service/income-tax/nino/AA000000A/sources/exclude-journey/$taxYear")
          .withHeaders("mtditid" -> "1234567890", "sessionId" -> "sessionId")

        lazy val result = {
          mockAuth()
          mockFind(Left(MongoError("uh oh")))
          controller.getExclusions(taxYear, nino)(request)
        }

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

    }

  }

  ".handleKey" must {

    "return a NoContent" when {

      "data is successfully created or updated" in {
        lazy val dataModel = ExclusionUserDataModel(
          nino, taxYear, Seq(
            ExcludeJourneyModel(INTEREST, None)
          )
        )

        lazy val result = {
          mockFind(Right(Some(dataModel)))
          mockModelGeneration(INTEREST)
          mockCreateOrUpdate(Right(true))
          await(controller.handleKey(taxYear, INTEREST))
        }

        result.header.status mustBe NO_CONTENT
      }

    }

    "return an InternalServerError" when {

      "journeyToKey returns an error" in {
        val data = ExclusionUserDataModel(
          nino, taxYear, Seq(ExcludeJourneyModel(INTEREST, None))
        )

        val result = {
          mockFind(Right(Some(data)))
          mockModelGeneration(INTEREST, Some(Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("NO", "WAY")))))
          await(controller.handleKey(taxYear, INTEREST))
        }

        result.header.status mustBe INTERNAL_SERVER_ERROR
      }

      "an error is returned when trying to find data" in {
        val result = {
          mockFind(Left(MongoError("nah bud")))
          await(controller.handleKey(taxYear, INTEREST))
        }

        result.header.status mustBe INTERNAL_SERVER_ERROR
      }

    }

  }

  ".excludeJourney" must {

    "return a NoContent" when {

      controller.allJourneys.foreach { key =>
        s"the journey key is $key" in {
          lazy val request = FakeRequest("POST", s"/income-tax-submission-service/income-tax/nino/AA000000A/sources/exclude-journey/$taxYear")
            .withHeaders("mtditid" -> "1234567890", "sessionId" -> "sessionId")
            .withJsonBody(Json.obj("journey" -> key))

          lazy val result = {
            mockAuth()
            mockFind(Right(None))
            mockModelGeneration(key, Some(Right(ExcludeJourneyModel(key, None))))
            mockCreateOrUpdate(Right(true))

            await(controller.excludeJourney(taxYear, nino)(request))
          }

          result.header.status mustBe NO_CONTENT
        }
      }

    }

    "return a BadRequest" when {

      "The body is not valid json" which {
        lazy val request = FakeRequest("POST", s"/income-tax-submission-service/income-tax/nino/AA000000A/sources/exclude-journey/$taxYear")
          .withHeaders("mtditid" -> "1234567890", "sessionId" -> "sessionId")

        lazy val result = {
          mockAuth()
          controller.excludeJourney(taxYear, nino)(request)
        }

        "has the body of 'Invalid Body'" in {
          bodyOf(result) mustBe "Invalid Body"
        }

        "has the status of BAD_REQUEST(400)" in {
          status(result) mustBe BAD_REQUEST
        }
      }

      "The json is valid, but the wrong data" which {
        lazy val request = FakeRequest("POST", s"/income-tax-submission-service/income-tax/nino/AA000000A/sources/exclude-journey/$taxYear")
          .withHeaders("mtditid" -> "1234567890", "sessionId" -> "sessionId")
          .withJsonBody(Json.obj("notAJourney" -> INTEREST))

        lazy val result = {
          mockAuth()
          controller.excludeJourney(taxYear, nino)(request)
        }

        "has the body of 'Incorrect Json Body'" in {
          bodyOf(result) mustBe "Incorrect Json Body"
        }

        "has the status of BAD_REQUEST(400)" in {
          status(result) mustBe BAD_REQUEST
        }
      }

      "The journey key is not valid" which {
        lazy val request = FakeRequest("POST", s"/income-tax-submission-service/income-tax/nino/AA000000A/sources/exclude-journey/$taxYear")
          .withHeaders("mtditid" -> "1234567890", "sessionId" -> "sessionId")
          .withJsonBody(Json.obj("journey" -> "NotAJourneyKey"))

        lazy val result = {
          mockAuth()
          controller.excludeJourney(taxYear, nino)(request)
        }

        "has a status of BAD_REQUEST(400)" in {
          status(result) mustBe BAD_REQUEST
        }

        "has the body of 'Invalid Journey Key'" in {
          bodyOf(result) mustBe "Invalid Journey Key"
        }
      }

    }

  }

  ".clearJourneys" must {

    "return a NoContent" when {

      "the journeys are valid" in {
        lazy val request = FakeRequest("POST", s"/income-tax-submission-service/income-tax/nino/AA000000A/sources/clear-excluded-journeys/$taxYear")
          .withHeaders("mtditid" -> "1234567890", "sessionId" -> "sessionId")
          .withJsonBody(Json.obj("journeys" -> Json.arr(
            INTEREST,
            CIS
          )))

        val lastUpdated = Instant.now()

        val findData = ExclusionUserDataModel(
          nino,
          taxYear,
          Seq(
            ExcludeJourneyModel(INTEREST, None),
            ExcludeJourneyModel(CIS, None),
            ExcludeJourneyModel(GIFT_AID, None),
            ExcludeJourneyModel(DIVIDENDS, None)
          ),
          lastUpdated
        )

        val expectedInputData = ExclusionUserDataModel(
          nino,
          taxYear,
          Seq(
            ExcludeJourneyModel(GIFT_AID, None),
            ExcludeJourneyModel(DIVIDENDS, None)
          ),
          lastUpdated
        )

        val result = {
          mockAuth()
          mockFind(Right(Some(findData)))
          mockCreateOrUpdateMultiple(expectedInputData, Right(true))
          controller.clearJourneys(taxYear, nino)(request)
        }

        status(result) mustBe NO_CONTENT
      }

    }

    "return a BadRequest" when {

      "the body is not valid json" which {
        lazy val request = FakeRequest("POST", s"/income-tax-submission-service/income-tax/nino/AA000000A/sources/clear-excluded-journeys/$taxYear")
          .withHeaders("mtditid" -> "1234567890", "sessionId" -> "sessionId")

        lazy val result = {
          mockAuth()
          controller.clearJourneys(taxYear, nino)(request)
        }

        "has the body of 'Invalid Body'" in {
          bodyOf(result) mustBe "Invalid Body"
        }

        "has the status of BAD_REQUEST(400)" in {
          status(result) mustBe BAD_REQUEST
        }
      }

    }

    "return an InternalServerError" when {

      "the find request returns an error" in {
        lazy val request = FakeRequest("POST", s"/income-tax-submission-service/income-tax/nino/AA000000A/sources/clear-excluded-journeys/$taxYear")
          .withHeaders("mtditid" -> "1234567890", "sessionId" -> "sessionId")
          .withJsonBody(Json.obj("journeys" -> Json.arr(
            INTEREST,
            CIS
          )))

        lazy val result = {
          mockAuth()
          mockFind(Left(MongoError("eh")))
          controller.clearJourneys(taxYear, nino)(request)
        }

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "the create/update database action returns an error" in {

        val dt = Instant.now()
        lazy val request = FakeRequest("POST", s"/income-tax-submission-service/income-tax/nino/AA000000A/sources/clear-excluded-journeys/$taxYear")
          .withHeaders("mtditid" -> "1234567890", "sessionId" -> "sessionId")
          .withJsonBody(Json.obj("journeys" -> Json.arr(
            INTEREST,
            CIS
          )))

        lazy val result = {
          mockAuth()
          mockFind(Right(Some(ExclusionUserDataModel(
            nino, taxYear, Seq.empty, dt
          ))))
          mockCreateOrUpdateMultiple(ExclusionUserDataModel(nino, taxYear, Seq.empty, dt), Left(MongoError("eh")))
          controller.clearJourneys(taxYear, nino)(request)
        }

        status(result) mustBe INTERNAL_SERVER_ERROR
      }

    }

  }
}
