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

import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates.set
import config.AppConfig
import models.User
import models.mongo._
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.{and, equal}
import org.mongodb.scala.model.Indexes.{ascending, compoundIndex}
import org.mongodb.scala.model.{FindOneAndReplaceOptions, FindOneAndUpdateOptions, IndexModel, IndexOptions}
import play.api.Logging
import services.EncryptionService
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs.toBson
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import utils.PagerDutyHelper.PagerDutyKeys.{ENCRYPTION_DECRYPTION_ERROR, FAILED_TO_FIND_DATA, FAILED_TO_UPDATE_DATA}
import utils.PagerDutyHelper.pagerDutyLog

import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class IncomeTaxUserDataRepositoryImpl @Inject()(mongo: MongoComponent, appConfig: AppConfig, encryptionService: EncryptionService)(implicit ec: ExecutionContext
) extends PlayMongoRepository[EncryptedUserData](
  mongoComponent = mongo,
  collectionName = "userData",
  domainFormat = EncryptedUserData.formats,
  indexes = IncomeTaxUserDataIndexes.indexes(appConfig)
) with IncomeTaxUserDataRepository with Logging {

  def update(userData: UserData): Future[Either[DatabaseError, Unit]] = {

    lazy val start = "[IncomeTaxUserDataRepositoryImpl][update]"

    Try {
      encryptionService.encryptUserData(userData)
    }.toEither match {
      case Left(exception: Exception) => Future.successful(handleEncryptionDecryptionException(exception, start))
      case Right(encryptedData) =>

        collection.findOneAndReplace(
          filter = filter(userData.sessionId, userData.mtdItId, userData.nino, userData.taxYear),
          replacement = encryptedData,
          options = FindOneAndReplaceOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
        ).toFutureOption().map {
          case Some(_) => Right(())
          case None =>
            pagerDutyLog(FAILED_TO_UPDATE_DATA, s"$start Failed to update user data.")
            Left(DataNotUpdated)
        }.recover {
          case exception: Exception =>
            pagerDutyLog(FAILED_TO_UPDATE_DATA, s"$start Failed to update user data. Exception: ${exception.getMessage}")
            Left(MongoError(exception.getMessage))
        }
    }
  }

  def find[T](user: User[T], taxYear: Int): Future[Either[DatabaseError, Option[UserData]]] = {

    lazy val start = "[IncomeTaxUserDataRepositoryImpl][find]"

    val findResult = collection.findOneAndUpdate(
      filter = filter(user.sessionId, user.mtditid, user.nino, taxYear),
      update = set("lastUpdated", toBson(Instant.now())),
      options = FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
    ).toFutureOption().map(Right(_)).recover {
      case exception: Exception =>
        pagerDutyLog(FAILED_TO_FIND_DATA, s"$start Failed to find user data. Exception: ${exception.getMessage}")
        Left(MongoError(exception.getMessage))
    }

    findResult.map {
      case Left(error) => Left(error)
      case Right(encryptedData) =>
        Try {
          encryptedData.map(encryptionService.decryptUserData)
        }.toEither match {
          case Left(exception: Exception) => handleEncryptionDecryptionException(exception, start)
          case Right(decryptedData) => Right(decryptedData)
        }
    }
  }

  private def filter(sessionId: String, mtdItId: String, nino: String, taxYear: Int): Bson = and(
    equal("sessionId", toBson(sessionId)),
    equal("mtdItId", toBson(mtdItId)),
    equal("nino", toBson(nino)),
    equal("taxYear", toBson(taxYear))
  )

  def handleEncryptionDecryptionException[T](exception: Exception, startOfMessage: String): Left[DatabaseError, T] = {
    pagerDutyLog(ENCRYPTION_DECRYPTION_ERROR, s"$startOfMessage ${exception.getMessage}")
    Left(EncryptionDecryptionError(exception.getMessage))
  }
}

trait IncomeTaxUserDataRepository {
  def find[T](user: User[T], taxYear: Int): Future[Either[DatabaseError, Option[UserData]]]
  def update(userData: UserData): Future[Either[DatabaseError, Unit]]
}

private object IncomeTaxUserDataIndexes {

  private val lookUpIndex: Bson = compoundIndex(
    ascending("sessionId"),
    ascending("mtdItId"),
    ascending("nino"),
    ascending("taxYear")
  )

  def indexes(appConfig: AppConfig): Seq[IndexModel] = {
    Seq(
      IndexModel(lookUpIndex, IndexOptions().unique(true).name("UserDataLookupIndex")),
      IndexModel(ascending("lastUpdated"), IndexOptions().expireAfter(appConfig.mongoTTL, TimeUnit.MINUTES).name("UserDataTTL"))
    )
  }

}
