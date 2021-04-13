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

package controllers


import models._
import models.giftAid.{GiftAidPaymentsModel, GiftsModel, SubmittedGiftAidModel}
import org.scalamock.handlers.CallHandler5
import play.api.http.Status._
import play.api.test.FakeRequest
import services.GetIncomeSourcesService
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtils

import scala.concurrent.Future

class GetIncomeSourcesControllerSpec extends TestUtils {

  val getIncomeSourcesService: GetIncomeSourcesService = mock[GetIncomeSourcesService]
  val controller = new GetIncomeSourcesController(getIncomeSourcesService, mockControllerComponents,authorisedAction)
  val nino :String = "123456789"
  val mtditid :String = "1234567890"
  val taxYear: Int = 1234
  private val fakeGetRequestWithHeader = FakeRequest("GET", "/").withHeaders("mtditid" -> "1234567890").withSession("MTDITID" -> "12234567890")
  private val fakeGetRequestWithExcludedHeader = FakeRequest("GET", "/").withHeaders("mtditid" -> "1234567890",
    "excluded-income-sources" -> "dividends,interest,gift-aid,employment").withSession("MTDITID" -> "12234567890")
  private val fakeGetRequestWithoutHeader = FakeRequest("GET", "/").withSession("MTDITID" -> "12234567890")

  def mockGetIncomeSourcesTurnedOff(): CallHandler5[String, Int, String, Seq[String], HeaderCarrier, Future[getIncomeSourcesService.IncomeSourceResponse]] = {
    (getIncomeSourcesService.getAllIncomeSources(_: String, _: Int, _: String, _:Seq[String])(_: HeaderCarrier))
      .expects(*, *, *, Seq("dividends","interest","gift-aid","employment"), *)
      .returning(Future.successful(Right(IncomeSourcesResponseModel(None,None, None))))
  }

  val giftAidPayments: GiftAidPaymentsModel = {
    GiftAidPaymentsModel(Some(List("")), Some(12345.67), Some(12345.67), Some(12345.67), Some(12345.67), Some(12345.67))
  }
  val gifts: GiftsModel = GiftsModel(Some(List("")), Some(12345.67), Some(12345.67) , Some(12345.67))

  def mockGetIncomeSourcesValid(): CallHandler5[String, Int, String, Seq[String], HeaderCarrier, Future[getIncomeSourcesService.IncomeSourceResponse]] = {
    val incomeSources: IncomeSourcesResponseModel = IncomeSourcesResponseModel(Some(DividendsResponseModel(Some(12345.67),Some(12345.67))),
      Some(Seq(SubmittedInterestModel("someName", "12345", Some(12345.67), Some(12345.67)))), Some(SubmittedGiftAidModel(Some(giftAidPayments), Some(gifts))))
    (getIncomeSourcesService.getAllIncomeSources(_: String, _: Int, _: String, _:Seq[String])(_: HeaderCarrier))
      .expects(*, *, *, Seq(), *)
      .returning(Future.successful(Right(incomeSources)))
  }

  def mockGetIncomeSourcesInvalid(): CallHandler5[String, Int, String, Seq[String], HeaderCarrier, Future[getIncomeSourcesService.IncomeSourceResponse]] = {
    val invalidIncomeSource: APIErrorModel = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong"))
      (getIncomeSourcesService.getAllIncomeSources(_: String, _: Int, _: String, _:Seq[String])(_: HeaderCarrier))
      .expects(*, *, *, Seq(), *)
      .returning(Future.successful(Left(invalidIncomeSource)))
  }


  "calling .getIncomeSources" should {

    "with either existing dividend, interest or giftaid" should {

      "return a 204 response when sources are turned off" in {
        val result = {
          mockAuth()
          mockGetIncomeSourcesTurnedOff()
          controller.getIncomeSources(nino, taxYear)(fakeGetRequestWithExcludedHeader)
        }
        status(result) mustBe NO_CONTENT
      }

      "return an OK 200 response when called as an individual" in {
        val result = {
          mockAuth()
          mockGetIncomeSourcesValid()
          controller.getIncomeSources(nino, taxYear)(fakeGetRequestWithHeader)
        }
        status(result) mustBe OK
      }

      "return an OK 200 response when called as an agent" in {
        val result = {
          mockAuthAsAgent()
          mockGetIncomeSourcesValid()
          controller.getIncomeSources(nino, taxYear)(fakeGetRequestWithHeader)
        }
        status(result) mustBe OK
      }

    }
    "without existing dividend, interest and giftaid" should {

      "return an InternalServerError response when called as an individual" in {
        val result = {
          mockAuth()
          mockGetIncomeSourcesInvalid()
          controller.getIncomeSources(nino, taxYear)(fakeGetRequestWithHeader)
        }
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "return an InternalServerError response when called as an agent" in {
        val result = {
          mockAuthAsAgent()
          mockGetIncomeSourcesInvalid()
          controller.getIncomeSources(nino, taxYear)(fakeGetRequestWithHeader)
        }
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

    }
    "without mtditid present in header" should {

      "return an UNAUTHORIZED 401 response when called as an individual" in {
        val result = {
          controller.getIncomeSources(nino, taxYear)(fakeGetRequestWithoutHeader)
        }
        status(result) mustBe UNAUTHORIZED
      }

      "return an UNAUTHORIZED 401 response when called as an agent" in {
        val result = {
          controller.getIncomeSources(nino, taxYear)(fakeGetRequestWithoutHeader)
        }
        status(result) mustBe UNAUTHORIZED
      }

    }

  }
}
