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

package services

import builders.models.DividendsBuilder.aDividends
import builders.models.IncomeSourcesBuilder.anIncomeSources
import builders.models.InterestBuilder.anInterest
import builders.models.SavingsIncomeBuilder
import builders.models.cis.AllCISDeductionsBuilder
import builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import builders.models.gains.GainsBuilder
import builders.models.gifts.GiftAidBuilder.aGiftAid
import builders.models.gifts.GiftsBuilder.aGifts
import builders.models.pensions.PensionsBuilder.aPensions
import builders.models.statebenefits.AllStateBenefitsDataBuilder
import common.IncomeSources.OTHER_EMPLOYMENT_INCOME
import models.{APIErrorBodyModel, APIErrorModel, IncomeSources, User}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.mvc.Results.{InternalServerError, NoContent}
import play.api.mvc.{AnyContent, Result}
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtils

import scala.concurrent.{ExecutionContext, Future}

class RefreshCacheServiceSpec extends TestUtils {

  private val taxYear = 2022

  private implicit val user: User[AnyContent] = User(mtditid = "1234567890", arn = None, nino = "AA123456A",
    sessionId = "sessionId-1618a1e8-4979-41d8-a32e-5ffbe69fac81")

  private val getIncomeSourcesService = mock[GetIncomeSourcesService]
  private val incomeTaxUserDataService = mock[IncomeTaxUserDataService]

  private val underTest = new RefreshCacheService(getIncomeSourcesService, incomeTaxUserDataService, mockAppConfig)

  ".getLatestDataAndRefreshCache" should {
    "return an error when get call errors" in {
      (getIncomeSourcesService.getDividends(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("Failed", "Reason")))))

      val result = await(underTest.getLatestDataAndRefreshCache(taxYear, "dividends"))

      result.header.status mustBe INTERNAL_SERVER_ERROR
    }

    "return an error when find data from DB errors" in {
      (getIncomeSourcesService.getDividends(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(Some(aDividends))))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("Failed", "Reason")))))

      val result = await(underTest.getLatestDataAndRefreshCache(taxYear, "dividends"))

      result.header.status mustBe INTERNAL_SERVER_ERROR
    }

    "return an error when save data errors" in {
      (getIncomeSourcesService.getDividends(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(Some(aDividends))))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(dividends = Some(aDividends)))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSources])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(InternalServerError))

      val result = await(underTest.getLatestDataAndRefreshCache(taxYear, "dividends"))

      result.header.status mustBe INTERNAL_SERVER_ERROR
    }

    "get the latest dividends and update the data" in {
      (getIncomeSourcesService.getDividends(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(Some(aDividends))))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(dividends = Some(aDividends)))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSources])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "dividends")) mustBe NoContent
    }

    "get the latest interest and update the data" in {
      (getIncomeSourcesService.getInterest(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(Some(List(anInterest)))))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(interest = Some(Seq(anInterest))))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSources])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "interest")) mustBe NoContent
    }

    "get the latest interest and update the data when no interest data" in {
      (getIncomeSourcesService.getInterest(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(None)))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(interest = None))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSources])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "interest")) mustBe NoContent
    }

    "get the latest interest and update the data when no interest data in session as well as in the get" in {
      (getIncomeSourcesService.getInterest(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(None)))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(anIncomeSources.copy(interest = None)))))

      val expected = Some(anIncomeSources.copy(interest = None))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSources])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "interest")) mustBe NoContent
    }

    "get the latest gift aid and update the data" in {
      (getIncomeSourcesService.getGiftAid(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(Some(aGiftAid.copy(gifts = Some(aGifts.copy(sharesOrSecurities = Some(43534555.56))))))))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(giftAid = Some(aGiftAid.copy(gifts = Some(aGifts.copy(sharesOrSecurities = Some(43534555.56)))))))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSources])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "gift-aid")) mustBe NoContent
    }

    "get the latest gift aid and update the data when no gift aid data" in {
      (getIncomeSourcesService.getGiftAid(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(None)))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(giftAid = None))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSources])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "gift-aid")) mustBe NoContent
    }

    "get the latest gift aid and update the data when no gift aid data in session as well as in the get" in {
      (getIncomeSourcesService.getGiftAid(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(None)))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(anIncomeSources.copy(giftAid = None)))))

      val expected = Some(anIncomeSources.copy(giftAid = None))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSources])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "gift-aid")) mustBe NoContent
    }

    "get the latest employment and update the data" in {
      (getIncomeSourcesService.getEmployment(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(Some(anAllEmploymentData.copy(customerEmploymentData = Seq())))))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(employment = Some(anAllEmploymentData.copy(customerEmploymentData = Seq()))))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSources])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "employment")) mustBe NoContent
    }

    "get the latest employment and update the data when no employment data" in {
      (getIncomeSourcesService.getEmployment(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(None)))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(employment = None))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSources])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "employment")) mustBe NoContent
    }

    "get the latest employment and update the data when no employment data in session as well as in the get" in {
      (getIncomeSourcesService.getEmployment(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(None)))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(anIncomeSources.copy(employment = None)))))

      val expected = Some(anIncomeSources.copy(employment = None))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSources])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "employment")) mustBe NoContent
    }

    "get the latest pensions and update the data" in {
      (getIncomeSourcesService.getPensions(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(Some(aPensions.copy(pensionCharges = None)))))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(pensions = Some(aPensions.copy(pensionCharges = None))))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSources])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "pensions")) mustBe NoContent
    }

    "get the latest pensions and update the data when no pensions data" in {
      (getIncomeSourcesService.getPensions(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(None)))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(pensions = None))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSources])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "pensions")) mustBe NoContent
    }

    "get the latest pensions and update the data when no pensions data in session or in the get" in {
      (getIncomeSourcesService.getPensions(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(None)))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(anIncomeSources.copy(pensions = None)))))

      val expected = Some(anIncomeSources.copy(pensions = None))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSources])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "pensions")) mustBe NoContent
    }

    "get the latest cis and update the data" in {
      (getIncomeSourcesService.getCIS(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(Some(AllCISDeductionsBuilder.anAllCISDeductions))))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(cis = Some(AllCISDeductionsBuilder.anAllCISDeductions)))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSources])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "cis")) mustBe NoContent
    }

    "get the latest cis and update the data when no cis data" in {
      (getIncomeSourcesService.getCIS(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(None)))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(cis = None))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSources])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "cis")) mustBe NoContent
    }

    "get the latest cis and update the data when no cis data in session or in the get" in {
      (getIncomeSourcesService.getCIS(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(None)))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(anIncomeSources.copy(cis = None)))))

      val expected = Some(anIncomeSources.copy(cis = None))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSources])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "cis")) mustBe NoContent
    }

    "get the latest state-benefits and update the data" in {
      (getIncomeSourcesService.getStateBenefits(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(Some(AllStateBenefitsDataBuilder.anAllStateBenefitsData))))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(stateBenefits = Some(AllStateBenefitsDataBuilder.anAllStateBenefitsData)))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSources])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "state-benefits")) mustBe NoContent
    }

    "get the latest state-benefits and update the data when no state-benefits data" in {
      (getIncomeSourcesService.getStateBenefits(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(None)))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(stateBenefits = None))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSources])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "state-benefits")) mustBe NoContent
    }

    "get the latest state-benefits and update the data when no state-benefits data in session or in the get" in {
      (getIncomeSourcesService.getStateBenefits(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(None)))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(anIncomeSources.copy(stateBenefits = None)))))

      val expected = Some(anIncomeSources.copy(stateBenefits = None))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSources])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "state-benefits")) mustBe NoContent
    }

    "get the latest interest-savings and update the data" in {
      (getIncomeSourcesService.getSavingsInterest(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(Some(SavingsIncomeBuilder.anSavingIncome))))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(interestSavings = Some(SavingsIncomeBuilder.anSavingIncome)))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSources])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "interest-savings")) mustBe NoContent
    }

    "get the latest interest-savings and update the data when no interest-savings data" in {
      (getIncomeSourcesService.getSavingsInterest(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(None)))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(interestSavings = None))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSources])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "interest-savings")) mustBe NoContent
    }

    "get the latest interest-savings and update the data when no interest-savings data in session or in the get" in {
      (getIncomeSourcesService.getSavingsInterest(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(None)))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(anIncomeSources.copy(interestSavings = None)))))

      val expected = Some(anIncomeSources.copy(interestSavings = None))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSources])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "interest-savings")) mustBe NoContent
    }

    "get the latest gains and update the data" in {
      (getIncomeSourcesService.getGains(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(Some(GainsBuilder.anGains))))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(gains = Some(GainsBuilder.anGains)))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSources])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "gains")) mustBe NoContent
    }

    "get the latest gains and update the data when no gains data" in {
      (getIncomeSourcesService.getGains(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(None)))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(gains = None))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSources])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "gains")) mustBe NoContent
    }

    "get the latest gains and update the data when no gains data in session or in the get" in {
      (getIncomeSourcesService.getGains(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(None)))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(anIncomeSources.copy(gains = None)))))

      val expected = Some(anIncomeSources.copy(gains = None))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSources])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "gains")) mustBe NoContent
    }


    "get the latest other employment income and update the data when no other employment income data in session or in the get" in {
      (getIncomeSourcesService.getOtherEmploymentIncome(_: String, _: Int, _: String, _: Seq[String])(_: HeaderCarrier))
        .expects("AA123456A", taxYear, "1234567890", *, *)
        .returning(Future.successful(Right(None)))

      (incomeTaxUserDataService.findUserData(_: User[_], _: Int)(_: ExecutionContext))
        .expects(user, taxYear, *)
        .returning(Future.successful(Right(Some(anIncomeSources.copy(otherEmploymentIncome = None)))))

      val expected = Some(anIncomeSources.copy(otherEmploymentIncome = None))

      (incomeTaxUserDataService.saveUserData(_: Int, _: Option[IncomeSources])(_: Result)(_: User[_], _: ExecutionContext))
        .expects(taxYear, expected, NoContent, user, *)
        .returning(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, incomeSource = OTHER_EMPLOYMENT_INCOME)) mustBe NoContent
    }

  }
}
