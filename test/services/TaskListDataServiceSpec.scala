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


import connectors._
import models._
import models.tasklist.SectionTitle.{CharitableDonationsTitle, DividendsTitle, InsuranceGainsTitle, InterestTitle, PensionsTitle}
import models.tasklist.TaskTitle._
import models.tasklist._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TaskListDataServiceSpec extends AnyWordSpec with Matchers with ScalaFutures with MockitoSugar {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  override implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(5, Seconds), interval = Span(100, Millis))

  val taxYear2023 = 2023
  val nino = "nino"

  val tailoringResponse: Future[Right[Nothing, Some[TaskListModel]]] = Future.successful(Right(Some(TaskListModel(Seq(
    TaskListSection(
      PensionsTitle, Some(Seq(
        TaskListSectionItem(UnauthorisedPayments, TaskStatus.Completed, Some("CYAPage")),
        TaskListSectionItem(ShortServiceRefunds, TaskStatus.NotStarted, Some("ShortServiceRefundsPage")),
        TaskListSectionItem(IncomeFromOverseas, TaskStatus.NotStarted, Some("IncomeFromOverseasPage"))
      ))),
    TaskListSection(
      DividendsTitle, Some(Seq(
        TaskListSectionItem(CashDividends, TaskStatus.NotStarted, Some("CashDividendsPage")),
        TaskListSectionItem(DividendsFromUnitTrusts, TaskStatus.NotStarted, Some("DividendsFromUnitTrustsPage"))
      ))),
    TaskListSection(
      InsuranceGainsTitle, Some(Seq(
        TaskListSectionItem(LifeInsurance, TaskStatus.InProgress, Some("LifeInsurancePage")),
        TaskListSectionItem(LifeAnnuity, TaskStatus.Completed, Some("CYAPage")),
        TaskListSectionItem(CapitalRedemption, TaskStatus.NotStarted, Some("CapitalRedemptionPage")),
        TaskListSectionItem(VoidedISA, TaskStatus.NotStarted, Some("VoidedISAPage"))
      ))),
    TaskListSection(
      CharitableDonationsTitle, Some(Seq(
      TaskListSectionItem(DonationsUsingGiftAid, TaskStatus.Completed, Some("CYAPage")),
      TaskListSectionItem(GiftsOfLandOrProperty, TaskStatus.Completed, Some("CYAPage")),
      TaskListSectionItem(GiftsOfShares, TaskStatus.Completed, Some("CYAPage")),
      TaskListSectionItem(GiftsToOverseas, TaskStatus.Completed, Some("CYAPage"))
    ))),
    TaskListSection(
      InterestTitle, Some(Seq(
      TaskListSectionItem(BanksAndBuilding, TaskStatus.Completed, Some("CYAPage")),
      TaskListSectionItem(TrustFundBond, TaskStatus.Completed, Some("CYAPage")),
      TaskListSectionItem(GiltEdged, TaskStatus.Completed, Some("CYAPage"))
    )))
  )))))

  val pensionResponse: Future[Right[Nothing, Some[TaskListModel]]] = Future.successful(Right(Some(TaskListModel(Seq(
    TaskListSection(PensionsTitle, Some(Seq(
      TaskListSectionItem(StatePension, TaskStatus.InProgress, Some("StatePensionPage")),
      TaskListSectionItem(OtherUkPensions, TaskStatus.Completed, Some("CYAPage")),
      TaskListSectionItem(UnauthorisedPayments, TaskStatus.InProgress, Some("UnauthorisedPaymentsPage")),
      TaskListSectionItem(ShortServiceRefunds, TaskStatus.NotStarted, Some("ShortServiceRefundsPage")),
      TaskListSectionItem(IncomeFromOverseas, TaskStatus.NotStarted, Some("IncomeFromOverseasPage")),
    )))
  )))))

  val dividendsResponse: Future[Right[Nothing, Some[TaskListSection]]] = Future.successful(Right(Some(TaskListSection(
    DividendsTitle, Some(Seq(
      TaskListSectionItem(CashDividends, TaskStatus.InProgress, Some("CashDividendsPage")),
      TaskListSectionItem(models.tasklist.TaskTitle.StockDividends, TaskStatus.Completed, Some("CYAPage")),
      TaskListSectionItem(DividendsFromUnitTrusts, TaskStatus.NotStarted, Some("DividendsFromUnitTrustsPage"))
    ))
  ))))

  val additionalInfoResponse: Future[Right[Nothing, Some[TaskListSection]]] = Future.successful(Right(Some(TaskListSection(
    InsuranceGainsTitle, Some(Seq(
      TaskListSectionItem(LifeInsurance, TaskStatus.InProgress, Some("LifeInsurancePage")),
      TaskListSectionItem(LifeAnnuity, TaskStatus.Completed, Some("CYAPage")),
      TaskListSectionItem(CapitalRedemption, TaskStatus.NotStarted, Some("CapitalRedemptionPage")),
      TaskListSectionItem(VoidedISA, TaskStatus.NotStarted, Some("VoidedISAPage"))
    ))
  ))))

  val charitableDonationsResponse: Future[Right[Nothing, Some[TaskListSection]]] = Future.successful(Right(Some(TaskListSection(
    CharitableDonationsTitle, Some(Seq(
      TaskListSectionItem(DonationsUsingGiftAid, TaskStatus.Completed, Some("CYAPage")),
      TaskListSectionItem(GiftsOfLandOrProperty, TaskStatus.Completed, Some("CYAPage")),
      TaskListSectionItem(GiftsOfShares, TaskStatus.Completed, Some("CYAPage")),
      TaskListSectionItem(GiftsToOverseas, TaskStatus.Completed, Some("CYAPage"))
    ))
  ))))

  val interestResponse: Future[Right[Nothing, Some[TaskListSection]]] = Future.successful(Right(Some(TaskListSection(
    InterestTitle, Some(Seq(
      TaskListSectionItem(BanksAndBuilding, TaskStatus.Completed, Some("CYAPage")),
      TaskListSectionItem(TrustFundBond, TaskStatus.Completed, Some("CYAPage")),
      TaskListSectionItem(GiltEdged, TaskStatus.Completed, Some("CYAPage"))
    ))
  ))))

  val errorModel: APIErrorModel = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorsBodyModel(Seq(
    APIErrorBodyModel("INVALID_IDTYPE", "ID is invalid"),
    APIErrorBodyModel("INVALID_IDTYPE_2", "ID 2 is invalid")
  )))

  val errorResponse: Future[Left[APIErrorModel, Nothing]] = Future.successful(Left(errorModel))

  val mockConnector: TaskListDataConnector = mock[TaskListDataConnector]
  val mockPensionConnector: PensionTaskListDataConnector = mock[PensionTaskListDataConnector]
  val mockDividendsConnector: DividendsTaskListDataConnector = mock[DividendsTaskListDataConnector]
  val mockAdditionalInfoConnector: AdditionalInfoTaskListDataConnector = mock[AdditionalInfoTaskListDataConnector]
  val mockCharitableDonationsConnector: CharitableDonationsTaskListDataConnector = mock[CharitableDonationsTaskListDataConnector]
  val mockInterestTaskListDataConnector: InterestTaskListDataConnector = mock[InterestTaskListDataConnector]

  val taskListDataService =
    new TaskListDataService(
      mockConnector, mockPensionConnector, mockDividendsConnector, mockAdditionalInfoConnector,
      mockCharitableDonationsConnector, mockInterestTaskListDataConnector
    )

  "TaskListDataService" should {

    "correctly merge task list sections from all services" in {

      when(mockConnector.get(any[Int])(any[HeaderCarrier])).thenReturn(tailoringResponse)
      when(mockPensionConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(pensionResponse)
      when(mockDividendsConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(dividendsResponse)
      when(mockAdditionalInfoConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(additionalInfoResponse)
      when(mockCharitableDonationsConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(charitableDonationsResponse)
      when(mockInterestTaskListDataConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(interestResponse)

      val result: Either[APIErrorModel, Option[TaskListModel]] = taskListDataService.get(taxYear2023, nino).futureValue

      result match {
        case Right(Some(taskListModel)) =>
          val pensionsSection = taskListModel.taskList.find(_.sectionTitle == PensionsTitle)
          val dividendsSection = taskListModel.taskList.find(_.sectionTitle == DividendsTitle)
          val insuranceGainsSection = taskListModel.taskList.find(_.sectionTitle == InsuranceGainsTitle)
          val charitableDonationsSection = taskListModel.taskList.find(_.sectionTitle == CharitableDonationsTitle)
          val interestSection = taskListModel.taskList.find(_.sectionTitle == InterestTitle)

          pensionsSection shouldBe defined
          dividendsSection shouldBe defined
          insuranceGainsSection shouldBe defined
          charitableDonationsSection shouldBe defined
          interestSection shouldBe defined

          pensionsSection.get.taskItems.get should contain theSameElementsAs Seq(
            //was inprogress in pension, but was not selected in tailoring
            TaskListSectionItem(StatePension, TaskStatus.CheckNow, Some("StatePensionPage")),
            //was Completed in pension, but was not selected in tailoring
            TaskListSectionItem(OtherUkPensions, TaskStatus.CheckNow, Some("CYAPage")),
            //was Completed (CYAPage) in Tailoring but pension had InProgress("UnauthorisedPaymentsPage"
            TaskListSectionItem(UnauthorisedPayments, TaskStatus.InProgress, Some("UnauthorisedPaymentsPage")),
            //Below two stays same as similar response
            TaskListSectionItem(ShortServiceRefunds, TaskStatus.NotStarted, Some("ShortServiceRefundsPage")),
            TaskListSectionItem(IncomeFromOverseas, TaskStatus.NotStarted, Some("IncomeFromOverseasPage"))
          )

          dividendsSection.get.taskItems.get should contain theSameElementsAs Seq(
            TaskListSectionItem(CashDividends, TaskStatus.InProgress, Some("CashDividendsPage")),
            TaskListSectionItem(models.tasklist.TaskTitle.StockDividends, TaskStatus.CheckNow, Some("CYAPage")),
            TaskListSectionItem(DividendsFromUnitTrusts, TaskStatus.NotStarted, Some("DividendsFromUnitTrustsPage"))
          )
          //In gains we cover the scenario where response from Tailoring and Downstream are same
          insuranceGainsSection.get.taskItems.get should contain theSameElementsAs Seq(
            TaskListSectionItem(LifeInsurance, TaskStatus.InProgress, Some("LifeInsurancePage")),
            TaskListSectionItem(LifeAnnuity, TaskStatus.Completed, Some("CYAPage")),
            TaskListSectionItem(CapitalRedemption, TaskStatus.NotStarted, Some("CapitalRedemptionPage")),
            TaskListSectionItem(VoidedISA, TaskStatus.NotStarted, Some("VoidedISAPage"))
          )

          charitableDonationsSection.get.taskItems.get should contain theSameElementsAs Seq(
            TaskListSectionItem(DonationsUsingGiftAid, TaskStatus.Completed, Some("CYAPage")),
            TaskListSectionItem(GiftsOfLandOrProperty, TaskStatus.Completed, Some("CYAPage")),
            TaskListSectionItem(GiftsOfShares, TaskStatus.Completed, Some("CYAPage")),
            TaskListSectionItem(GiftsToOverseas, TaskStatus.Completed, Some("CYAPage"))
          )

          interestSection.get.taskItems.get should contain theSameElementsAs Seq(
            TaskListSectionItem(BanksAndBuilding, TaskStatus.Completed, Some("CYAPage")),
            TaskListSectionItem(TrustFundBond, TaskStatus.Completed, Some("CYAPage")),
            TaskListSectionItem(GiltEdged, TaskStatus.Completed, Some("CYAPage"))
          )

        case _ => fail("Unexpected result")
      }
    }

    "correctly merge task list sections and return only tailoring data if downstream does not have any known data" in {
      val emptyResponse = Future.successful(Right(None))

      when(mockConnector.get(any[Int])(any[HeaderCarrier])).thenReturn(tailoringResponse)
      when(mockPensionConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(emptyResponse)
      when(mockDividendsConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(emptyResponse)
      when(mockAdditionalInfoConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(additionalInfoResponse)
      when(mockCharitableDonationsConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(emptyResponse)
      when(mockInterestTaskListDataConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(emptyResponse)

      val result: Either[APIErrorModel, Option[TaskListModel]] = taskListDataService.get(taxYear2023, nino).futureValue

      result match {
        case Right(Some(taskListModel)) =>
          val pensionsSection = taskListModel.taskList.find(_.sectionTitle == PensionsTitle)
          val dividendsSection = taskListModel.taskList.find(_.sectionTitle == DividendsTitle)
          val insuranceGainsSection = taskListModel.taskList.find(_.sectionTitle == InsuranceGainsTitle)
          val charitableDonationsSection = taskListModel.taskList.find(_.sectionTitle == CharitableDonationsTitle)
          val interestSection = taskListModel.taskList.find(_.sectionTitle == InterestTitle)

          pensionsSection shouldBe defined
          dividendsSection shouldBe defined
          insuranceGainsSection shouldBe defined
          interestSection shouldBe defined

          pensionsSection.get.taskItems.get should contain theSameElementsAs Seq(
            //below section details are as from Tailoring
            TaskListSectionItem(UnauthorisedPayments, TaskStatus.Completed, Some("CYAPage")),
            TaskListSectionItem(ShortServiceRefunds, TaskStatus.NotStarted, Some("ShortServiceRefundsPage")),
            TaskListSectionItem(IncomeFromOverseas, TaskStatus.NotStarted, Some("IncomeFromOverseasPage"))
          )

          dividendsSection.get.taskItems.get should contain theSameElementsAs Seq(
            TaskListSectionItem(CashDividends, TaskStatus.NotStarted, Some("CashDividendsPage")),
            TaskListSectionItem(DividendsFromUnitTrusts, TaskStatus.NotStarted, Some("DividendsFromUnitTrustsPage")),
          )

          insuranceGainsSection.get.taskItems.get should contain theSameElementsAs Seq(
            TaskListSectionItem(LifeInsurance, TaskStatus.InProgress, Some("LifeInsurancePage")),
            TaskListSectionItem(LifeAnnuity, TaskStatus.Completed, Some("CYAPage")),
            TaskListSectionItem(CapitalRedemption, TaskStatus.NotStarted, Some("CapitalRedemptionPage")),
            TaskListSectionItem(VoidedISA, TaskStatus.NotStarted, Some("VoidedISAPage"))
          )

          charitableDonationsSection.get.taskItems.get should contain theSameElementsAs Seq(
            TaskListSectionItem(DonationsUsingGiftAid, TaskStatus.Completed, Some("CYAPage")),
            TaskListSectionItem(GiftsOfLandOrProperty, TaskStatus.Completed, Some("CYAPage")),
            TaskListSectionItem(GiftsOfShares, TaskStatus.Completed, Some("CYAPage")),
            TaskListSectionItem(GiftsToOverseas, TaskStatus.Completed, Some("CYAPage"))
          )

          interestSection.get.taskItems.get should contain theSameElementsAs Seq(
            TaskListSectionItem(BanksAndBuilding, TaskStatus.Completed, Some("CYAPage")),
            TaskListSectionItem(TrustFundBond, TaskStatus.Completed, Some("CYAPage")),
            TaskListSectionItem(GiltEdged, TaskStatus.Completed, Some("CYAPage"))
          )

        case _ => fail("Unexpected result")
      }
    }

    "return under maintenance if service has internal error" in {

      when(mockConnector.get(any[Int])(any[HeaderCarrier])).thenReturn(tailoringResponse)
      when(mockPensionConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(errorResponse)
      when(mockDividendsConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(errorResponse)
      when(mockAdditionalInfoConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(additionalInfoResponse)
      when(mockCharitableDonationsConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(charitableDonationsResponse)
      when(mockInterestTaskListDataConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(interestResponse)

      val result: Either[APIErrorModel, Option[TaskListModel]] = taskListDataService.get(taxYear2023, nino).futureValue

      result match {
        case Right(Some(taskListModel)) =>

          val pensionsSection = taskListModel.taskList.find(_.sectionTitle == PensionsTitle)
          val dividendsSection = taskListModel.taskList.find(_.sectionTitle == DividendsTitle)
          val insuranceGainsSection = taskListModel.taskList.find(_.sectionTitle == InsuranceGainsTitle)
          val charitableDonationsSection = taskListModel.taskList.find(_.sectionTitle == CharitableDonationsTitle)
          val interestSection = taskListModel.taskList.find(_.sectionTitle == InterestTitle)

          pensionsSection shouldBe defined
          dividendsSection shouldBe defined
          insuranceGainsSection shouldBe defined
          charitableDonationsSection shouldBe defined
          interestSection shouldBe defined

          pensionsSection.get.taskItems.get should contain theSameElementsAs Seq(
            TaskListSectionItem(UnauthorisedPayments, TaskStatus.UnderMaintenance, None),
            TaskListSectionItem(ShortServiceRefunds, TaskStatus.UnderMaintenance, None),
            TaskListSectionItem(IncomeFromOverseas, TaskStatus.UnderMaintenance, None)
          )

          dividendsSection.get.taskItems.get should contain theSameElementsAs Seq(
            TaskListSectionItem(CashDividends, TaskStatus.UnderMaintenance, None),
            TaskListSectionItem(DividendsFromUnitTrusts, TaskStatus.UnderMaintenance, None)
          )

          insuranceGainsSection.get.taskItems.get should contain theSameElementsAs Seq(
            TaskListSectionItem(LifeInsurance, TaskStatus.InProgress, Some("LifeInsurancePage")),
            TaskListSectionItem(LifeAnnuity, TaskStatus.Completed, Some("CYAPage")),
            TaskListSectionItem(CapitalRedemption, TaskStatus.NotStarted, Some("CapitalRedemptionPage")),
            TaskListSectionItem(VoidedISA, TaskStatus.NotStarted, Some("VoidedISAPage"))
          )

          charitableDonationsSection.get.taskItems.get should contain theSameElementsAs Seq(
            TaskListSectionItem(DonationsUsingGiftAid, TaskStatus.Completed, Some("CYAPage")),
            TaskListSectionItem(GiftsOfLandOrProperty, TaskStatus.Completed, Some("CYAPage")),
            TaskListSectionItem(GiftsOfShares, TaskStatus.Completed, Some("CYAPage")),
            TaskListSectionItem(GiftsToOverseas, TaskStatus.Completed, Some("CYAPage"))
          )

          interestSection.get.taskItems.get should contain theSameElementsAs Seq(
            TaskListSectionItem(BanksAndBuilding, TaskStatus.Completed, Some("CYAPage")),
            TaskListSectionItem(TrustFundBond, TaskStatus.Completed, Some("CYAPage")),
            TaskListSectionItem(GiltEdged, TaskStatus.Completed, Some("CYAPage"))
          )

        case _ => fail("Unexpected result")
      }

    }

    "return under maintenance if any of the service fails" in {

      val serviceError = Future.failed(new RuntimeException("Service failed"))

      when(mockConnector.get(any[Int])(any[HeaderCarrier])).thenReturn(tailoringResponse)
      when(mockPensionConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(serviceError)
      when(mockDividendsConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(serviceError)
      when(mockAdditionalInfoConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(additionalInfoResponse)
      when(mockInterestTaskListDataConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(interestResponse)

      val result: Either[APIErrorModel, Option[TaskListModel]] = taskListDataService.get(taxYear2023, nino).futureValue

      result match {
        case Right(Some(taskListModel)) =>

          val pensionsSection = taskListModel.taskList.find(_.sectionTitle == PensionsTitle)
          val dividendsSection = taskListModel.taskList.find(_.sectionTitle == DividendsTitle)
          val insuranceGainsSection = taskListModel.taskList.find(_.sectionTitle == InsuranceGainsTitle)
          val interestSection = taskListModel.taskList.find(_.sectionTitle == InterestTitle)

          pensionsSection shouldBe defined
          dividendsSection shouldBe defined
          insuranceGainsSection shouldBe defined
          interestSection shouldBe defined

          pensionsSection.get.taskItems.get should contain theSameElementsAs Seq(
            TaskListSectionItem(UnauthorisedPayments, TaskStatus.UnderMaintenance, None),
            TaskListSectionItem(ShortServiceRefunds, TaskStatus.UnderMaintenance, None),
            TaskListSectionItem(IncomeFromOverseas, TaskStatus.UnderMaintenance, None)
          )

          dividendsSection.get.taskItems.get should contain theSameElementsAs Seq(
            TaskListSectionItem(CashDividends, TaskStatus.UnderMaintenance, None),
            TaskListSectionItem(DividendsFromUnitTrusts, TaskStatus.UnderMaintenance, None)
          )
          //In gains we cover the scenario where response from Tailoring and Downstream are same
          insuranceGainsSection.get.taskItems.get should contain theSameElementsAs Seq(
            TaskListSectionItem(LifeInsurance, TaskStatus.InProgress, Some("LifeInsurancePage")),
            TaskListSectionItem(LifeAnnuity, TaskStatus.Completed, Some("CYAPage")),
            TaskListSectionItem(CapitalRedemption, TaskStatus.NotStarted, Some("CapitalRedemptionPage")),
            TaskListSectionItem(VoidedISA, TaskStatus.NotStarted, Some("VoidedISAPage"))
          )

          interestSection.get.taskItems.get should contain theSameElementsAs Seq(
            TaskListSectionItem(BanksAndBuilding, TaskStatus.Completed, Some("CYAPage")),
            TaskListSectionItem(TrustFundBond, TaskStatus.Completed, Some("CYAPage")),
            TaskListSectionItem(GiltEdged, TaskStatus.Completed, Some("CYAPage"))
          )

        case _ => fail("Unexpected result")
      }
    }

    "return an error if tailoring data is empty" in {
      val tailoringNoneResponse = Future.successful(Right(None))

      when(mockConnector.get(any[Int])(any[HeaderCarrier])).thenReturn(tailoringNoneResponse)

      val result = taskListDataService.get(taxYear2023, nino).futureValue

      result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INVALID STATE", "Tailoring task list data cannot be empty")))
    }

    "return an error if tailoring data service fails" in {
      when(mockConnector.get(any[Int])(any[HeaderCarrier])).thenReturn(errorResponse)

      val result = taskListDataService.get(taxYear2023, nino).futureValue

      result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INVALID STATE", "Failed to retrieve tailoring task list data")))
    }
  }
}
