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

import builders.models.DividendsBuilder.aDividends
import builders.models.InterestBuilder.anInterest
import builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import builders.models.gifts.GiftAidBuilder.aGiftAid
import builders.models.mongo.UserDataBuilder.aUserData
import builders.models.pensions.PensionsBuilder.aPensions
import models._
import models.mongo.{DataNotFound, DataNotUpdated, EncryptionDecryptionError, MongoError}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.mvc.Results._
import utils.{MockIncomeTaxUserDataRepository, TestUtils}

import scala.concurrent.Future

class IncomeTaxUserDataServiceSpec extends TestUtils with MockIncomeTaxUserDataRepository {

  private val taxYear = 2022
  private implicit val user: User[AnyContentAsEmpty.type] =
    User(mtditid = "1234567890", arn = None, nino = "AA123456A", sessionId = "sessionId-1618a1e8-4979-41d8-a32e-5ffbe69fac81")

  private val underTest: IncomeTaxUserDataService = new IncomeTaxUserDataService(mockRepository, mockAppConfig)

  ".findUserData" should {
    "return the repo response when there is no data" in {
      mockFind(Right(None))

      await(underTest.findUserData(user, taxYear)) mustBe Right(None)
    }

    "return the repo response when there is data" in {
      mockFind(Right(Some(aUserData)))

      await(underTest.findUserData(user, taxYear)) mustBe Right(Some(aUserData.toIncomeSourcesResponseModel))
    }
  }

  ".saveUserData" should {
    "return the repo response" in {
      mockUpdate()

      await(underTest.saveUserData(taxYear)(NoContent)) mustBe NoContent
    }

    "return the repo response when saving all income sources" in {
      val incomeData = IncomeSources(Some(aDividends), Some(Seq(anInterest)),
        Some(aGiftAid), Some(anAllEmploymentData), Some(aPensions))

      mockUpdate()

      await(underTest.saveUserData(taxYear, Some(incomeData))(Ok(Json.toJson(incomeData)))) mustBe Ok(Json.toJson(incomeData))
    }

    "return the repo response when it fails to save" in {
      mockUpdate(Left(DataNotUpdated))

      val error = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("FAILED_TO_SAVE_USER_DATA", "User data was not updated due to mongo exception"))

      val result = await(underTest.saveUserData(taxYear)(NoContent))

      result.header.status mustBe INTERNAL_SERVER_ERROR
      bodyOf(Future.successful(result)) mustBe error.toJson.toString()
    }

    "return the repo response when it fails to save due to mongo exception" in {
      val error = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("FAILED_TO_SAVE_USER_DATA", "Mongo exception occurred. Exception: it failed"))

      mockUpdate(Left(MongoError("it failed")))

      val result = await(underTest.saveUserData(taxYear)(NoContent))

      result.header.status mustBe INTERNAL_SERVER_ERROR
      bodyOf(Future.successful(result)) mustBe error.toJson.toString()
    }

    "return the repo response when it fails to save due to encryption exception" in {
      mockUpdate(Left(EncryptionDecryptionError("it failed")))

      val result = await(underTest.saveUserData(taxYear)(NoContent))

      val error = APIErrorModel(INTERNAL_SERVER_ERROR,
        APIErrorBodyModel("FAILED_TO_SAVE_USER_DATA", "Encryption / Decryption exception occurred. Exception: it failed"))

      result.header.status mustBe INTERNAL_SERVER_ERROR
      bodyOf(Future.successful(result)) mustBe error.toJson.toString()
    }

    "return the repo response when it fails to save due to data not found error" in {
      mockUpdate(Left(DataNotFound))

      val result = await(underTest.saveUserData(taxYear)(NoContent))

      val error = APIErrorModel(INTERNAL_SERVER_ERROR,
        APIErrorBodyModel("FAILED_TO_SAVE_USER_DATA", "User data could not be found due to mongo exception"))

      result.header.status mustBe INTERNAL_SERVER_ERROR
      bodyOf(Future.successful(result)) mustBe error.toJson.toString()
    }
  }
}
