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

package utils

import akka.actor.ActorSystem
import com.codahale.metrics.SharedMetricRegistries
import common.{EnrolmentIdentifiers, EnrolmentKeys}
import config.AppConfig
import controllers.predicates.AuthorisedAction
import models.employment._
import models.gifts.{GiftAid, GiftAidPayments, Gifts}
import models.mongo.UserData
import models.pensions._
import models.pensions.charges._
import models.pensions.reliefs.{PensionReliefs, Reliefs}
import models.pensions.statebenefits.{StateBenefit, StateBenefits, StateBenefitsModel}
import models.{Dividends, IncomeSourcesResponseModel, Interest}
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.mvc.{AnyContentAsEmpty, ControllerComponents, DefaultActionBuilder, Result}
import play.api.test.{FakeRequest, Helpers}
import services.AuthService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Awaitable, ExecutionContext, Future}

trait TestUtils extends AnyWordSpec with Matchers with MockFactory with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    super.beforeEach()
    SharedMetricRegistries.clear()
  }

  implicit val actorSystem: ActorSystem = ActorSystem()

  def await[T](awaitable: Awaitable[T]): T = Await.result(awaitable, Duration.Inf)

  implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET",
    "/income-tax-submission-service/income-tax/nino/AA123456A/sources?taxYear=2022").withHeaders("mtditid" -> "1234567890", "sessionId" -> "sessionId")
  implicit val emptyHeaderCarrier: HeaderCarrier = HeaderCarrier()

  lazy val mockAppConfig: AppConfig = new MockAppConfig(isEncrypted = false)
  lazy val mockAppConfigWithEncryption: AppConfig = new MockAppConfig(isEncrypted = true)
  implicit val mockControllerComponents: ControllerComponents = Helpers.stubControllerComponents()
  implicit val mockExecutionContext: ExecutionContext = ExecutionContext.Implicits.global
  implicit val mockAuthConnector: AuthConnector = mock[AuthConnector]
  implicit val mockAuthService: AuthService = new AuthService(mockAuthConnector)
  val defaultActionBuilder: DefaultActionBuilder = DefaultActionBuilder(mockControllerComponents.parsers.default)
  val authorisedAction = new AuthorisedAction()(mockAuthConnector, defaultActionBuilder, mockControllerComponents)


  def status(awaitable: Future[Result]): Int = await(awaitable).header.status

  def bodyOf(awaitable: Future[Result]): String = {
    val awaited = await(awaitable)
    await(awaited.body.consumeData.map(_.utf8String))
  }

  val individualEnrolments: Enrolments = Enrolments(Set(
    Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, "1234567890")), "Activated"),
    Enrolment(EnrolmentKeys.nino, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, "1234567890")), "Activated")))

  //noinspection ScalaStyle
  def mockAuth(enrolments: Enrolments = individualEnrolments) = {

    (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, Retrievals.affinityGroup, *, *)
      .returning(Future.successful(Some(AffinityGroup.Individual)))

    (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
      .returning(Future.successful(enrolments and ConfidenceLevel.L200))
  }

  val agentEnrolments: Enrolments = Enrolments(Set(
    Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, "1234567890")), "Activated"),
    Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, "0987654321")), "Activated")
  ))

  //noinspection ScalaStyle
  def mockAuthAsAgent(enrolments: Enrolments = agentEnrolments) = {

    (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, Retrievals.affinityGroup, *, *)
      .returning(Future.successful(Some(AffinityGroup.Agent)))

    (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
      .expects(Enrolment(EnrolmentKeys.Individual)
        .withIdentifier(EnrolmentIdentifiers.individualId, "1234567890")
        .withDelegatedAuthRule("mtd-it-auth"), *, *, *)
      .returning(Future.successful(enrolments))
  }

  //noinspection ScalaStyle
  def mockAuthReturnException(exception: Exception) = {
    (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *)
      .returning(Future.failed(exception))
  }

  val allEmploymentData =
    AllEmploymentData(
      Seq(
        EmploymentSource(
          employmentId = "00000000-0000-0000-1111-000000000000",
          employerRef = Some("666/66666"),
          employerName = "Business",
          payrollId = Some("1234567890"),
          startDate = Some("2020-01-01"),
          cessationDate = Some("2020-01-01"),
          dateIgnored = Some("2020-01-01T10:00:38Z"),
          submittedOn = None,
          employmentData = Some(EmploymentData(
            "2020-01-04T05:01:01Z",
            employmentSequenceNumber = Some("1002"),
            companyDirector = Some(false),
            closeCompany = Some(true),
            directorshipCeasedDate = Some("2020-02-12"),
            occPen = Some(false),
            disguisedRemuneration = Some(false),
            pay = Some(Pay(
              taxablePayToDate = Some(34234.15),
              totalTaxToDate = Some(6782.92),
              payFrequency = Some("CALENDAR MONTHLY"),
              paymentDate = Some("2020-04-23"),
              taxWeekNo = Some(32),
              taxMonthNo = Some(2)
            )),
            Some(Deductions(
              studentLoans = Some(StudentLoans(
                uglDeductionAmount = Some(100.00),
                pglDeductionAmount = Some(100.00)
              ))
            ))
          )),
          employmentBenefits = Some(
            EmploymentBenefits(
              "2020-01-04T05:01:01Z",
              benefits = Some(Benefits(
                Some(100), Some(100), Some(100), Some(100), Some(100), Some(100), Some(100), Some(100), Some(100), Some(100),
                Some(100), Some(100), Some(100), Some(100), Some(100), Some(100), Some(100), Some(100), Some(100), Some(100),
                Some(100), Some(100), Some(100), Some(100), Some(100), Some(100), Some(100), Some(100)
              ))
            )
          )
        )
      ),
      hmrcExpenses = Some(
        EmploymentExpenses(
          Some("2020-01-04T05:01:01Z"),
          Some("2020-01-04T05:01:01Z"),
          totalExpenses = Some(800),
          expenses = Some(Expenses(
            Some(100), Some(100), Some(100), Some(100), Some(100), Some(100), Some(100), Some(100)
          ))
        )
      ),
      Seq(
        EmploymentSource(
          employmentId = "00000000-0000-0000-2222-000000000000",
          employerRef = Some("666/66666"),
          employerName = "Business",
          payrollId = Some("1234567890"),
          startDate = Some("2020-01-01"),
          cessationDate = Some("2020-01-01"),
          dateIgnored = None,
          submittedOn = Some("2020-01-01T10:00:38Z"),
          employmentData = Some(
            EmploymentData(
              "2020-01-04T05:01:01Z",
              employmentSequenceNumber = Some("1002"),
              companyDirector = Some(false),
              closeCompany = Some(true),
              directorshipCeasedDate = Some("2020-02-12"),
              occPen = Some(false),
              disguisedRemuneration = Some(false),
              pay = Some(Pay(
                taxablePayToDate = Some(34234.15),
                totalTaxToDate = Some(6782.92),
                payFrequency = Some("CALENDAR MONTHLY"),
                paymentDate = Some("2020-04-23"),
                taxWeekNo = Some(32),
                taxMonthNo = Some(2)
              )),
              Some(Deductions(
                studentLoans = Some(StudentLoans(
                  uglDeductionAmount = Some(100.00),
                  pglDeductionAmount = Some(100.00)
                ))
              ))
            )
          ),
          employmentBenefits = Some(
            EmploymentBenefits(
              "2020-01-04T05:01:01Z",
              benefits = Some(Benefits(
                Some(100), Some(100), Some(100), Some(100), Some(100), Some(100), Some(100), Some(100), Some(100), Some(100),
                Some(100), Some(100), Some(100), Some(100), Some(100), Some(100), Some(100), Some(100), Some(100), Some(100),
                Some(100), Some(100), Some(100), Some(100), Some(100), Some(100), Some(100), Some(100)
              ))
            )
          )
        )
      ),
      customerExpenses = Some(
        EmploymentExpenses(
          Some("2020-01-04T05:01:01Z"),
          Some("2020-01-04T05:01:01Z"),
          totalExpenses = Some(800),
          expenses = Some(Expenses(
            Some(100), Some(100), Some(100), Some(100), Some(100), Some(100), Some(100), Some(100)
          ))
        )
      )
    )


  lazy val dividendsModel: Option[Dividends] = Some(Dividends(Some(100.00), Some(100.00)))
  lazy val interestsModel: Option[List[Interest]] = Some(List(Interest("TestName", "TestSource", Some(100.00), Some(100.00))))
  lazy val employmentsModel: AllEmploymentData = AllEmploymentData(
    hmrcEmploymentData = Seq(
      EmploymentSource(
        employmentId = "001",
        employerName = "maggie",
        employerRef = Some("223/AB12399"),
        payrollId = Some("123456789999"),
        startDate = Some("2019-04-21"),
        cessationDate = Some("2020-03-11"),
        dateIgnored = Some("2020-04-04T01:01:01Z"),
        submittedOn = Some("2020-01-04T05:01:01Z"),
        employmentData = Some(EmploymentData(
          submittedOn = "2020-02-12",
          employmentSequenceNumber = Some("123456789999"),
          companyDirector = Some(true),
          closeCompany = Some(false),
          directorshipCeasedDate = Some("2020-02-12"),
          occPen = Some(false),
          disguisedRemuneration = Some(false),
          pay = Some(Pay(Some(34234.15), Some(6782.92), Some("CALENDAR MONTHLY"), Some("2020-04-23"), Some(32), Some(2))),
          Some(Deductions(
            studentLoans = Some(StudentLoans(
              uglDeductionAmount = Some(100.00),
              pglDeductionAmount = Some(100.00)
            ))
          ))
        )),
        None
      )
    ),
    hmrcExpenses = None,
    customerEmploymentData = Seq(),
    customerExpenses = None
  )
  val giftAidPaymentsModel: Option[GiftAidPayments] = Some(GiftAidPayments(
    nonUkCharitiesCharityNames = Some(List("non uk charity name", "non uk charity name 2")),
    currentYear = Some(1234.56),
    oneOffCurrentYear = Some(1234.56),
    currentYearTreatedAsPreviousYear = Some(1234.56),
    nextYearTreatedAsCurrentYear = Some(1234.56),
    nonUkCharities = Some(1234.56),
  ))

  val giftsModel: Option[Gifts] = Some(Gifts(
    investmentsNonUkCharitiesCharityNames = Some(List("charity 1", "charity 2")),
    landAndBuildings = Some(10.21),
    sharesOrSecurities = Some(10.21),
    investmentsNonUkCharities = Some(1234.56)
  ))

  val giftAidModel: GiftAid = GiftAid(
    giftAidPaymentsModel,
    giftsModel
  )

  val fullPensionsModel = Pensions(
    pensionReliefs = Some(PensionReliefs(
      submittedOn = "2020-01-04T05:01:01Z",
      deletedOn = Some("2020-01-04T05:01:01Z"),
      pensionReliefs = Reliefs(
        regularPensionContributions = Some(100.01),
        oneOffPensionContributionsPaid = Some(100.01),
        retirementAnnuityPayments = Some(100.01),
        paymentToEmployersSchemeNoTaxRelief = Some(100.01),
        overseasPensionSchemeContributions = Some(100.01)))
    ),
    pensionCharges = Some(PensionCharges(
      submittedOn = "2020-07-27T17:00:19Z",
      pensionSavingsTaxCharges = Some(PensionSavingsTaxCharges(
        pensionSchemeTaxReference = Seq("00123456RA", "00123456RB"),
        lumpSumBenefitTakenInExcessOfLifetimeAllowance = Some(LifetimeAllowance(
          amount = 800.02,
          taxPaid = 200.02
        )),
        benefitInExcessOfLifetimeAllowance = Some(LifetimeAllowance(
          amount = 800.02,
          taxPaid = 200.02
        )),
        isAnnualAllowanceReduced = false,
        taperedAnnualAllowance = Some(false),
        moneyPurchasedAllowance = Some(false)
      )),
      pensionSchemeOverseasTransfers = Some(PensionSchemeOverseasTransfers(
        overseasSchemeProvider = Seq(OverseasSchemeProvider(
          providerName = "overseas providerName 1 qualifying scheme",
          providerAddress = "overseas address 1",
          providerCountryCode = "ESP",
          qualifyingRecognisedOverseasPensionScheme = Some(Seq("Q100000", "Q100002")),
          pensionSchemeTaxReference = None
        )),
        transferCharge = 22.77,
        transferChargeTaxPaid = 33.88
      )),
      pensionSchemeUnauthorisedPayments = Some(PensionSchemeUnauthorisedPayments(
        pensionSchemeTaxReference = Seq("00123456RA", "00123456RB"),
        surcharge = Some(Charge(
          amount = 124.44,
          foreignTaxPaid = 123.33
        )),
        noSurcharge = Some(Charge(
          amount = 222.44,
          foreignTaxPaid = 223.33
        ))
      )),
      pensionContributions = Some(PensionContributions(
        pensionSchemeTaxReference = Seq("00123456RA", "00123456RB"),
        inExcessOfTheAnnualAllowance = 150.67,
        annualAllowanceTaxPaid = 178.65)),
      overseasPensionContributions = Some(OverseasPensionContributions(
        overseasSchemeProvider = Seq(OverseasSchemeProvider(
          providerName = "overseas providerName 1 tax ref",
          providerAddress = "overseas address 1",
          providerCountryCode = "ESP",
          qualifyingRecognisedOverseasPensionScheme = None,
          pensionSchemeTaxReference = Some(Seq("00123456RA", "00123456RB"))
        )),
        shortServiceRefund = 1.11,
        shortServiceRefundTaxPaid = 2.22
      )))
    ),
    Some(StateBenefitsModel(
      Some(StateBenefits(
        incapacityBenefit = Some(List(StateBenefit(
          benefitId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c934",
          startDate = "2019-11-13",
          dateIgnored = Some("2019-04-11T16:22:00Z"),
          submittedOn = Some("2020-09-11T17:23:00Z"),
          endDate = Some("2020-08-23"),
          amount = Some(1212.34),
          taxPaid = Some(22323.23)
        ))),
        statePension = Some(StateBenefit(
          benefitId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c935",
          startDate = "2018-06-03",
          dateIgnored = Some("2018-09-09T19:23:00Z"),
          submittedOn = Some("2020-08-07T12:23:00Z"),
          endDate = Some("2020-09-13"),
          amount = Some(42323.23),
          taxPaid = Some(2323.44)
        )),
        statePensionLumpSum = Some(StateBenefit(
          benefitId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c936",
          startDate = "2019-04-23",
          dateIgnored = Some("2019-07-08T05:23:00Z"),
          submittedOn = Some("2020-03-13T19:23:00Z"),
          endDate = Some("2020-08-13"),
          amount = Some(45454.23),
          taxPaid = Some(45432.56)
        )),
        employmentSupportAllowance = Some(List(StateBenefit(
          benefitId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c937",
          startDate = "2019-09-23",
          dateIgnored = Some("2019-09-28T10:23:00Z"),
          submittedOn = Some("2020-11-13T19:23:00Z"),
          endDate = Some("2020-08-23"),
          amount = Some(44545.43),
          taxPaid = Some(35343.23)
        ))),
        jobSeekersAllowance = Some(List(StateBenefit(
          benefitId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c938",
          startDate = "2019-09-19",
          dateIgnored = Some("2019-08-18T13:23:00Z"),
          submittedOn = Some("2020-07-10T18:23:00Z"),
          endDate = Some("2020-09-23"),
          amount = Some(33223.12),
          taxPaid = Some(44224.56)
        ))),
        bereavementAllowance = Some(StateBenefit(
          benefitId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c939",
          startDate = "2019-05-22",
          dateIgnored = Some("2020-08-10T12:23:00Z"),
          submittedOn = Some("2020-09-19T19:23:00Z"),
          endDate = Some("2020-09-26"),
          amount = Some(56534.23),
          taxPaid = Some(34343.57)
        )),
        otherStateBenefits = Some(StateBenefit(
          benefitId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c940",
          startDate = "2018-09-03",
          dateIgnored = Some("2020-01-11T15:23:00Z"),
          submittedOn = Some("2020-09-13T15:23:00Z"),
          endDate = Some("2020-06-03"),
          amount = Some(56532.45),
          taxPaid = Some(5656.89)
        )),
      )),
      Some(StateBenefits(
        incapacityBenefit = Some(List(StateBenefit(
          benefitId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c941",
          startDate = "2018-07-17",
          submittedOn = Some("2020-11-17T19:23:00Z"),
          endDate = Some("2020-09-23"),
          amount = Some(45646.78),
          taxPaid = Some(4544.34),
          dateIgnored = None
        ))),
        statePension = Some(StateBenefit(
          benefitId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c943",
          startDate = "2018-04-03",
          submittedOn = Some("2020-06-11T10:23:00Z"),
          endDate = Some("2020-09-13"),
          amount = Some(45642.45),
          taxPaid = Some(6764.34),
          dateIgnored = None
        )),
        statePensionLumpSum = Some(StateBenefit(
          benefitId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c956",
          startDate = "2019-09-23",
          submittedOn = Some("2020-06-13T05:29:00Z"),
          endDate = Some("2020-09-26"),
          amount = Some(34322.34),
          taxPaid = Some(4564.45),
          dateIgnored = None
        )),
        employmentSupportAllowance = Some(List(StateBenefit(
          benefitId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c988",
          startDate = "2019-09-11",
          submittedOn = Some("2020-02-10T11:20:00Z"),
          endDate = Some("2020-06-13"),
          amount = Some(45424.23),
          taxPaid = Some(23232.34),
          dateIgnored = None
        ))),
        jobSeekersAllowance = Some(List(StateBenefit(
          benefitId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c990",
          startDate = "2019-07-10",
          submittedOn = Some("2020-05-13T14:23:00Z"),
          endDate = Some("2020-05-11"),
          amount = Some(34343.78),
          taxPaid = Some(3433.56),
          dateIgnored = None
        ))),
        bereavementAllowance = Some(StateBenefit(
          benefitId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c997",
          startDate = "2018-08-12",
          submittedOn = Some("2020-02-13T11:23:00Z"),
          endDate = Some("2020-07-13"),
          amount = Some(45423.45),
          taxPaid = Some(4543.64),
          dateIgnored = None
        )),
        otherStateBenefits = Some(StateBenefit(
          benefitId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c957",
          startDate = "2018-01-13",
          submittedOn = Some("2020-09-12T12:23:00Z"),
          endDate = Some("2020-08-13"),
          amount = Some(63333.33),
          taxPaid = Some(4644.45),
          dateIgnored = None
        )),
      )))
    )
  )

  val userData: UserData = UserData(
    "sessionId-1618a1e8-4979-41d8-a32e-5ffbe69fac81",
    "1234567890",
    "AA123456A",
    2022,
    dividendsModel,
    interestsModel,
    Some(giftAidModel),
    Some(employmentsModel)
  )

  val incomeSourcesResponse: IncomeSourcesResponseModel = IncomeSourcesResponseModel(Some(Dividends(Some(123456.78), Some(123456.78))),
    Some(Seq(Interest("someName", "12345", Some(12345.67), Some(12345.67)))), Some(GiftAid(giftAidPaymentsModel, giftsModel)),
    Some(allEmploymentData), Some(fullPensionsModel))
}

