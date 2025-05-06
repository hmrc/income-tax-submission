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


import config.AppConfig
import connectors._
import models._
import models.tasklist.SectionTitle._
import models.tasklist.TaskStatus.NotStarted
import models.tasklist.TaskTitle._
import models.tasklist._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TaskListDataServiceSpec extends AnyWordSpec with Matchers with ScalaFutures with MockitoSugar with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    reset(mockSETaskListDataConnector, mockAppConfig)
    super.beforeEach()
  }

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
      PaymentsIntoPensionsTitle, Some(Seq(
        TaskListSectionItem(PaymentsIntoUk, TaskStatus.InProgress, Some("Payments into UK pensions")),
        TaskListSectionItem(PaymentsIntoOverseas, TaskStatus.Completed, Some("Payments into overseas pensions")),
        TaskListSectionItem(OverseasTransfer, TaskStatus.InProgress, Some("Overseas transfer charges"))))
    ),
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
      ))),
    TaskListSection(
      SelfEmploymentTitle, Some(Seq(
        TaskListSectionItem(CIS, TaskStatus.Completed, Some("CISPage")),
        TaskListSectionItem(CheckSEDetails, TaskStatus.Completed, Some("SEDetailsPage"))
      ))),
    TaskListSection(
      EmploymentTitle, Some(Seq(
        TaskListSectionItem(PayeEmployment, TaskStatus.Completed, Some("CYAPage"))
      ))),
    TaskListSection(
      UkPropertyTitle, Some(Seq(
        TaskListSectionItem(UkProperty, TaskStatus.NotStarted, Some("/uk-property"))
      ))
    )
  )))))

  val pensionResponse: Future[Right[Nothing, Some[Seq[TaskListSection]]]] = Future.successful(Right(Some(Seq(
    TaskListSection(
      PensionsTitle, Some(Seq(
        TaskListSectionItem(StatePension, TaskStatus.InProgress, Some("StatePensionPage")),
        TaskListSectionItem(OtherUkPensions, TaskStatus.Completed, Some("CYAPage")),
        TaskListSectionItem(UnauthorisedPayments, TaskStatus.InProgress, Some("UnauthorisedPaymentsPage")),
        TaskListSectionItem(ShortServiceRefunds, TaskStatus.NotStarted, Some("ShortServiceRefundsPage")),
        TaskListSectionItem(IncomeFromOverseas, TaskStatus.NotStarted, Some("IncomeFromOverseasPage"))))
    ),
    TaskListSection(
      PaymentsIntoPensionsTitle, Some(Seq(
        TaskListSectionItem(PaymentsIntoUk, TaskStatus.InProgress, Some("Payments into UK pensions")),
        TaskListSectionItem(PaymentsIntoOverseas, TaskStatus.Completed, Some("Payments into overseas pensions")),
        TaskListSectionItem(OverseasTransfer, TaskStatus.InProgress, Some("Overseas transfer charges"))))
    )
  ))))

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

  val employmentResponse: Future[Right[Nothing, Some[TaskListSection]]] = Future.successful(Right(Some(TaskListSection(
    EmploymentTitle, Some(Seq(
      TaskListSectionItem(PayeEmployment, TaskStatus.Completed, Some("CYAPage")),
    ))
  ))))

  val interestResponse: Future[Right[Nothing, Some[TaskListSection]]] = Future.successful(Right(Some(TaskListSection(
    InterestTitle, Some(Seq(
      TaskListSectionItem(BanksAndBuilding, TaskStatus.Completed, Some("CYAPage")),
      TaskListSectionItem(TrustFundBond, TaskStatus.Completed, Some("CYAPage")),
      TaskListSectionItem(GiltEdged, TaskStatus.Completed, Some("CYAPage"))
    ))
  ))))

  val cisResponse: Future[Right[Nothing, Some[TaskListSection]]] = Future.successful(Right(Some(TaskListSection(
    SelfEmploymentTitle, Some(Seq(
      TaskListSectionItem(CIS, TaskStatus.Completed, Some("CISPage"))
    ))
  ))))

  val seResponse: Future[Right[Nothing, Some[TaskListSection]]] = Future.successful(Right(Some(TaskListSection(
    SelfEmploymentTitle, Some(Seq(
      TaskListSectionItem(CheckSEDetails, TaskStatus.Completed, Some("SEDetailsPage")),
      TaskListSectionItem(IndustrySector, TaskStatus.Completed, Some("IndustrySectorPage")),
      TaskListSectionItem(YourIncome, TaskStatus.Completed, Some("YourIncomePage"))
    ))
  ))))

  val stateBenefitsResponse: Future[Right[Nothing, Some[Seq[TaskListSection]]]] = Future.successful(Right(Some(Seq(
    TaskListSection(
      EsaTitle, Some(Seq(
        TaskListSectionItem(ESA, TaskStatus.Completed, Some("ESAPage"))
      ))
    ), TaskListSection(
      JsaTitle, Some(Seq(
        TaskListSectionItem(JSA, TaskStatus.Completed, Some("JSAPage"))
      ))
    )))))

  val propertyResponse: Future[Right[Nothing, Some[Seq[TaskListSection]]]] =
    Future.successful(Right(Some(Seq(
      TaskListSection(
        UkPropertyTitle, Some(Seq(
          TaskListSectionItem(UkProperty, TaskStatus.InProgress, Some("/uk-property"))
        ))
      ),
      TaskListSection(
        ForeignPropertyTitle, None
      ),
      TaskListSection(
        UkForeignPropertyTitle, Some(Seq(
          TaskListSectionItem(UkForeignProperty, TaskStatus.InProgress, Some("/uk-and-foreign-property"))
        ))
      )
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
  val mockCISTaskListDataConnector: CISTaskListDataConnector = mock[CISTaskListDataConnector]
  val mockSETaskListDataConnector: SelfEmploymentTaskListDataConnector = mock[SelfEmploymentTaskListDataConnector]
  val mockStateBenefitsTaskListDataConnector: StateBenefitsTaskListDataConnector = mock[StateBenefitsTaskListDataConnector]
  val mockEmploymentTaskListDataConnector: EmploymentTaskListDataConnector = mock[EmploymentTaskListDataConnector]
  val mockAppConfig: AppConfig = mock[AppConfig]
  val mockPropertyTaskListDataConnector: PropertyTaskListDataConnector = mock[PropertyTaskListDataConnector]

  val taskListDataService =
    new TaskListDataService(
      mockConnector, mockPensionConnector, mockDividendsConnector, mockAdditionalInfoConnector,
      mockCharitableDonationsConnector, mockInterestTaskListDataConnector, mockCISTaskListDataConnector,
      mockSETaskListDataConnector, mockStateBenefitsTaskListDataConnector, mockEmploymentTaskListDataConnector,
      mockPropertyTaskListDataConnector, mockAppConfig
    )

  "TaskListDataService" should {

    "correctly merge task list sections from all services" in {

      when(mockConnector.get(any[Int])(any[HeaderCarrier])).thenReturn(tailoringResponse)
      when(mockPensionConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(pensionResponse)
      when(mockDividendsConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(dividendsResponse)
      when(mockAdditionalInfoConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(additionalInfoResponse)
      when(mockCharitableDonationsConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(charitableDonationsResponse)
      when(mockInterestTaskListDataConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(interestResponse)
      when(mockCISTaskListDataConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(cisResponse)
      when(mockSETaskListDataConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(seResponse)
      when(mockStateBenefitsTaskListDataConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(stateBenefitsResponse)
      when(mockEmploymentTaskListDataConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(employmentResponse)
      when(mockAppConfig.selfEmploymentTaskListEnabled).thenReturn(true)
      when(mockPropertyTaskListDataConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(propertyResponse)

      val result: Either[APIErrorModel, Option[TaskListModel]] = taskListDataService.get(taxYear2023, nino).futureValue

      result match {
        case Right(Some(taskListModel)) =>
          val pensionsSection = taskListModel.taskList.find(_.sectionTitle == PensionsTitle)
          val dividendsSection = taskListModel.taskList.find(_.sectionTitle == DividendsTitle)
          val insuranceGainsSection = taskListModel.taskList.find(_.sectionTitle == InsuranceGainsTitle)
          val charitableDonationsSection = taskListModel.taskList.find(_.sectionTitle == CharitableDonationsTitle)
          val interestSection = taskListModel.taskList.find(_.sectionTitle == InterestTitle)
          val selfEmploymentSection = taskListModel.taskList.find(_.sectionTitle == SelfEmploymentTitle)
          val employmentSection = taskListModel.taskList.find(_.sectionTitle == EmploymentTitle)
          val ukPropertySection = taskListModel.taskList.find(_.sectionTitle == UkPropertyTitle)
          val foreignPropertySection = taskListModel.taskList.find(_.sectionTitle == ForeignPropertyTitle)
          val ukForeignPropertySection = taskListModel.taskList.find(_.sectionTitle == UkForeignPropertyTitle)

          pensionsSection shouldBe defined
          dividendsSection shouldBe defined
          insuranceGainsSection shouldBe defined
          charitableDonationsSection shouldBe defined
          interestSection shouldBe defined
          selfEmploymentSection shouldBe defined
          employmentSection shouldBe defined
          ukPropertySection shouldBe defined
          foreignPropertySection shouldBe None
          ukForeignPropertySection shouldBe None //Although it is in the property response, it is not in the tailoring response - so should not be shown


          pensionsSection.get.taskItems.get should contain theSameElementsAs Seq(
            //was inprogress in pension, but was not selected in tailoring
            TaskListSectionItem(StatePension, TaskStatus.NotStarted, Some("StatePensionPage")),
            //was Completed in pension, but was not selected in tailoring
            TaskListSectionItem(OtherUkPensions, TaskStatus.NotStarted, Some("CYAPage")),
            //was Completed (CYAPage) in Tailoring but pension had InProgress("UnauthorisedPaymentsPage"
            TaskListSectionItem(UnauthorisedPayments, TaskStatus.InProgress, Some("UnauthorisedPaymentsPage")),
            //Below two stays same as similar response
            TaskListSectionItem(ShortServiceRefunds, TaskStatus.NotStarted, Some("ShortServiceRefundsPage")),
            TaskListSectionItem(IncomeFromOverseas, TaskStatus.NotStarted, Some("IncomeFromOverseasPage"))
          )

          dividendsSection.get.taskItems.get should contain theSameElementsAs Seq(
            TaskListSectionItem(CashDividends, TaskStatus.InProgress, Some("CashDividendsPage")),
            TaskListSectionItem(models.tasklist.TaskTitle.StockDividends, TaskStatus.NotStarted, Some("CYAPage")),
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

          selfEmploymentSection.get.taskItems.get should contain theSameElementsAs Seq(
            TaskListSectionItem(CIS, TaskStatus.Completed, Some("CISPage")),
            TaskListSectionItem(CheckSEDetails, TaskStatus.Completed, Some("SEDetailsPage")),
            TaskListSectionItem(IndustrySector, NotStarted, Some("IndustrySectorPage")),
            TaskListSectionItem(YourIncome, NotStarted, Some("YourIncomePage"))
          )

          employmentSection.get.taskItems.get should contain theSameElementsAs Seq(
            TaskListSectionItem(PayeEmployment, TaskStatus.Completed, Some("CYAPage"))
          )

          ukPropertySection.get.taskItems.get should contain theSameElementsAs Seq(
            TaskListSectionItem(UkProperty, TaskStatus.InProgress, Some("/uk-property"))
          )

        case _ => fail("Unexpected result")
      }
    }

    "correctly merge task list sections from all services when selfEmploymentTaskListEnabled feature flag is set to 'false'" in {

      when(mockConnector.get(any[Int])(any[HeaderCarrier])).thenReturn(tailoringResponse)
      when(mockPensionConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(pensionResponse)
      when(mockDividendsConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(dividendsResponse)
      when(mockAdditionalInfoConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(additionalInfoResponse)
      when(mockCharitableDonationsConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(charitableDonationsResponse)
      when(mockInterestTaskListDataConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(interestResponse)
      when(mockCISTaskListDataConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(cisResponse)
      when(mockSETaskListDataConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(seResponse)
      when(mockStateBenefitsTaskListDataConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(stateBenefitsResponse)
      when(mockEmploymentTaskListDataConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(employmentResponse)
      when(mockAppConfig.selfEmploymentTaskListEnabled).thenReturn(false)

      val result: Either[APIErrorModel, Option[TaskListModel]] = taskListDataService.get(taxYear2023, nino).futureValue

      result match {
        case Right(Some(taskListModel)) =>
          val pensionsSection = taskListModel.taskList.find(_.sectionTitle == PensionsTitle)
          val dividendsSection = taskListModel.taskList.find(_.sectionTitle == DividendsTitle)
          val insuranceGainsSection = taskListModel.taskList.find(_.sectionTitle == InsuranceGainsTitle)
          val charitableDonationsSection = taskListModel.taskList.find(_.sectionTitle == CharitableDonationsTitle)
          val interestSection = taskListModel.taskList.find(_.sectionTitle == InterestTitle)
          val selfEmploymentSection = taskListModel.taskList.find(_.sectionTitle == SelfEmploymentTitle)
          val employmentSection = taskListModel.taskList.find(_.sectionTitle == EmploymentTitle)

          pensionsSection shouldBe defined
          dividendsSection shouldBe defined
          insuranceGainsSection shouldBe defined
          charitableDonationsSection shouldBe defined
          interestSection shouldBe defined
          selfEmploymentSection shouldBe defined
          employmentSection shouldBe defined


          pensionsSection.get.taskItems.get should contain theSameElementsAs Seq(
            //was inprogress in pension, but was not selected in tailoring
            TaskListSectionItem(StatePension, TaskStatus.NotStarted, Some("StatePensionPage")),
            //was Completed in pension, but was not selected in tailoring
            TaskListSectionItem(OtherUkPensions, TaskStatus.NotStarted, Some("CYAPage")),
            //was Completed (CYAPage) in Tailoring but pension had InProgress("UnauthorisedPaymentsPage"
            TaskListSectionItem(UnauthorisedPayments, TaskStatus.InProgress, Some("UnauthorisedPaymentsPage")),
            //Below two stays same as similar response
            TaskListSectionItem(ShortServiceRefunds, TaskStatus.NotStarted, Some("ShortServiceRefundsPage")),
            TaskListSectionItem(IncomeFromOverseas, TaskStatus.NotStarted, Some("IncomeFromOverseasPage"))
          )

          dividendsSection.get.taskItems.get should contain theSameElementsAs Seq(
            TaskListSectionItem(CashDividends, TaskStatus.InProgress, Some("CashDividendsPage")),
            TaskListSectionItem(models.tasklist.TaskTitle.StockDividends, TaskStatus.NotStarted, Some("CYAPage")),
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

          selfEmploymentSection.get.taskItems.get should contain theSameElementsAs Seq(
            TaskListSectionItem(CIS, TaskStatus.Completed, Some("CISPage")),
            TaskListSectionItem(CheckSEDetails, TaskStatus.NotStarted, Some("SEDetailsPage"))
          )

          employmentSection.get.taskItems.get should contain theSameElementsAs Seq(
            TaskListSectionItem(PayeEmployment, TaskStatus.Completed, Some("CYAPage"))
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
      when(mockCISTaskListDataConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(emptyResponse)
      when(mockSETaskListDataConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(emptyResponse)
      when(mockStateBenefitsTaskListDataConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(emptyResponse)
      when(mockEmploymentTaskListDataConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(emptyResponse)
      when(mockAppConfig.selfEmploymentTaskListEnabled).thenReturn(true)
      when(mockPropertyTaskListDataConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(emptyResponse)

      val result: Either[APIErrorModel, Option[TaskListModel]] = taskListDataService.get(taxYear2023, nino).futureValue

      result match {
        case Right(Some(taskListModel)) =>
          val pensionsSection = taskListModel.taskList.find(_.sectionTitle == PensionsTitle)
          val dividendsSection = taskListModel.taskList.find(_.sectionTitle == DividendsTitle)
          val insuranceGainsSection = taskListModel.taskList.find(_.sectionTitle == InsuranceGainsTitle)
          val charitableDonationsSection = taskListModel.taskList.find(_.sectionTitle == CharitableDonationsTitle)
          val interestSection = taskListModel.taskList.find(_.sectionTitle == InterestTitle)
          val selfEmploymentSection = taskListModel.taskList.find(_.sectionTitle == SelfEmploymentTitle)
          val employmentSection = taskListModel.taskList.find(_.sectionTitle == EmploymentTitle)
          val ukPropertySection = taskListModel.taskList.find(_.sectionTitle == UkPropertyTitle)
          val foreignPropertySection = taskListModel.taskList.find(_.sectionTitle == ForeignPropertyTitle)
          val ukForeignPropertySection = taskListModel.taskList.find(_.sectionTitle == UkForeignPropertyTitle)

          pensionsSection shouldBe defined
          dividendsSection shouldBe defined
          insuranceGainsSection shouldBe defined
          charitableDonationsSection shouldBe defined
          interestSection shouldBe defined
          selfEmploymentSection shouldBe defined
          employmentSection shouldBe defined
          ukPropertySection shouldBe defined
          foreignPropertySection shouldBe None
          ukForeignPropertySection shouldBe None

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

          selfEmploymentSection.get.taskItems.get should contain theSameElementsAs Seq(
            TaskListSectionItem(CIS, TaskStatus.Completed, Some("CISPage")),
            TaskListSectionItem(CheckSEDetails, TaskStatus.Completed, Some("SEDetailsPage"))
          )

          employmentSection.get.taskItems.get should contain theSameElementsAs Seq(
            TaskListSectionItem(PayeEmployment, TaskStatus.Completed, Some("CYAPage"))
          )

          ukPropertySection.get.taskItems.get should contain theSameElementsAs Seq(
            TaskListSectionItem(UkProperty, TaskStatus.NotStarted, Some("/uk-property"))
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
      when(mockCISTaskListDataConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(errorResponse)
      when(mockSETaskListDataConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(errorResponse)
      when(mockStateBenefitsTaskListDataConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(errorResponse)
      when(mockEmploymentTaskListDataConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(errorResponse)
      when(mockAppConfig.selfEmploymentTaskListEnabled).thenReturn(true)
      when(mockPropertyTaskListDataConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(errorResponse)

      val result: Either[APIErrorModel, Option[TaskListModel]] = taskListDataService.get(taxYear2023, nino).futureValue

      result match {
        case Right(Some(taskListModel)) =>

          val pensionsSection = taskListModel.taskList.find(_.sectionTitle == PensionsTitle)
          val dividendsSection = taskListModel.taskList.find(_.sectionTitle == DividendsTitle)
          val insuranceGainsSection = taskListModel.taskList.find(_.sectionTitle == InsuranceGainsTitle)
          val charitableDonationsSection = taskListModel.taskList.find(_.sectionTitle == CharitableDonationsTitle)
          val interestSection = taskListModel.taskList.find(_.sectionTitle == InterestTitle)
          val selfEmploymentSection = taskListModel.taskList.find(_.sectionTitle == SelfEmploymentTitle)
          val employmentSection = taskListModel.taskList.find(_.sectionTitle == EmploymentTitle)
          val ukPropertySection = taskListModel.taskList.find(_.sectionTitle == UkPropertyTitle)
          val foreignPropertySection = taskListModel.taskList.find(_.sectionTitle == ForeignPropertyTitle)
          val ukForeignPropertySection = taskListModel.taskList.find(_.sectionTitle == UkForeignPropertyTitle)

          pensionsSection shouldBe defined
          dividendsSection shouldBe defined
          insuranceGainsSection shouldBe defined
          charitableDonationsSection shouldBe defined
          interestSection shouldBe defined
          selfEmploymentSection shouldBe defined
          employmentSection shouldBe defined
          ukPropertySection shouldBe defined
          foreignPropertySection shouldBe None
          ukForeignPropertySection shouldBe None

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
          selfEmploymentSection.get.taskItems.get should contain theSameElementsAs Seq(
            TaskListSectionItem(CIS, TaskStatus.UnderMaintenance, None),
            TaskListSectionItem(CheckSEDetails, TaskStatus.UnderMaintenance, None)
          )

          employmentSection.get.taskItems.get should contain theSameElementsAs Seq(
            TaskListSectionItem(PayeEmployment, TaskStatus.UnderMaintenance, None)
          )

          ukPropertySection.get.taskItems.get should contain theSameElementsAs Seq(
            TaskListSectionItem(UkProperty, TaskStatus.UnderMaintenance, None)
          )

        case _ => fail("Unexpected result")
      }

    }

    "return under maintenance if a service fails" in {

      val serviceError = Future.failed(new RuntimeException("Service failed"))

      when(mockConnector.get(any[Int])(any[HeaderCarrier])).thenReturn(tailoringResponse)
      when(mockPensionConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(serviceError)
      when(mockDividendsConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(serviceError)
      when(mockAdditionalInfoConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(additionalInfoResponse)
      when(mockInterestTaskListDataConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(interestResponse)
      when(mockCISTaskListDataConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(errorResponse)
      when(mockSETaskListDataConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(errorResponse)
      when(mockEmploymentTaskListDataConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(errorResponse)
      when(mockAppConfig.selfEmploymentTaskListEnabled).thenReturn(true)
      when(mockPropertyTaskListDataConnector.get(any[Int], any[String])(any[HeaderCarrier])).thenReturn(errorResponse)

      val result: Either[APIErrorModel, Option[TaskListModel]] = taskListDataService.get(taxYear2023, nino).futureValue

      result match {
        case Right(Some(taskListModel)) =>

          val pensionsSection = taskListModel.taskList.find(_.sectionTitle == PensionsTitle)
          val dividendsSection = taskListModel.taskList.find(_.sectionTitle == DividendsTitle)
          val insuranceGainsSection = taskListModel.taskList.find(_.sectionTitle == InsuranceGainsTitle)
          val interestSection = taskListModel.taskList.find(_.sectionTitle == InterestTitle)
          val selfEmploymentSection = taskListModel.taskList.find(_.sectionTitle == SelfEmploymentTitle)
          val employmentSection = taskListModel.taskList.find(_.sectionTitle == EmploymentTitle)
          val ukPropertySection = taskListModel.taskList.find(_.sectionTitle == UkPropertyTitle)
          val foreignPropertySection = taskListModel.taskList.find(_.sectionTitle == ForeignPropertyTitle)
          val ukForeignPropertySection = taskListModel.taskList.find(_.sectionTitle == UkForeignPropertyTitle)

          pensionsSection shouldBe defined
          dividendsSection shouldBe defined
          insuranceGainsSection shouldBe defined
          interestSection shouldBe defined
          selfEmploymentSection shouldBe defined
          employmentSection shouldBe defined
          ukPropertySection shouldBe defined
          foreignPropertySection shouldBe None
          ukForeignPropertySection shouldBe None

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

          selfEmploymentSection.get.taskItems.get should contain theSameElementsAs Seq(
            TaskListSectionItem(CIS, TaskStatus.UnderMaintenance, None),
            TaskListSectionItem(CheckSEDetails, TaskStatus.UnderMaintenance, None)
          )

          employmentSection.get.taskItems.get should contain theSameElementsAs Seq(
            TaskListSectionItem(PayeEmployment, TaskStatus.UnderMaintenance, None)
          )

          ukPropertySection.get.taskItems.get should contain theSameElementsAs Seq(
            TaskListSectionItem(UkProperty, TaskStatus.UnderMaintenance, None)
          )

        case _ => fail("Unexpected result")
      }
    }

    "return an error if tailoring data is empty" in {
      val tailoringNoneResponse = Future.successful(Right(None))

      when(mockConnector.get(any[Int])(any[HeaderCarrier])).thenReturn(tailoringNoneResponse)

      val result = taskListDataService.get(taxYear2023, nino).futureValue

      result shouldBe Left(APIErrorModel(NOT_FOUND, APIErrorBodyModel("NOT_FOUND", "Tailoring task list data is not found")))
    }

    "return an error if tailoring data service fails" in {
      when(mockConnector.get(any[Int])(any[HeaderCarrier])).thenReturn(errorResponse)

      val result = taskListDataService.get(taxYear2023, nino).futureValue

      result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INVALID STATE", "Failed to retrieve tailoring task list data")))
    }
  }
}
