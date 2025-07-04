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

package controllers.predicates

import common.{DelegatedAuthRules, EnrolmentIdentifiers, EnrolmentKeys}
import config.AppConfig
import models.User
import play.api.http.Status._
import play.api.mvc.Results._
import play.api.mvc.{AnyContent, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, status}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtils

import scala.concurrent.{ExecutionContext, Future}

class AuthorisedActionSpec extends TestUtils {

  override lazy val mockAppConfig: AppConfig = mock[AppConfig]
  override val authorisedAction: AuthorisedAction = {
    new AuthorisedAction()(mockAuthConnector, defaultActionBuilder, mockControllerComponents)
  }

  ".enrolmentGetIdentifierValue" should {

    "return the value for a given identifier" in {
      val returnValue = "anIdentifierValue"
      val returnValueAgent = "anAgentIdentifierValue"

      val enrolments = Enrolments(Set(
        Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, returnValue)), "Activated"),
        Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, returnValueAgent)), "Activated")
      ))

      authorisedAction.enrolmentGetIdentifierValue(EnrolmentKeys.Individual, EnrolmentIdentifiers.individualId, enrolments) mustBe Some(returnValue)
      authorisedAction.enrolmentGetIdentifierValue(EnrolmentKeys.Agent, EnrolmentIdentifiers.agentReference, enrolments) mustBe Some(returnValueAgent)
    }
    "return a None" when {
      val key = "someKey"
      val identifierKey = "anIdentifier"
      val returnValue = "anIdentifierValue"

      val enrolments = Enrolments(Set(Enrolment(key, Seq(EnrolmentIdentifier(identifierKey, returnValue)), "someState")))


      "the given identifier cannot be found" in {
        authorisedAction.enrolmentGetIdentifierValue(key, "someOtherIdentifier", enrolments) mustBe None
      }

      "the given key cannot be found" in {
        authorisedAction.enrolmentGetIdentifierValue("someOtherKey", identifierKey, enrolments) mustBe None
      }

    }
  }

  ".individualAuthentication" should {

    "perform the block action" when {

      "the correct enrolment exist and nino exist" which {
        val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
        val mtditid = "AAAAAA"
        val enrolments = Enrolments(Set(Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtditid)), "Activated"),
          Enrolment(
            EnrolmentKeys.nino,
            Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, mtditid)), "Activated")))

        lazy val result: Future[Result] = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L250))
          authorisedAction.individualAuthentication(block, mtditid)(fakeRequest, emptyHeaderCarrier)
        }

        "returns an OK status" in {
          status(result) mustBe OK
        }

        "returns a body of the mtditid" in {
          contentAsString(result) mustBe mtditid
        }
      }

      "the correct enrolment and nino exist but the request is for a different id" which {
        val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
        val mtditid = "AAAAAA"
        val enrolments = Enrolments(Set(Enrolment(
          EnrolmentKeys.Individual,
          Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, "123456")), "Activated"),
          Enrolment(
            EnrolmentKeys.nino,
            Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, mtditid)), "Activated")
        ))

        lazy val result: Future[Result] = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L250))
          authorisedAction.individualAuthentication(block, mtditid)(fakeRequest, emptyHeaderCarrier)
        }

        "returns an UNAUTHORIZED status" in {
          status(result) mustBe UNAUTHORIZED
        }
      }
      "the correct enrolment and nino exist but low CL" which {
        val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
        val mtditid = "AAAAAA"
        val enrolments = Enrolments(Set(Enrolment(
          EnrolmentKeys.Individual,
          Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtditid)), "Activated"),
          Enrolment(
            EnrolmentKeys.nino,
            Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, mtditid)), "Activated")
        ))

        lazy val result: Future[Result] = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L50))
          authorisedAction.individualAuthentication(block, mtditid)(fakeRequest, emptyHeaderCarrier)
        }

        "returns an UNAUTHORIZED status" in {
          status(result) mustBe UNAUTHORIZED
        }
      }

      "the correct enrolment exist but no nino" which {
        val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
        val mtditid = "AAAAAA"
        val enrolments = Enrolments(Set(Enrolment(
          EnrolmentKeys.Individual,
          Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtditid)), "Activated")
        ))

        lazy val result: Future[Result] = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L250))
          authorisedAction.individualAuthentication(block, mtditid)(fakeRequest, emptyHeaderCarrier)
        }

        "returns an 401 status" in {
          status(result) mustBe UNAUTHORIZED
        }
      }
      "the correct nino exist but no enrolment" which {
        val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
        val id = "AAAAAA"
        val enrolments = Enrolments(Set(Enrolment(
          EnrolmentKeys.nino,
          Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, id)), "Activated")
        ))

        lazy val result: Future[Result] = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L250))
          authorisedAction.individualAuthentication(block, id)(fakeRequest, emptyHeaderCarrier)
        }

        "returns an 401 status" in {
          status(result) mustBe UNAUTHORIZED
        }
      }

    }
    "return a UNAUTHORIZED" when {

      "the correct enrolment is missing" which {
        val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
        val mtditid = "AAAAAA"
        val enrolments = Enrolments(Set(Enrolment("notAnIndividualOops", Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtditid)), "Activated")))

        lazy val result: Future[Result] = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L250))
          authorisedAction.individualAuthentication(block, mtditid)(fakeRequest, emptyHeaderCarrier)
        }

        "returns an UNAUTHORIZED" in {
          status(result) mustBe UNAUTHORIZED
        }
      }
    }

    "the correct enrolment and nino exist but the request is for a different id" which {
      val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
      val mtditid = "AAAAAA"
      val enrolments = Enrolments(Set(Enrolment(
        EnrolmentKeys.Individual,
        Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, "123456")), "Activated"),
        Enrolment(
          EnrolmentKeys.nino,
          Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, mtditid)), "Activated")
      ))

      lazy val result: Future[Result] = {
        (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
          .returning(Future.successful(enrolments and ConfidenceLevel.L250))
        authorisedAction.individualAuthentication(block, mtditid)(fakeRequest, emptyHeaderCarrier)
      }

      "returns an UNAUTHORIZED status" in {
        status(result) mustBe UNAUTHORIZED
      }
    }
    "the session id does not exist in the headers" which {
      val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
      val mtditid = "AAAAAA"
      val enrolments = Enrolments(Set(Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtditid)), "Activated"),
        Enrolment(
          EnrolmentKeys.nino,
          Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, mtditid)), "Activated")))

      val request = FakeRequest("GET",
        "/income-tax-submission-service/income-tax/nino/AA123456A/sources?taxYear=2022").withHeaders("mtditid" -> "1234567890")

      lazy val result: Future[Result] = {
        (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
          .returning(Future.successful(enrolments and ConfidenceLevel.L250))
        authorisedAction.individualAuthentication(block, mtditid)(request, emptyHeaderCarrier)
      }

      "returns an UNAUTHORIZED status" in {
        status(result) mustBe UNAUTHORIZED
      }
    }
    "the correct enrolment and nino exist but low CL" which {
      val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
      val mtditid = "AAAAAA"
      val enrolments = Enrolments(Set(Enrolment(
        EnrolmentKeys.Individual,
        Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtditid)), "Activated"),
        Enrolment(
          EnrolmentKeys.nino,
          Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, mtditid)), "Activated")
      ))

      lazy val result: Future[Result] = {
        (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
          .returning(Future.successful(enrolments and ConfidenceLevel.L50))
        authorisedAction.individualAuthentication(block, mtditid)(fakeRequest, emptyHeaderCarrier)
      }

      "returns an UNAUTHORIZED status" in {
        status(result) mustBe UNAUTHORIZED
      }
    }

    "the correct enrolment exist but no nino" which {
      val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
      val mtditid = "AAAAAA"
      val enrolments = Enrolments(Set(Enrolment(
        EnrolmentKeys.Individual,
        Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtditid)), "Activated")
      ))

      lazy val result: Future[Result] = {
        (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
          .returning(Future.successful(enrolments and ConfidenceLevel.L250))
        authorisedAction.individualAuthentication(block, mtditid)(fakeRequest, emptyHeaderCarrier)
      }

      "returns an 401 status" in {
        status(result) mustBe UNAUTHORIZED
      }
    }
    "the correct nino exist but no enrolment" which {
      val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
      val id = "AAAAAA"
      val enrolments = Enrolments(Set(Enrolment(
        EnrolmentKeys.nino,
        Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, id)), "Activated")
      ))

      lazy val result: Future[Result] = {
        (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
          .returning(Future.successful(enrolments and ConfidenceLevel.L250))
        authorisedAction.individualAuthentication(block, id)(fakeRequest, emptyHeaderCarrier)
      }

      "returns an 401 status" in {
        status(result) mustBe UNAUTHORIZED
      }
    }
  }

  ".agentAuthenticated" should {

    val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(s"${user.mtditid} ${user.arn.get}"))

    "perform the block action" when {

      "the agent is authorised for the given user (primary Agent)" which {

        val enrolments = Enrolments(Set(
          Enrolment(
            key = EnrolmentKeys.Individual,
            identifiers = Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, "1234567890")),
            state = "Activated",
            delegatedAuthRule = Some(DelegatedAuthRules.agentDelegatedAuthRule)
          ),
          Enrolment(
            key = EnrolmentKeys.Agent,
            identifiers = Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, "0987654321")),
            state = "Activated"
          )
        ))

        lazy val result = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, Retrievals.allEnrolments, *, *)
            .returning(Future.successful(enrolments))

          authorisedAction.agentAuthentication(block, "1234567890")(fakeRequest, emptyHeaderCarrier)
        }

        "has a status of OK" in {
          status(result) mustBe OK
        }

        "has the correct body" in {
          contentAsString(result) mustBe "1234567890 0987654321"
        }
      }
    }

    "return an Unauthorised" when {

      "the authorisation service returns an AuthorisationException" in {
        object AuthException extends AuthorisationException("Some reason")

        lazy val result = {

          //First auth failure to simulate not being a primary agent
          mockAuthReturnException(AuthException)

          authorisedAction.agentAuthentication(block, "1234567890")(fakeRequest, emptyHeaderCarrier)
        }
        status(result) mustBe UNAUTHORIZED
      }

      "the session id does not exist in the headers" which {
        val enrolments = Enrolments(Set(
          Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, "1234567890")), "Activated"),
          Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, "0987654321")), "Activated")
        ))

        val request = FakeRequest("GET",
          "/income-tax-submission-service/income-tax/nino/AA123456A/sources?taxYear=2022").withHeaders("mtditid" -> "1234567890")

        lazy val result = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, Retrievals.allEnrolments, *, *)
            .returning(Future.successful(enrolments))

          authorisedAction.agentAuthentication(block, "1234567890")(request, emptyHeaderCarrier)
        }

        "returns an UNAUTHORIZED status" in {
          status(result) mustBe UNAUTHORIZED
        }
      }

      "the authorisation service returns a NoActiveSession exception" in {
        object NoActiveSession extends NoActiveSession("Some reason")

        lazy val result = {
          mockAuthReturnException(NoActiveSession)
          authorisedAction.agentAuthentication(block, "1234567890")(fakeRequest, emptyHeaderCarrier)
        }

        status(result) mustBe UNAUTHORIZED
      }

      "the user does not have an enrolment for the agent" in {
        val enrolments = Enrolments(Set(
          Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, "1234567890")), "Activated")
        ))

        lazy val result = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, Retrievals.allEnrolments, *, *)
            .returning(Future.successful(enrolments))
          authorisedAction.agentAuthentication(block, "1234567890")(fakeRequest, emptyHeaderCarrier)
        }
        status(result) mustBe UNAUTHORIZED
      }

      "the user does not the nino in uri" in {

        val fakeRequest = FakeRequest("GET",
          "/income-tax-submission-service/income-tax/id/AA123123A/sources?taxYear=2022").withHeaders("mtditid" -> "1234567890")

        lazy val result = {
          authorisedAction.agentAuthentication(block, "1234567890")(fakeRequest, emptyHeaderCarrier)
        }
        status(result) mustBe UNAUTHORIZED
      }
    }

    "return an InternalServerError" when {
      "initial agent auth call returns an unexpected error" in {
        object UnexpectedError extends IndexOutOfBoundsException("Some reason")

        lazy val result = {
          mockAuthReturnException(UnexpectedError)
          authorisedAction.agentAuthentication(block, "1234567890")(fakeRequest, emptyHeaderCarrier)
        }
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  ".async" should {

    lazy val block: User[AnyContent] => Future[Result] = user =>
      Future.successful(Ok(s"mtditid: ${user.mtditid}${user.arn.fold("")(arn => " arn: " + arn)}"))

    "perform the block action" when {

      "the user is successfully verified as an agent" which {

        lazy val result: Future[Result] = {
          mockAuthAsAgent()
          authorisedAction.async(block)(fakeRequest)
        }

        "should return an OK(200) status" in {

          status(result) mustBe OK
          contentAsString(result) mustBe "mtditid: 1234567890 arn: 0987654321"
        }
      }

      "the user is successfully verified as an individual" in {

        lazy val result = {
          mockAuth()
          authorisedAction.async(block)(fakeRequest)
        }

        status(result) mustBe OK
        contentAsString(result) mustBe "mtditid: 1234567890"
      }
    }

    "return an InternalServerError" when {
      "the authorisation service returns an unexpected exception" in {
        object AuthException extends IndexOutOfBoundsException("Some reason")

        lazy val result = {
          mockAuthReturnException(AuthException)
          authorisedAction.async(block)
        }

        status(result(fakeRequest)) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "return an Unauthorised" when {
      "the authorisation service returns an AuthorisationException exception" in {
        object AuthException extends AuthorisationException("Some reason")

        lazy val result = {
          mockAuthReturnException(AuthException)
          authorisedAction.async(block)
        }

        status(result(fakeRequest)) mustBe UNAUTHORIZED
      }

      "the authorisation service returns a NoActiveSession exception" in {
        object NoActiveSession extends NoActiveSession("Some reason")

        lazy val result = {
          mockAuthReturnException(NoActiveSession)
          authorisedAction.async(block)
        }

        status(result(fakeRequest)) mustBe UNAUTHORIZED
      }

      "the request does not contain mtditid header" in {
        lazy val result = {
          authorisedAction.async(block)
        }

        status(result(FakeRequest())) mustBe UNAUTHORIZED
      }
    }

  }
}
