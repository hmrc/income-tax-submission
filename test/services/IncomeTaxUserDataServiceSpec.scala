/*
 * Copyright 2021 HM Revenue & Customs
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

import models.{IncomeSourcesResponseModel, User, _}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.Json
import play.api.mvc.Results._
import utils.{MockIncomeTaxUserDataRepository, TestUtils}

class IncomeTaxUserDataServiceSpec extends TestUtils with MockIncomeTaxUserDataRepository{

  val service: IncomeTaxUserDataService = new IncomeTaxUserDataService(mockRepository,mockAppConfig)

  implicit val user = User(mtditid = "1234567890", arn = None, nino = "AA123456A", sessionId = "sessionId-1618a1e8-4979-41d8-a32e-5ffbe69fac81")

  ".findUserData" should {

    "return the repo response when there is no data" in {

      mockFind(None)

      val result = await(service.findUserData(
        user, 2022
      ))

      result mustBe None
    }
    "return the repo response when there is data" in {

      mockFind(Some(userData))

      val result = await(service.findUserData(
        user, 2022
      ))

      result mustBe Some(userData.toIncomeSourcesResponseModel)
    }
  }

  ".saveUserData" should {

    "return the repo response" in {

      mockUpdate()

      val result = await(service.saveUserData(
        2022
      )(NoContent))

      result mustBe NoContent
    }
    "return the repo response when saving all income sources" in {

      val incomeData = IncomeSourcesResponseModel(
        dividendsModel, interestsModel, Some(giftAidModel), Some(employmentsModel)
      )

      mockUpdate()

      val result = await(service.saveUserData(
        2022,
        Some(incomeData)
      )(Ok(Json.toJson(incomeData))))

      result mustBe Ok(Json.toJson(incomeData))
    }
    "return the repo response when it fails to save" in {

      mockUpdate(false)

      val result = await(service.saveUserData(
        2022
      )(NoContent))

      val error = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("FAILED_TO_SAVE_USER_DATA", "Failed to save user data"))
      Status(error.status)(error.toJson)

      result mustBe Status(error.status)(error.toJson)
    }

  }

}
