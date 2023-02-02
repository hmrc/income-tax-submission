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
import models.User
import models.mongo._
import org.joda.time.{DateTime, DateTimeZone}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.{and, equal}
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.model.{FindOneAndReplaceOptions, FindOneAndUpdateOptions}
import uk.gov.hmrc.mongo.play.json.Codecs.{logger, toBson}
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats
import utils.PagerDutyHelper.PagerDutyKeys._
import utils.PagerDutyHelper.pagerDutyLog

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Left, Try}

trait UserDataRepository[C <: ExclusionUserDataTemplate, V] {
  self: PlayMongoRepository[C] =>
  implicit val ec: ExecutionContext

  val repoName: String

  def encryptionMethod: V => C

  def decryptionMethod: C => V

  def create[T](userData: V)(implicit user: User[T]): Future[Either[DatabaseError, Boolean]] = {
    lazy val start = s"[$repoName][create]"
    Try {
      encryptionMethod(userData)
    }.toOption match {
      case None => Future.failed(new Exception(DataNotUpdated.message))
      case Some(exception: Exception) => Future.successful(handleEncryptionDecryptionException(exception, start))
      case Some(encryptedData) =>
        collection.insertOne(encryptedData).toFutureOption().map {
          case Some(_) => Right(true)
          case None => Left(DataNotUpdated)
        }.recover {
          case exception: Exception =>
            pagerDutyLog(FAILED_TO_CREATE_DATA, s"$start Failed to create user data. Exception: ${exception.getMessage}")
            Left(DataNotUpdated)
        }
    }
  }

  def find[T](taxYear: Int)(implicit user: User[T]): Future[Either[DatabaseError, Option[V]]] = {
    lazy val start = s"[$repoName][find]"

    val userData = collection.findOneAndUpdate(
      filter = filter(user.nino, taxYear),
      update = set("lastUpdated", toBson(DateTime.now(DateTimeZone.UTC))(MongoJodaFormats.dateTimeWrites)),
      options = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
    ).toFutureOption().map {
      case Some(data) => Right(Some(data))
      case None =>
        logger.info(s"$start No CYA data found for user. SessionId: ${user.sessionId}")
        Right(None)
    }.recover {
      case exception: Exception =>
        pagerDutyLog(FAILED_TO_FIND_DATA, s"$start Failed to find user data. Exception: ${exception.getMessage}")
        Left(DataNotFound)
    }

    userData.map {
      case Left(_) => Left(DataNotFound)
      case Right(data) =>
        Try {
          data.map(decryptionMethod)
        }.toEither match {
          case Left(value) => handleEncryptionDecryptionException(value.asInstanceOf[Exception], start)
          case Right(value) => Right(value)
        }
    }

  }

  def update(userData: V): Future[Either[DatabaseError, Boolean]] = {
    lazy val start = s"[$repoName][update]"

    Try {
      encryptionMethod.apply(userData)
    }.toOption match {
      case None => Future.failed(new Exception(DataNotUpdated.message))
      case Some(exception: Exception) => Future.successful(handleEncryptionDecryptionException(exception, start))
      case Some(encryptedData) =>
        collection.findOneAndReplace(
          filter = filter(encryptedData.nino, encryptedData.taxYear),
          replacement = encryptedData,
          options = FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER)
        ).toFutureOption().map {
          case Some(_) => Right(true)
          case None => Left(DataNotUpdated)
        }.recover {
          case exception: Exception =>
            pagerDutyLog(FAILED_TO_UPDATE_DATA, s"$start Failed to update user data. Exception: ${exception.getMessage}")
            Left(DataNotUpdated)
        }
    }
  }

  def clear(taxYear: Int)(implicit user: User[_]): Future[Boolean] = collection.deleteOne(
    filter = filter(user.nino, taxYear)
  ).toFutureOption().map(_.isDefined)

  def filter(nino: String, taxYear: Int): Bson = and(
    equal("nino", toBson(nino)),
    equal("taxYear", toBson(taxYear))
  )

  private def handleEncryptionDecryptionException[T](exception: Exception, startOfMessage: String): Left[DatabaseError, T] = {
    pagerDutyLog(ENCRYPTION_DECRYPTION_ERROR, s"$startOfMessage ${exception.getMessage}")
    Left(EncryptionDecryptionError(exception.getMessage))
  }


}
