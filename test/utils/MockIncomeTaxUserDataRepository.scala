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
import org.scalamock.handlers.{CallHandler1, CallHandler2}
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import repositories.IncomeTaxUserDataRepository

import scala.concurrent.Future

trait MockIncomeTaxUserDataRepository extends MockFactory { _: TestSuite =>

  val mockRepository: IncomeTaxUserDataRepository = mock[IncomeTaxUserDataRepository]

  def mockUpdate(response: Either[DatabaseError, Unit] = Right(())): CallHandler1[UserData, Future[Either[DatabaseError, Unit]]] = {
    (mockRepository.update(_: UserData))
      .expects(*)
      .returns(Future.successful(response))
      .anyNumberOfTimes()
  }

  def mockFind(data: Either[DatabaseError, Option[UserData]]): CallHandler2[User[_], Int, Future[Either[DatabaseError, Option[UserData]]]] = {
    (mockRepository.find(_: User[_], _: Int))
      .expects(*, *)
      .returns(Future.successful(data))
      .anyNumberOfTimes()
  }
}
