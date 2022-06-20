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

package services

import common.IncomeSources.{CIS, DIVIDENDS, GIFT_AID, INTEREST}
import models._
import models.gifts.{GiftAid, GiftAidPayments, Gifts}
import models.mongo.{DatabaseError, ExclusionUserDataModel, MongoError}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.mvc.AnyContentAsEmpty
import repositories.ExclusionUserDataRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtils

import scala.concurrent.Future

class ExcludeJourneyServiceSpec extends TestUtils {

  private val mtditid = "1234567890"
  private val nino = "AA000000A"
  private val sessionId = "1234567890987654321"

  implicit val implicitUser: User[_] = user

  private lazy val user: User[AnyContentAsEmpty.type] = User(mtditid, None, nino, sessionId)

  private val taxYear = 2023

  private lazy val excludeRepository = mock[ExclusionUserDataRepository]
  private lazy val incomeSourcesConnector = mock[GetIncomeSourcesService]

  lazy val service = new ExcludeJourneyService(
    excludeRepository,
    incomeSourcesConnector
  )

  private def mockFind(result: Either[DatabaseError, Option[ExclusionUserDataModel]]) = {
    (excludeRepository.find(_: Int)(_: User[_]))
      .expects(*, *)
      .returning(Future.successful(result))
  }

  private def mockInterestCall(taxYear: Int, result: Option[Either[APIErrorModel, Option[List[Interest]]]] = None) = {
    val defaultResult = Right(Some(List(
      Interest("Whiterun Account", "", None, None)
    )))

    (incomeSourcesConnector.getInterest(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
      .expects(*, taxYear, *, *, *)
      .returning(Future.successful(result.getOrElse(defaultResult)))
  }

  private def mockGiftAidCall(taxYear: Int, result: Option[Either[APIErrorModel, Option[GiftAid]]] = None) = {
    val defaultResult = Right(Some(
      GiftAid(
        Some(GiftAidPayments(Some(List("Knee Protection Ltd.")), None, None, None, None, None)),
        Some(Gifts(Some(List("Riften Gate Fund")), None, None, None))
      )
    ))

    (incomeSourcesConnector.getGiftAid(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
      .expects(*, taxYear, *, *, *)
      .returning(Future.successful(result.getOrElse(defaultResult)))
  }

  private def mockCreate(expectedInput: ExclusionUserDataModel, result: Either[DatabaseError, Boolean]) = {
    (excludeRepository.create(_: ExclusionUserDataModel)(_: User[_]))
      .expects(expectedInput, *)
      .returning(Future.successful(result))
  }

  private def mockUpdate(expectedInput: ExclusionUserDataModel, result: Either[DatabaseError, Boolean]) = {
    (excludeRepository.update(_: ExclusionUserDataModel))
      .expects(expectedInput)
      .returning(Future.successful(result))
  }

  ".obtainHash" must {

    "return a Right(None)" when {

      "the journey is neither Interest nor GiftAid" in {
        val result = {
          await(service.obtainHash(taxYear, DIVIDENDS)).right.get
        }

        result mustBe None
      }

    }

    "return a hash" when {

      "the journey is Interest" in {
        val result = {
          mockInterestCall(taxYear)
          await(service.obtainHash(taxYear, INTEREST)).right.get
        }
        if (result.isEmpty) fail("Interest result is a None")
        result mustBe Some("94a7073922648d8c1a834f2c83f07b173249f7404772e973b5d23b6d7ae69af4")
      }

      "the journey is GiftAid" in {
        val result = {
          mockGiftAidCall(taxYear)
          await(service.obtainHash(taxYear, GIFT_AID)).right.get
        }
        if (result.isEmpty) fail("Interest result is a None")
        result mustBe Some("394e36048bb70addfe89538cb638d7796400ceca72fc61b49b07eb34264357c5")
      }

    }

    "return an error" when {

      "an error occurs while looking for interest data" in {
        lazy val error = Some(Left(APIErrorModel(
          INTERNAL_SERVER_ERROR,
          APIErrorBodyModel("A_BIG_OOPS", "oops")
        )))

        val result = {
          mockInterestCall(taxYear, error)
          await(service.obtainHash(taxYear, INTEREST))
        }
        result.left.get.body mustBe APIErrorBodyModel("A_BIG_OOPS", "oops")
      }

      "an error occurs while looking for gift aid data" in {
        lazy val error = Some(Left(APIErrorModel(
          INTERNAL_SERVER_ERROR,
          APIErrorBodyModel("A_BIG_OOPS", "oops")
        )))

        val result = {
          mockGiftAidCall(taxYear, error)
          await(service.obtainHash(taxYear, GIFT_AID))
        }
        result.left.get.body mustBe APIErrorBodyModel("A_BIG_OOPS", "oops")
      }

    }

  }

  ".findExclusionData" should {

    "return the result from the dabase" in {
      val returnedUserData = ExclusionUserDataModel(nino, taxYear, Seq(
        ExcludeJourneyModel(GIFT_AID, None)
      ))

      val result = {
        mockFind(Right(Some(returnedUserData)))
        service.findExclusionData(taxYear)
      }

      await(result) mustBe Right(Some(returnedUserData))
    }

  }

  ".createOrUpdate" when {

    "pre existing is true and there are prior exclusions without the current journey" should {

      "update the data with an updated list of journeys" in {
        val newJourney = ExcludeJourneyModel(INTEREST, None)
        val oldJourneys = ExclusionUserDataModel(nino, taxYear, Seq(
          ExcludeJourneyModel(GIFT_AID, None)
        ))

        val expectedInput = ExclusionUserDataModel(nino, taxYear, Seq(
          ExcludeJourneyModel(GIFT_AID, None),
          newJourney
        ))

        val result = {
          mockUpdate(expectedInput, Right(true))
          await(service.createOrUpdate(newJourney, oldJourneys, preExisting = true))
        }

        result mustBe Right(true)
      }

    }

    "pre existing is true and there are prior exclusions with the current journey" should {

      "update the data with an updated list of journeys" in {
        val newJourney = ExcludeJourneyModel(INTEREST, None)
        val oldJourneys = ExclusionUserDataModel(nino, taxYear, Seq(
          ExcludeJourneyModel(GIFT_AID, None),
          newJourney
        ))

        val expectedInput = ExclusionUserDataModel(nino, taxYear, Seq(
          ExcludeJourneyModel(GIFT_AID, None),
          newJourney
        ))

        val result = {
          mockUpdate(expectedInput, Right(true))
          await(service.createOrUpdate(newJourney, oldJourneys, preExisting = true))
        }

        result mustBe Right(true)
      }

    }

    "pre existing is false and there are prior exclusions without the current journey" should {

      "create the data with an updated list of journeys" in {
        val newJourney = ExcludeJourneyModel(INTEREST, None)
        val oldJourneys = ExclusionUserDataModel(nino, taxYear, Seq(
          ExcludeJourneyModel(GIFT_AID, None)
        ))

        val expectedInput = ExclusionUserDataModel(nino, taxYear, Seq(
          ExcludeJourneyModel(GIFT_AID, None),
          newJourney
        ))

        val result = {
          mockCreate(expectedInput, Right(true))
          await(service.createOrUpdate(newJourney, oldJourneys, preExisting = false))
        }

        result mustBe Right(true)
      }

    }

    "pre existing is false and there are prior exclusions with the current journey" should {

      "create the data with an updated list of journeys" in {
        val newJourney = ExcludeJourneyModel(INTEREST, None)
        val oldJourneys = ExclusionUserDataModel(nino, taxYear, Seq(
          ExcludeJourneyModel(GIFT_AID, None),
          newJourney
        ))

        val expectedInput = ExclusionUserDataModel(nino, taxYear, Seq(
          ExcludeJourneyModel(GIFT_AID, None),
          newJourney
        ))

        val result = {
          mockCreate(expectedInput, Right(true))
          await(service.createOrUpdate(newJourney, oldJourneys, preExisting = false))
        }

        result mustBe Right(true)
      }

    }

    "when passing in an exclusion model with no new journey" when {

      "preExisting is true" should {

        "return a true, if the database does" in {
          val model = ExclusionUserDataModel(nino, taxYear, Seq(
            ExcludeJourneyModel(GIFT_AID, None),
            ExcludeJourneyModel(CIS, None)
          ))

          val result = {
            mockUpdate(model, Right(true))
            service.createOrUpdate(model, preExisting = true)
          }

          await(result) mustBe Right(true)
        }
      }

      "preExisting is false" should {

        "return a true, if the database does" in {
          val model = ExclusionUserDataModel(nino, taxYear, Seq(
            ExcludeJourneyModel(GIFT_AID, None),
            ExcludeJourneyModel(CIS, None)
          ))

          val result = {
            mockCreate(model, Right(true))
            service.createOrUpdate(model, preExisting = false)
          }

          await(result) mustBe Right(true)
        }
      }

    }
  }

  ".removeJourney" should {

    "return a true" when {

      "the provided journey key is removed from the excluded list" in {
        val findData = Right(Some(ExclusionUserDataModel(
          nino, taxYear, Seq(
            ExcludeJourneyModel(GIFT_AID, None),
            ExcludeJourneyModel(INTEREST, None)
          )
        )))

        val expectedUpdateInput = ExclusionUserDataModel(
          nino, taxYear, Seq(
            ExcludeJourneyModel(GIFT_AID, None)
          )
        )

        val result = {
          mockFind(findData)
          mockUpdate(expectedUpdateInput, Right(true))
          service.removeJourney(taxYear, INTEREST)
        }

        await(result) mustBe Right(true)
      }

      "there is no exclusion data" in {
        val result = {
          mockFind(Right(None))
          service.removeJourney(taxYear, INTEREST)
        }

        await(result) mustBe Right(true)
      }

    }

    "return a database error" when {

      "there is an error finding the exclusion data" in {
        val findData = Left(MongoError("nah mate"))

        val result = {
          mockFind(findData)
          service.removeJourney(taxYear, INTEREST)
        }

        await(result) mustBe Left(MongoError("nah mate"))
      }

      "there is an error updating the exclusion data" in {
        val findData = Right(Some(ExclusionUserDataModel(
          nino, taxYear, Seq(
            ExcludeJourneyModel(GIFT_AID, None),
            ExcludeJourneyModel(INTEREST, None)
          )
        )))

        val expectedUpdateInput = ExclusionUserDataModel(
          nino, taxYear, Seq(
            ExcludeJourneyModel(GIFT_AID, None)
          )
        )

        val result = {
          mockFind(findData)
          mockUpdate(expectedUpdateInput, Left(MongoError("couldn't update")))
          service.removeJourney(taxYear, INTEREST)
        }

        await(result) mustBe Left(MongoError("couldn't update"))
      }

    }

  }

}
