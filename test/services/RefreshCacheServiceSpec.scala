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

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when

import builders.models.DividendsBuilder.aDividends
import builders.models.IncomeSourcesBuilder.anIncomeSources
import builders.models.InterestBuilder.anInterest
import builders.models.SavingsIncomeBuilder
import builders.models.StockDividendsBuilder.anStockDividends
import builders.models.cis.AllCISDeductionsBuilder
import builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentData
import builders.models.gains.GainsBuilder
import builders.models.gifts.GiftAidBuilder.aGiftAid
import builders.models.gifts.GiftsBuilder.aGifts
import builders.models.pensions.PensionsBuilder.aPensions
import builders.models.statebenefits.AllStateBenefitsDataBuilder
import models.{APIErrorBodyModel, APIErrorModel, IncomeSources, User}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.mvc.Results.{InternalServerError, NoContent}
import play.api.mvc.{AnyContent, Result}
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtils

import scala.concurrent.{ExecutionContext, Future}

class RefreshCacheServiceSpec extends TestUtils {

  private val nino = "AA123456A"
  private val mtditid = "1234567890"
  private val taxYear = 2022

  private implicit val user: User[AnyContent] = User(mtditid = mtditid, arn = None, nino = nino,
    sessionId = "sessionId-1618a1e8-4979-41d8-a32e-5ffbe69fac81")(fakeRequest)

  private val getIncomeSourcesService = mock[GetIncomeSourcesService]
  private val incomeTaxUserDataService = mock[IncomeTaxUserDataService]

  private val underTest = new RefreshCacheService(getIncomeSourcesService, incomeTaxUserDataService)

  ".getLatestDataAndRefreshCache" should {
    "return an error when get call errors" in {
      when(getIncomeSourcesService.getDividends(eqTo(nino), eqTo(taxYear), eqTo(mtditid), any[Seq[String]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("Failed", "Reason")))))

      val result = await(underTest.getLatestDataAndRefreshCache(taxYear, "dividends"))

      result.header.status mustBe INTERNAL_SERVER_ERROR
    }

    "return an error when find data from DB errors" in {
      when(getIncomeSourcesService.getDividends(eqTo(nino), eqTo(taxYear), eqTo(mtditid), any[Seq[String]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(Some(aDividends))))

      when(incomeTaxUserDataService.findUserData(anyUser, eqTo(taxYear))(any[ExecutionContext]))
        .thenReturn(Future.successful(Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("Failed", "Reason")))))

      val result = await(underTest.getLatestDataAndRefreshCache(taxYear, "dividends"))

      result.header.status mustBe INTERNAL_SERVER_ERROR
    }

    "return an error when save data errors" in {
      when(getIncomeSourcesService.getDividends(eqTo(nino), eqTo(taxYear), eqTo(mtditid), any[Seq[String]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(Some(aDividends))))

      when(incomeTaxUserDataService.findUserData(anyUser, eqTo(taxYear))(any[ExecutionContext]))
        .thenReturn(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(dividends = Some(aDividends)))

      when(incomeTaxUserDataService.saveUserData(eqTo(taxYear), any[Option[IncomeSources]])(any[Result])(anyUser, any[ExecutionContext]))
        .thenReturn(Future.successful(InternalServerError))

      val result = await(underTest.getLatestDataAndRefreshCache(taxYear, "dividends"))

      result.header.status mustBe INTERNAL_SERVER_ERROR
    }

    "get the latest dividends and update the data" in {
      when(getIncomeSourcesService.getDividends(eqTo(nino), eqTo(taxYear), eqTo(mtditid), any[Seq[String]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(Some(aDividends))))

      when(incomeTaxUserDataService.findUserData(anyUser, eqTo(taxYear))(any[ExecutionContext]))
        .thenReturn(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(dividends = Some(aDividends)))

      when(incomeTaxUserDataService.saveUserData(eqTo(taxYear), any[Option[IncomeSources]])(any[Result])(anyUser, any[ExecutionContext]))
        .thenReturn(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "dividends")) mustBe NoContent
    }

    "get the latest dividends and update the data when no dividends data" in {
      when(getIncomeSourcesService.getDividends(eqTo(nino), eqTo(taxYear), eqTo(mtditid), any[Seq[String]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(None)))

      when(incomeTaxUserDataService.findUserData(anyUser, eqTo(taxYear))(any[ExecutionContext]))
        .thenReturn(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(dividends = None))

      when(incomeTaxUserDataService.saveUserData(eqTo(taxYear), any[Option[IncomeSources]])(any[Result])(anyUser, any[ExecutionContext]))
        .thenReturn(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "dividends")) mustBe NoContent
    }

    "get the latest dividends and update the data when no dividends data in session as well as in get" in {
      when(getIncomeSourcesService.getDividends(eqTo(nino), eqTo(taxYear), eqTo(mtditid), any[Seq[String]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(None)))

      when(incomeTaxUserDataService.findUserData(anyUser, eqTo(taxYear))(any[ExecutionContext]))
        .thenReturn(Future.successful(Right(Some(anIncomeSources.copy(dividends = None)))))

      val expected = Some(anIncomeSources.copy(dividends = None))

      when(incomeTaxUserDataService.saveUserData(eqTo(taxYear), any[Option[IncomeSources]])(any[Result])(anyUser, any[ExecutionContext]))
        .thenReturn(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "dividends")) mustBe NoContent
    }

    "return an error when get call errors for stock dividends" in {
      when(getIncomeSourcesService.getStockDividends(eqTo(nino), eqTo(taxYear), eqTo(mtditid), any[Seq[String]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("Failed", "Reason")))))

      val result = await(underTest.getLatestDataAndRefreshCache(taxYear, "stock-dividends"))

      result.header.status mustBe INTERNAL_SERVER_ERROR
    }

    "return an error when find data from DB errors for stock dividends" in {
      when(getIncomeSourcesService.getStockDividends(eqTo(nino), eqTo(taxYear), eqTo(mtditid), any[Seq[String]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(Some(anStockDividends))))

      when(incomeTaxUserDataService.findUserData(anyUser, eqTo(taxYear))(any[ExecutionContext]))
        .thenReturn(Future.successful(Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("Failed", "Reason")))))

      val result = await(underTest.getLatestDataAndRefreshCache(taxYear, "stock-dividends"))

      result.header.status mustBe INTERNAL_SERVER_ERROR
    }

    "return an error when save data errors for stock dividends" in {
      when(getIncomeSourcesService.getStockDividends(eqTo(nino), eqTo(taxYear), eqTo(mtditid), any[Seq[String]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(Some(anStockDividends))))

      when(incomeTaxUserDataService.findUserData(anyUser, eqTo(taxYear))(any[ExecutionContext]))
        .thenReturn(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(stockDividends = Some(anStockDividends)))

      when(incomeTaxUserDataService.saveUserData(eqTo(taxYear), any[Option[IncomeSources]])(any[Result])(anyUser, any[ExecutionContext]))
        .thenReturn(Future.successful(InternalServerError))

      val result = await(underTest.getLatestDataAndRefreshCache(taxYear, "stock-dividends"))

      result.header.status mustBe INTERNAL_SERVER_ERROR
    }

    "get the latest stock dividends and update the data" in {
      when(getIncomeSourcesService.getStockDividends(eqTo(nino), eqTo(taxYear), eqTo(mtditid), any[Seq[String]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(Some(anStockDividends))))

      when(incomeTaxUserDataService.findUserData(anyUser, eqTo(taxYear))(any[ExecutionContext]))
        .thenReturn(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(stockDividends = Some(anStockDividends)))

      when(incomeTaxUserDataService.saveUserData(eqTo(taxYear), any[Option[IncomeSources]])(any[Result])(anyUser, any[ExecutionContext]))
        .thenReturn(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "stock-dividends")) mustBe NoContent
    }

    "get the latest stock dividends and update the data when no stock dividends data" in {
      when(getIncomeSourcesService.getStockDividends(eqTo(nino), eqTo(taxYear), eqTo(mtditid), any[Seq[String]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(None)))

      when(incomeTaxUserDataService.findUserData(anyUser, eqTo(taxYear))(any[ExecutionContext]))
        .thenReturn(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(stockDividends = None))

      when(incomeTaxUserDataService.saveUserData(eqTo(taxYear), any[Option[IncomeSources]])(any[Result])(anyUser, any[ExecutionContext]))
        .thenReturn(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "stock-dividends")) mustBe NoContent
    }

    "get the latest stock dividends and update the data when no stock dividends data in session as well as in get" in {
      when(getIncomeSourcesService.getStockDividends(eqTo(nino), eqTo(taxYear), eqTo(mtditid), any[Seq[String]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(None)))

      when(incomeTaxUserDataService.findUserData(anyUser, eqTo(taxYear))(any[ExecutionContext]))
        .thenReturn(Future.successful(Right(Some(anIncomeSources.copy(stockDividends = None)))))

      val expected = Some(anIncomeSources.copy(stockDividends = None))

      when(incomeTaxUserDataService.saveUserData(eqTo(taxYear), any[Option[IncomeSources]])(any[Result])(anyUser, any[ExecutionContext]))
        .thenReturn(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "stock-dividends")) mustBe NoContent
    }

    "get the latest interest and update the data" in {
      when(getIncomeSourcesService.getInterest(eqTo(nino), eqTo(taxYear), eqTo(mtditid), any[Seq[String]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(Some(List(anInterest)))))

      when(incomeTaxUserDataService.findUserData(anyUser, eqTo(taxYear))(any[ExecutionContext]))
        .thenReturn(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(interest = Some(Seq(anInterest))))

      when(incomeTaxUserDataService.saveUserData(eqTo(taxYear), any[Option[IncomeSources]])(any[Result])(anyUser, any[ExecutionContext]))
        .thenReturn(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "interest")) mustBe NoContent
    }

    "get the latest interest and update the data when no interest data" in {
      when(getIncomeSourcesService.getInterest(eqTo(nino), eqTo(taxYear), eqTo(mtditid), any[Seq[String]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(None)))

      when(incomeTaxUserDataService.findUserData(anyUser, eqTo(taxYear))(any[ExecutionContext]))
        .thenReturn(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(interest = None))

      when(incomeTaxUserDataService.saveUserData(eqTo(taxYear), any[Option[IncomeSources]])(any[Result])(anyUser, any[ExecutionContext]))
        .thenReturn(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "interest")) mustBe NoContent
    }

    "get the latest interest and update the data when no interest data in session as well as in the get" in {
      when(getIncomeSourcesService.getInterest(eqTo(nino), eqTo(taxYear), eqTo(mtditid), any[Seq[String]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(None)))

      when(incomeTaxUserDataService.findUserData(anyUser, eqTo(taxYear))(any[ExecutionContext]))
        .thenReturn(Future.successful(Right(Some(anIncomeSources.copy(interest = None)))))

      val expected = Some(anIncomeSources.copy(interest = None))

      when(incomeTaxUserDataService.saveUserData(eqTo(taxYear), any[Option[IncomeSources]])(any[Result])(anyUser, any[ExecutionContext]))
        .thenReturn(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "interest")) mustBe NoContent
    }

    "get the latest gift aid and update the data" in {
      when(getIncomeSourcesService.getGiftAid(eqTo(nino), eqTo(taxYear), eqTo(mtditid), any[Seq[String]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(Some(aGiftAid.copy(gifts = Some(aGifts.copy(sharesOrSecurities = Some(43534555.56))))))))

      when(incomeTaxUserDataService.findUserData(anyUser, eqTo(taxYear))(any[ExecutionContext]))
        .thenReturn(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(giftAid = Some(aGiftAid.copy(gifts = Some(aGifts.copy(sharesOrSecurities = Some(43534555.56)))))))

      when(incomeTaxUserDataService.saveUserData(eqTo(taxYear), any[Option[IncomeSources]])(any[Result])(anyUser, any[ExecutionContext]))
        .thenReturn(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "gift-aid")) mustBe NoContent
    }

    "get the latest gift aid and update the data when no gift aid data" in {
      when(getIncomeSourcesService.getGiftAid(eqTo(nino), eqTo(taxYear), eqTo(mtditid), any[Seq[String]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(None)))

      when(incomeTaxUserDataService.findUserData(anyUser, eqTo(taxYear))(any[ExecutionContext]))
        .thenReturn(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(giftAid = None))

      when(incomeTaxUserDataService.saveUserData(eqTo(taxYear), any[Option[IncomeSources]])(any[Result])(anyUser, any[ExecutionContext]))
        .thenReturn(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "gift-aid")) mustBe NoContent
    }

    "get the latest gift aid and update the data when no gift aid data in session as well as in the get" in {
      when(getIncomeSourcesService.getGiftAid(eqTo(nino), eqTo(taxYear), eqTo(mtditid), any[Seq[String]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(None)))

      when(incomeTaxUserDataService.findUserData(anyUser, eqTo(taxYear))(any[ExecutionContext]))
        .thenReturn(Future.successful(Right(Some(anIncomeSources.copy(giftAid = None)))))

      val expected = Some(anIncomeSources.copy(giftAid = None))

      when(incomeTaxUserDataService.saveUserData(eqTo(taxYear), any[Option[IncomeSources]])(any[Result])(anyUser, any[ExecutionContext]))
        .thenReturn(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "gift-aid")) mustBe NoContent
    }

    "get the latest employment and update the data" in {
      when(getIncomeSourcesService.getEmployment(eqTo(nino), eqTo(taxYear), eqTo(mtditid), any[Seq[String]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(Some(anAllEmploymentData.copy(customerEmploymentData = Seq())))))

      when(incomeTaxUserDataService.findUserData(anyUser, eqTo(taxYear))(any[ExecutionContext]))
        .thenReturn(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(employment = Some(anAllEmploymentData.copy(customerEmploymentData = Seq()))))

      when(incomeTaxUserDataService.saveUserData(eqTo(taxYear), any[Option[IncomeSources]])(any[Result])(anyUser, any[ExecutionContext]))
        .thenReturn(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "employment")) mustBe NoContent
    }

    "get the latest employment and update the data when no employment data" in {
      when(getIncomeSourcesService.getEmployment(eqTo(nino), eqTo(taxYear), eqTo(mtditid), any[Seq[String]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(None)))

      when(incomeTaxUserDataService.findUserData(anyUser, eqTo(taxYear))(any[ExecutionContext]))
        .thenReturn(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(employment = None))

      when(incomeTaxUserDataService.saveUserData(eqTo(taxYear), any[Option[IncomeSources]])(any[Result])(anyUser, any[ExecutionContext]))
        .thenReturn(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "employment")) mustBe NoContent
    }

    "get the latest employment and update the data when no employment data in session as well as in the get" in {
      when(getIncomeSourcesService.getEmployment(eqTo(nino), eqTo(taxYear), eqTo(mtditid), any[Seq[String]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(None)))

      when(incomeTaxUserDataService.findUserData(anyUser, eqTo(taxYear))(any[ExecutionContext]))
        .thenReturn(Future.successful(Right(Some(anIncomeSources.copy(employment = None)))))

      val expected = Some(anIncomeSources.copy(employment = None))

      when(incomeTaxUserDataService.saveUserData(eqTo(taxYear), any[Option[IncomeSources]])(any[Result])(anyUser, any[ExecutionContext]))
        .thenReturn(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "employment")) mustBe NoContent
    }

    "get the latest pensions and update the data" in {
      when(getIncomeSourcesService.getPensions(eqTo(nino), eqTo(taxYear), eqTo(mtditid), any[Seq[String]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(Some(aPensions.copy(pensionCharges = None)))))

      when(incomeTaxUserDataService.findUserData(anyUser, eqTo(taxYear))(any[ExecutionContext]))
        .thenReturn(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(pensions = Some(aPensions.copy(pensionCharges = None))))

      when(incomeTaxUserDataService.saveUserData(eqTo(taxYear), any[Option[IncomeSources]])(any[Result])(anyUser, any[ExecutionContext]))
        .thenReturn(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "pensions")) mustBe NoContent
    }

    "get the latest pensions and update the data when no pensions data" in {
      when(getIncomeSourcesService.getPensions(eqTo(nino), eqTo(taxYear), eqTo(mtditid), any[Seq[String]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(None)))

      when(incomeTaxUserDataService.findUserData(anyUser, eqTo(taxYear))(any[ExecutionContext]))
        .thenReturn(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(pensions = None))

      when(incomeTaxUserDataService.saveUserData(eqTo(taxYear), any[Option[IncomeSources]])(any[Result])(anyUser, any[ExecutionContext]))
        .thenReturn(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "pensions")) mustBe NoContent
    }

    "get the latest pensions and update the data when no pensions data in session or in the get" in {
      when(getIncomeSourcesService.getPensions(eqTo(nino), eqTo(taxYear), eqTo(mtditid), any[Seq[String]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(None)))

      when(incomeTaxUserDataService.findUserData(anyUser, eqTo(taxYear))(any[ExecutionContext]))
        .thenReturn(Future.successful(Right(Some(anIncomeSources.copy(pensions = None)))))

      val expected = Some(anIncomeSources.copy(pensions = None))

      when(incomeTaxUserDataService.saveUserData(eqTo(taxYear), any[Option[IncomeSources]])(any[Result])(anyUser, any[ExecutionContext]))
        .thenReturn(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "pensions")) mustBe NoContent
    }

    "get the latest cis and update the data" in {
      when(getIncomeSourcesService.getCIS(eqTo(nino), eqTo(taxYear), eqTo(mtditid), any[Seq[String]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(Some(AllCISDeductionsBuilder.anAllCISDeductions))))

      when(incomeTaxUserDataService.findUserData(anyUser, eqTo(taxYear))(any[ExecutionContext]))
        .thenReturn(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(cis = Some(AllCISDeductionsBuilder.anAllCISDeductions)))

      when(incomeTaxUserDataService.saveUserData(eqTo(taxYear), any[Option[IncomeSources]])(any[Result])(anyUser, any[ExecutionContext]))
        .thenReturn(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "cis")) mustBe NoContent
    }

    "get the latest cis and update the data when no cis data" in {
      when(getIncomeSourcesService.getCIS(eqTo(nino), eqTo(taxYear), eqTo(mtditid), any[Seq[String]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(None)))

      when(incomeTaxUserDataService.findUserData(anyUser, eqTo(taxYear))(any[ExecutionContext]))
        .thenReturn(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(cis = None))

      when(incomeTaxUserDataService.saveUserData(eqTo(taxYear), any[Option[IncomeSources]])(any[Result])(anyUser, any[ExecutionContext]))
        .thenReturn(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "cis")) mustBe NoContent
    }

    "get the latest cis and update the data when no cis data in session or in the get" in {
      when(getIncomeSourcesService.getCIS(eqTo(nino), eqTo(taxYear), eqTo(mtditid), any[Seq[String]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(None)))

      when(incomeTaxUserDataService.findUserData(anyUser, eqTo(taxYear))(any[ExecutionContext]))
        .thenReturn(Future.successful(Right(Some(anIncomeSources.copy(cis = None)))))

      val expected = Some(anIncomeSources.copy(cis = None))

      when(incomeTaxUserDataService.saveUserData(eqTo(taxYear), any[Option[IncomeSources]])(any[Result])(anyUser, any[ExecutionContext]))
        .thenReturn(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "cis")) mustBe NoContent
    }

    "get the latest state-benefits and update the data" in {
      when(getIncomeSourcesService.getStateBenefits(eqTo(nino), eqTo(taxYear), eqTo(mtditid), any[Seq[String]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(Some(AllStateBenefitsDataBuilder.anAllStateBenefitsData))))

      when(incomeTaxUserDataService.findUserData(anyUser, eqTo(taxYear))(any[ExecutionContext]))
        .thenReturn(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(stateBenefits = Some(AllStateBenefitsDataBuilder.anAllStateBenefitsData)))

      when(incomeTaxUserDataService.saveUserData(eqTo(taxYear), any[Option[IncomeSources]])(any[Result])(anyUser, any[ExecutionContext]))
        .thenReturn(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "state-benefits")) mustBe NoContent
    }

    "get the latest state-benefits and update the data when no state-benefits data" in {
      when(getIncomeSourcesService.getStateBenefits(eqTo(nino), eqTo(taxYear), eqTo(mtditid), any[Seq[String]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(None)))

      when(incomeTaxUserDataService.findUserData(anyUser, eqTo(taxYear))(any[ExecutionContext]))
        .thenReturn(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(stateBenefits = None))

      when(incomeTaxUserDataService.saveUserData(eqTo(taxYear), any[Option[IncomeSources]])(any[Result])(anyUser, any[ExecutionContext]))
        .thenReturn(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "state-benefits")) mustBe NoContent
    }

    "get the latest state-benefits and update the data when no state-benefits data in session or in the get" in {
      when(getIncomeSourcesService.getStateBenefits(eqTo(nino), eqTo(taxYear), eqTo(mtditid), any[Seq[String]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(None)))

      when(incomeTaxUserDataService.findUserData(anyUser, eqTo(taxYear))(any[ExecutionContext]))
        .thenReturn(Future.successful(Right(Some(anIncomeSources.copy(stateBenefits = None)))))

      val expected = Some(anIncomeSources.copy(stateBenefits = None))

      when(incomeTaxUserDataService.saveUserData(eqTo(taxYear), any[Option[IncomeSources]])(any[Result])(anyUser, any[ExecutionContext]))
        .thenReturn(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "state-benefits")) mustBe NoContent
    }

    "get the latest interest-savings and update the data" in {
      when(getIncomeSourcesService.getSavingsInterest(eqTo(nino), eqTo(taxYear), eqTo(mtditid), any[Seq[String]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(Some(SavingsIncomeBuilder.anSavingIncome))))

      when(incomeTaxUserDataService.findUserData(anyUser, eqTo(taxYear))(any[ExecutionContext]))
        .thenReturn(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(interestSavings = Some(SavingsIncomeBuilder.anSavingIncome)))

      when(incomeTaxUserDataService.saveUserData(eqTo(taxYear), any[Option[IncomeSources]])(any[Result])(anyUser, any[ExecutionContext]))
        .thenReturn(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "interest-savings")) mustBe NoContent
    }

    "get the latest interest-savings and update the data when no interest-savings data" in {
      when(getIncomeSourcesService.getSavingsInterest(eqTo(nino), eqTo(taxYear), eqTo(mtditid), any[Seq[String]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(None)))

      when(incomeTaxUserDataService.findUserData(anyUser, eqTo(taxYear))(any[ExecutionContext]))
        .thenReturn(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(interestSavings = None))

      when(incomeTaxUserDataService.saveUserData(eqTo(taxYear), any[Option[IncomeSources]])(any[Result])(anyUser, any[ExecutionContext]))
        .thenReturn(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "interest-savings")) mustBe NoContent
    }

    "get the latest interest-savings and update the data when no interest-savings data in session or in the get" in {
      when(getIncomeSourcesService.getSavingsInterest(eqTo(nino), eqTo(taxYear), eqTo(mtditid), any[Seq[String]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(None)))

      when(incomeTaxUserDataService.findUserData(anyUser, eqTo(taxYear))(any[ExecutionContext]))
        .thenReturn(Future.successful(Right(Some(anIncomeSources.copy(interestSavings = None)))))

      val expected = Some(anIncomeSources.copy(interestSavings = None))

      when(incomeTaxUserDataService.saveUserData(eqTo(taxYear), any[Option[IncomeSources]])(any[Result])(anyUser, any[ExecutionContext]))
        .thenReturn(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "interest-savings")) mustBe NoContent
    }

    "get the latest gains and update the data" in {
      when(getIncomeSourcesService.getGains(eqTo(nino), eqTo(taxYear), eqTo(mtditid), any[Seq[String]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(Some(GainsBuilder.anGains))))

      when(incomeTaxUserDataService.findUserData(anyUser, eqTo(taxYear))(any[ExecutionContext]))
        .thenReturn(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(gains = Some(GainsBuilder.anGains)))

      when(incomeTaxUserDataService.saveUserData(eqTo(taxYear), any[Option[IncomeSources]])(any[Result])(anyUser, any[ExecutionContext]))
        .thenReturn(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "gains")) mustBe NoContent
    }

    "get the latest gains and update the data when no gains data" in {
      when(getIncomeSourcesService.getGains(eqTo(nino), eqTo(taxYear), eqTo(mtditid), any[Seq[String]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(None)))

      when(incomeTaxUserDataService.findUserData(anyUser, eqTo(taxYear))(any[ExecutionContext]))
        .thenReturn(Future.successful(Right(Some(anIncomeSources))))

      val expected = Some(anIncomeSources.copy(gains = None))

      when(incomeTaxUserDataService.saveUserData(eqTo(taxYear), any[Option[IncomeSources]])(any[Result])(anyUser, any[ExecutionContext]))
        .thenReturn(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "gains")) mustBe NoContent
    }

    "get the latest gains and update the data when no gains data in session or in the get" in {
      when(getIncomeSourcesService.getGains(eqTo(nino), eqTo(taxYear), eqTo(mtditid), any[Seq[String]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(None)))

      when(incomeTaxUserDataService.findUserData(anyUser, eqTo(taxYear))(any[ExecutionContext]))
        .thenReturn(Future.successful(Right(Some(anIncomeSources.copy(gains = None)))))

      val expected = Some(anIncomeSources.copy(gains = None))

      when(incomeTaxUserDataService.saveUserData(eqTo(taxYear), any[Option[IncomeSources]])(any[Result])(anyUser, any[ExecutionContext]))
        .thenReturn(Future.successful(NoContent))

      await(underTest.getLatestDataAndRefreshCache(taxYear, "gains")) mustBe NoContent
    }

  }
}
