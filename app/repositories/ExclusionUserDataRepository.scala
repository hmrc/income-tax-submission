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

import com.google.inject.ImplementedBy
import config.AppConfig
import models.User
import models.mongo.{DatabaseError, EncryptedExclusionUserDataModel, ExclusionUserDataModel}
import services.ExclusionEncryptionService
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ExclusionUserDataRepositoryImpl @Inject()(
                                             mongo: MongoComponent,
                                             appConfig: AppConfig,
                                             encryptionService: ExclusionEncryptionService
                                           )(implicit val ec: ExecutionContext) extends PlayMongoRepository[EncryptedExclusionUserDataModel](
  mongoComponent = mongo,
  collectionName = "exclusionUserData",
  domainFormat = EncryptedExclusionUserDataModel.formats,
  indexes = ExclusionRepositoryIndexes.indexes()(appConfig)
) with ExclusionUserDataRepository with UserDataRepository[EncryptedExclusionUserDataModel, ExclusionUserDataModel] {
  override val repoName = "exclusionUserData"
  override def encryptionMethod: ExclusionUserDataModel => EncryptedExclusionUserDataModel = encryptionService.encryptExclusionUserData
  override def decryptionMethod: EncryptedExclusionUserDataModel => ExclusionUserDataModel = encryptionService.decryptExclusionUserData
}

@ImplementedBy(classOf[ExclusionUserDataRepositoryImpl])
trait ExclusionUserDataRepository {
  def create[T](userData: ExclusionUserDataModel)(implicit user: User[T]): Future[Either[DatabaseError, Boolean]]
  def find[T](taxYear: Int)(implicit user: User[T]): Future[Either[DatabaseError, Option[ExclusionUserDataModel]]]
  def update(userData: ExclusionUserDataModel): Future[Either[DatabaseError, Boolean]]
}
