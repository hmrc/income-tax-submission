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

import builders.models.DividendsBuilder.aDividends
import builders.models.mongo.UserDataBuilder.aUserData
import com.mongodb.client.result.InsertOneResult
import helpers.IntegrationSpec
import models.User
import models.mongo.{DatabaseError, EncryptionDecryptionError, UserData}
import org.bson.conversions.Bson
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import org.scalatest.BeforeAndAfterAll
import play.api.mvc.AnyContentAsEmpty
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}
import services.EncryptionService
import uk.gov.hmrc.mongo.MongoUtils
import uk.gov.hmrc.mongo.play.json.Codecs.toBson

import java.time.Instant
import scala.concurrent.Future

class IncomeTaxUserDataRepositoryISpec extends IntegrationSpec
  with BeforeAndAfterAll with FutureAwaits with DefaultAwaitTimeout {

  val repo: IncomeTaxUserDataRepositoryImpl = app.injector.instanceOf[IncomeTaxUserDataRepositoryImpl]
  val encryption: EncryptionService = app.injector.instanceOf[EncryptionService]

  private def count = await(repo.collection.countDocuments().toFuture())

  private def countFromOtherDatabase = await(repo.collection.countDocuments().toFuture())

  val repoWithInvalidEncryption: IncomeTaxUserDataRepositoryImpl = appWithInvalidEncryptionKey.injector.instanceOf[IncomeTaxUserDataRepositoryImpl]

  class EmptyDatabase {
    await(repo.collection.drop().toFuture())
    await(repo.ensureIndexes)
    await(repoWithInvalidEncryption.collection.drop().toFuture())
    await(repoWithInvalidEncryption.ensureIndexes)
  }

  val serviceWithInvalidEncryption: EncryptionService = appWithInvalidEncryptionKey.injector.instanceOf[EncryptionService]

  implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  "update with invalid encryption" should {
    "fail to add data" in new EmptyDatabase {
      countFromOtherDatabase mustBe 0
      val res: Either[DatabaseError, Unit] = await(repoWithInvalidEncryption.update(aUserData))
      res mustBe Left(EncryptionDecryptionError(
        "Failed encrypting data"))
    }
  }

  "find with invalid encryption" should {
    "fail to find data" in new EmptyDatabase {
      countFromOtherDatabase mustBe 0
      await(repoWithInvalidEncryption.collection.insertOne(encryption.encryptUserData(aUserData)).toFuture())
      countFromOtherDatabase mustBe 1
      val res = await(repoWithInvalidEncryption.find(User(aUserData.mtdItId, None, aUserData.nino, aUserData.sessionId), aUserData.taxYear))
      res mustBe Left(EncryptionDecryptionError(
        "Failed encrypting data"))
    }
  }

  "update" should {
    "fail to add a document to the collection when a mongo error occurs" in new EmptyDatabase {

      def ensureIndexes: Future[Seq[String]] = {
        val indexes = Seq(IndexModel(ascending("taxYear"), IndexOptions().unique(true).name("fakeIndex")))
        MongoUtils.ensureIndexes(repo.collection, indexes, true)
      }

      await(ensureIndexes)
      count mustBe 0

      val res: Either[DatabaseError, Unit] = await(repo.update(aUserData))
      res mustBe Right(())
      count mustBe 1

      val res2: Either[DatabaseError, Unit] = await(repo.update(aUserData.copy(sessionId = "1234567890")))
      res2.left.e.swap.getOrElse(new Exception("").getMessage).toString must include("Command failed with error 11000 (DuplicateKey)")
      count mustBe 1
    }

    "add a document to the collection" in new EmptyDatabase {
      count mustBe 0
      val res: Either[DatabaseError, Unit] = await(repo.update(aUserData))
      res mustBe Right(())
      count mustBe 1
      val data: Either[DatabaseError, Option[UserData]] =
        await(repo.find(User(aUserData.mtdItId, None, aUserData.nino, aUserData.sessionId), aUserData.taxYear))
      data.toOption.get.map(_.copy(lastUpdated = Instant.parse("2021-05-17T14:01:52.634Z"))) mustBe Some(
        aUserData.copy(lastUpdated = Instant.parse("2021-05-17T14:01:52.634Z"))
      )
    }

    "upsert a document to the collection when already exists" in {
      count mustBe 1
      val res = await(repo.update(aUserData))
      res mustBe Right(())
      count mustBe 1
      val data = await(repo.find(User(aUserData.mtdItId, None, aUserData.nino, aUserData.sessionId), aUserData.taxYear))
      data.toOption.get.map(_.copy(lastUpdated = Instant.parse("2021-05-17T14:01:52.634Z"))) mustBe Some(
        aUserData.copy(lastUpdated = Instant.parse("2021-05-17T14:01:52.634Z"))
      )
    }
    "update a document in the collection" in {
      val newUserData = aUserData.copy(dividends = Some(aDividends.copy(ukDividends = Some(344565.44))))
      count mustBe 1
      val res = await(repo.update(newUserData))
      res mustBe Right(())
      count mustBe 1
      val data = await(repo.find(User(aUserData.mtdItId, None, aUserData.nino, aUserData.sessionId), aUserData.taxYear))
      data.toOption.get.map(_.copy(lastUpdated = Instant.parse("2021-05-17T14:01:52.634Z"))) mustBe Some(
        newUserData.copy(lastUpdated = Instant.parse("2021-05-17T14:01:52.634Z"))
      )
    }
    "insert a new document to the collection if the sessionId is different" in {
      val newUserData = aUserData.copy(sessionId = "sessionId-000001")
      count mustBe 1
      val res = await(repo.update(newUserData))
      res mustBe Right(())
      count mustBe 2
    }
  }

  "find" should {

    def filter(sessionId: String, mtdItId: String, nino: String, taxYear: Int): Bson = org.mongodb.scala.model.Filters.and(
      org.mongodb.scala.model.Filters.equal("sessionId", toBson(sessionId)),
      org.mongodb.scala.model.Filters.equal("mtdItId", toBson(mtdItId)),
      org.mongodb.scala.model.Filters.equal("nino", toBson(nino)),
      org.mongodb.scala.model.Filters.equal("taxYear", toBson(taxYear))
    )

    "get a document and update the TTL" in {
      count mustBe 2
      val dataBefore: UserData = encryption.decryptUserData(await(repo.collection.find(filter(aUserData.sessionId, aUserData.mtdItId, aUserData.nino, aUserData.taxYear)).toFuture()).head)
      val dataAfter = await(repo.find(User(aUserData.mtdItId, None, aUserData.nino, aUserData.sessionId), aUserData.taxYear))

      dataAfter.toOption.get.map(_.copy(lastUpdated = dataBefore.lastUpdated)) mustBe Some(dataBefore)
      dataAfter.toOption.get.map(_.lastUpdated.isAfter(dataBefore.lastUpdated)) mustBe Some(true)
    }
  }

  "the set indexes" should {
    "enforce uniqueness" in {
      val result: Either[Exception, InsertOneResult] = try {
        Right(await(repo.collection.insertOne(encryption.encryptUserData(aUserData)).toFuture()))
      } catch {
        case e: Exception => Left(e)
      }
      result.isLeft mustBe true
      result.left.e.swap.getOrElse(new Exception(""))
        .getMessage must include("E11000 duplicate key error collection: income-tax-submission.userData index: UserDataLookupIndex dup key:")
    }
  }
}
