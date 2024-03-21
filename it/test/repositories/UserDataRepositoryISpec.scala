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

package repositories

import helpers.IntegrationSpec
import models.mongo._
import models.{EncryptedExcludeJourneyModel, ExcludeJourneyModel, User}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.result.InsertOneResult
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.mvc.{AnyContent, AnyContentAsEmpty}
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}
import uk.gov.hmrc.crypto.EncryptedValue
import uk.gov.hmrc.http.SessionKeys.sessionId
import uk.gov.hmrc.mongo.play.json.Codecs.toBson

class UserDataRepositoryISpec extends IntegrationSpec with FutureAwaits with DefaultAwaitTimeout {

  val mtditid = "1234567890"
  val nino = "AA000000A"
  val taxYear = 2023

  lazy val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders("X-Session-ID" -> sessionId)

  implicit lazy val user: User[AnyContent] = new User[AnyContent](mtditid, None, nino, sessionId)(FakeRequest().withHeaders("X-Session-ID" -> sessionId))

  lazy val excludedRepo: ExclusionUserDataRepositoryImpl = app.injector.instanceOf[ExclusionUserDataRepositoryImpl]
  lazy val excludedInvalidRepo: ExclusionUserDataRepository = appWithInvalidEncryptionKey.injector.instanceOf[ExclusionUserDataRepository]

  private def count: Long = await(excludedRepo.collection.countDocuments().toFuture())

  class EmptyDatabase {
    await(excludedRepo.collection.drop().toFuture())
    await(excludedRepo.ensureIndexes)
  }

  lazy val excludedUserData: ExclusionUserDataModel = ExclusionUserDataModel(
    nino,
    taxYear,
    Seq(ExcludeJourneyModel("interest", None))
  )

  implicit val request: FakeRequest[AnyContent] = FakeRequest()

  "create" must {
    "add a document to the collection" in new EmptyDatabase {
      count mustBe 0
      val result: Either[DatabaseError, Boolean] = await(excludedRepo.create(excludedUserData))
      result mustBe Right(true)
      count mustBe 1
    }
    "fail to add a document to the collection when it already exists" in new EmptyDatabase {
      count mustBe 0
      await(excludedRepo.create(excludedUserData))
      val result: Either[DatabaseError, Boolean] = await(excludedRepo.create(excludedUserData))
      result mustBe Left(DataNotUpdated)
      count mustBe 1
    }
  }

  "update" must {

    "update a document in the collection" in new EmptyDatabase {
      val testUser: User[AnyContent] = User(
        mtditid, None, nino, sessionId
      )(fakeRequest)

      val initialData: ExclusionUserDataModel = ExclusionUserDataModel(
        testUser.nino, taxYear,
        Seq(ExcludeJourneyModel("interest", None))
      )

      val newUserData: ExclusionUserDataModel = initialData.copy(
        exclusionModel = Seq(ExcludeJourneyModel("interest", None), ExcludeJourneyModel("dividends", None))
      )

      await(excludedRepo.create(initialData))
      count mustBe 1

      val res: Boolean = await(excludedRepo.update(newUserData).map {
        case Right(value) => value
        case Left(value) => false
      })
      res mustBe true
      count mustBe 1

      val data: Option[ExclusionUserDataModel] = await(excludedRepo.find(taxYear)(testUser).map {
        case Right(value) => value
        case Left(_) => None
      })

      data.get.exclusionModel shouldBe Seq(ExcludeJourneyModel("interest", None), ExcludeJourneyModel("dividends", None))
    }

    "return a leftDataNotUpdated if the document cannot be found" in {
      val newUserData = excludedUserData.copy(nino = "AA987654A")
      count mustBe 1
      val res = await(excludedRepo.update(newUserData))
      res mustBe Left(DataNotUpdated)
      count mustBe 1
    }
  }

  "find" must {
    def filter(nino: String, taxYear: Int): Bson = org.mongodb.scala.model.Filters.and(
      org.mongodb.scala.model.Filters.equal("nino", toBson(nino)),
      org.mongodb.scala.model.Filters.equal("taxYear", toBson(taxYear))
    )

    val testUser = User(
      mtditid, None, nino, sessionId
    )(fakeRequest)

    "get a document" in {
      count mustBe 1
      val dataAfter: Option[ExclusionUserDataModel] = await(excludedRepo.find(taxYear)(testUser).map {
        case Right(value) => value
        case Left(_) => fail("Unable to retrieve data from DB")
      })

      dataAfter.get.exclusionModel mustBe Seq(ExcludeJourneyModel("interest", None), ExcludeJourneyModel("dividends", None))
    }

    "return an dataNotFoundError" in {
      await(excludedInvalidRepo.find(taxYear)(testUser)) mustBe
        Left(EncryptionDecryptionError(
          "Failed encrypting data"))
    }
  }

  "the set indexes" must {

    "enforce uniqueness" in {
      val result: Either[Exception, InsertOneResult] = try {
        Right(await(excludedRepo.collection.insertOne(EncryptedExclusionUserDataModel(
          nino, taxYear, List(EncryptedExcludeJourneyModel(EncryptedValue("test", "test"), None))
        )).toFuture()))
      } catch {
        case e: Exception => Left(e)
      }
      result.isLeft mustBe true
      result.left.get.getMessage must include(
        "E11000 duplicate key error collection: income-tax-submission.exclusionUserData index: UserDataLookupIndex dup key:")
    }

  }

  "clear" must {

    "clear the document for the current user" in new EmptyDatabase {
      count shouldBe 0
      await(excludedRepo.create(ExclusionUserDataModel(nino, taxYear, Seq(ExcludeJourneyModel("interest", None)))))
      count shouldBe 1
      await(excludedRepo.clear(taxYear))
      count shouldBe 0
    }
  }

}
