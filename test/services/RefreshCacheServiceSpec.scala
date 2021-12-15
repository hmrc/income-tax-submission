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

import models.{APIErrorBodyModel, APIErrorModel, IncomeSourcesResponseModel, User}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.mvc.{AnyContent, Result}
import play.api.mvc.Results.{InternalServerError, NoContent}
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtils

import scala.concurrent.{ExecutionContext, Future}

class RefreshCacheServiceSpec extends TestUtils {

  val getIncomeSourcesService: GetIncomeSourcesService = mock[GetIncomeSourcesService]
  val incomeTaxUserDataService: IncomeTaxUserDataService = mock[IncomeTaxUserDataService]

  val taxYear = 2022
  val service: RefreshCacheService = new RefreshCacheService(getIncomeSourcesService,incomeTaxUserDataService,mockAppConfig)

  implicit val user: User[AnyContent] = User(mtditid = "1234567890", arn = None, nino = "AA123456A",
    sessionId = "sessionId-1618a1e8-4979-41d8-a32e-5ffbe69fac81")

  ".getLatestDataAndRefreshCache" should {

    "return an error when get call errors" in {

      (getIncomeSourcesService.getDividends(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("Failed","Reason")))))

      val result = await(service.getLatestDataAndRefreshCache(taxYear,"dividends"))

      result.header.status mustBe INTERNAL_SERVER_ERROR

    }
    "return an error when find data from DB errors" in {

      (getIncomeSourcesService.getDividends(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(dividendsModel)))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("Failed","Reason")))))

      val result = await(service.getLatestDataAndRefreshCache(taxYear,"dividends"))

      result.header.status mustBe INTERNAL_SERVER_ERROR

    }
    "return an error when save data errors" in {

      (getIncomeSourcesService.getDividends(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(dividendsModel)))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(incomeSourcesResponse))))

      val expected = Some(incomeSourcesResponse.copy(dividends = dividendsModel))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSourcesResponseModel])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(InternalServerError))

      val result = await(service.getLatestDataAndRefreshCache(taxYear,"dividends"))

      result.header.status mustBe INTERNAL_SERVER_ERROR

    }
    "get the latest dividends and update the data" in {

      (getIncomeSourcesService.getDividends(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(dividendsModel)))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(incomeSourcesResponse))))

      val expected = Some(incomeSourcesResponse.copy(dividends = dividendsModel))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSourcesResponseModel])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(NoContent))

      val result = await(service.getLatestDataAndRefreshCache(taxYear,"dividends"))

      result mustBe NoContent
    }
    "get the latest interest and update the data" in {

      (getIncomeSourcesService.getInterest(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(interestsModel)))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(incomeSourcesResponse))))

      val expected = Some(incomeSourcesResponse.copy(interest = interestsModel))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSourcesResponseModel])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(NoContent))

      val result = await(service.getLatestDataAndRefreshCache(taxYear,"interest"))

      result mustBe NoContent
    }
    "get the latest interest and update the data when no interest data" in {

      (getIncomeSourcesService.getInterest(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(None)))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(incomeSourcesResponse))))

      val expected = Some(incomeSourcesResponse.copy(interest = None))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSourcesResponseModel])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(NoContent))

      val result = await(service.getLatestDataAndRefreshCache(taxYear,"interest"))

      result mustBe NoContent
    }
    "get the latest interest and update the data when no interest data in session as well as in the get" in {

      (getIncomeSourcesService.getInterest(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(None)))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(incomeSourcesResponse.copy(interest = None)))))

      val expected = Some(incomeSourcesResponse.copy(interest = None))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSourcesResponseModel])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(NoContent))

      val result = await(service.getLatestDataAndRefreshCache(taxYear,"interest"))

      result mustBe NoContent
    }
    "get the latest gift aid and update the data" in {

      (getIncomeSourcesService.getGiftAid(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(Some(giftAidModel.copy(gifts = giftsModel.map(_.copy(sharesOrSecurities = Some(43534555.56))))))))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(incomeSourcesResponse))))

      val expected = Some(incomeSourcesResponse.copy(giftAid = Some(giftAidModel.copy(gifts = giftsModel.map(_.copy(sharesOrSecurities = Some(43534555.56)))))))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSourcesResponseModel])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(NoContent))

      val result = await(service.getLatestDataAndRefreshCache(taxYear,"gift-aid"))

      result mustBe NoContent
    }
    "get the latest gift aid and update the data when no gift aid data" in {

      (getIncomeSourcesService.getGiftAid(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(None)))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(incomeSourcesResponse))))

      val expected = Some(incomeSourcesResponse.copy(giftAid = None))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSourcesResponseModel])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(NoContent))

      val result = await(service.getLatestDataAndRefreshCache(taxYear,"gift-aid"))

      result mustBe NoContent
    }
    "get the latest gift aid and update the data when no gift aid data in session as well as in the get" in {

      (getIncomeSourcesService.getGiftAid(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(None)))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(incomeSourcesResponse.copy(giftAid = None)))))

      val expected = Some(incomeSourcesResponse.copy(giftAid = None))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSourcesResponseModel])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(NoContent))

      val result = await(service.getLatestDataAndRefreshCache(taxYear,"gift-aid"))

      result mustBe NoContent
    }
    "get the latest employment and update the data" in {

      (getIncomeSourcesService.getEmployment(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(Some(allEmploymentData.copy(customerEmploymentData = Seq())))))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(incomeSourcesResponse))))

      val expected = Some(incomeSourcesResponse.copy(employment = Some(allEmploymentData.copy(customerEmploymentData = Seq()))))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSourcesResponseModel])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(NoContent))

      val result = await(service.getLatestDataAndRefreshCache(taxYear,"employment"))

      result mustBe NoContent
    }
    "get the latest employment and update the data when no employment data" in {

      (getIncomeSourcesService.getEmployment(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(None)))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(incomeSourcesResponse))))

      val expected = Some(incomeSourcesResponse.copy(employment =None))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSourcesResponseModel])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(NoContent))

      val result = await(service.getLatestDataAndRefreshCache(taxYear,"employment"))

      result mustBe NoContent
    }
    "get the latest employment and update the data when no employment data in session as well as in the get" in {

      (getIncomeSourcesService.getEmployment(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(None)))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(incomeSourcesResponse.copy(employment = None)))))

      val expected = Some(incomeSourcesResponse.copy(employment =None))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSourcesResponseModel])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(NoContent))

      val result = await(service.getLatestDataAndRefreshCache(taxYear,"employment"))

      result mustBe NoContent
    }
  }

}
