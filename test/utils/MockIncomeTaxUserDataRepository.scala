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

package utils

import models.User
import models.mongo.{DatabaseError, UserData}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import repositories.IncomeTaxUserDataRepository

import scala.concurrent.Future

trait MockIncomeTaxUserDataRepository extends MockitoSugar {

  val mockRepository: IncomeTaxUserDataRepository = mock[IncomeTaxUserDataRepository]

  private def anyUser[A]: User[A] = any[User[Any]].asInstanceOf[User[A]]

  def mockUpdate(response: Either[DatabaseError, Unit] = Right(())): Unit = {
    when(mockRepository.update(any[UserData])).thenReturn(Future.successful(response))
  }

  def mockFind(data: Either[DatabaseError, Option[UserData]]): Unit = {
    when(mockRepository.find(anyUser, any[Int])).thenReturn(Future.successful(data))
  }
}
