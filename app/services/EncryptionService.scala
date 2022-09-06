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

import config.AppConfig
import models.employment._
import models.gifts._
import models.mongo.{EncryptedUserData, TextAndKey, UserData}
import models.pensions._
import models.pensions.charges._
import models.pensions.income._
import models.pensions.reliefs.{EncryptedPensionReliefs, EncryptedReliefs, PensionReliefs, Reliefs}
import models.pensions.statebenefits._
import models.{Dividends, EncryptedDividends, EncryptedInterest, Interest}
import utils.SecureGCMCipher
import javax.inject.Inject
import models.pensions.employmentPensions.{EmploymentPensionModel, EmploymentPensions, EncryptedEmploymentPensionModel, EncryptedEmploymentPensions}

//scalastyle:off
class EncryptionService @Inject()(encryptionService: SecureGCMCipher, appConfig: AppConfig) {
  implicit val secureGCMCipher: SecureGCMCipher = encryptionService

  def encryptUserData(userData: UserData): EncryptedUserData = {
    implicit val textAndKey: TextAndKey = TextAndKey(userData.mtdItId, appConfig.encryptionKey)

    EncryptedUserData(
      sessionId = userData.sessionId,
      mtdItId = userData.mtdItId,
      nino = userData.nino,
      taxYear = userData.taxYear,
      dividends = userData.dividends.map(encryptDividends),
      interest = userData.interest.map(encryptInterest),
      giftAid = userData.giftAid.map(encryptGiftAid),
      employment = userData.employment.map(encryptEmployment),
      pensions = userData.pensions.map(encryptPensions),
      cis = userData.cis.map(_.encrypted),
      lastUpdated = userData.lastUpdated
    )
  }

  private def encryptDividends(dividends: Dividends)(implicit textAndKey: TextAndKey): EncryptedDividends = {
    EncryptedDividends(
      ukDividends = dividends.ukDividends.map(encryptionService.encrypt[BigDecimal]),
      otherUkDividends = dividends.otherUkDividends.map(encryptionService.encrypt[BigDecimal])
    )
  }

  private def encryptInterest(interest: Seq[Interest])(implicit textAndKey: TextAndKey): Seq[EncryptedInterest] = {
    interest.map {
      interest =>
        EncryptedInterest(
          accountName = encryptionService.encrypt[String](interest.accountName),
          incomeSourceId = encryptionService.encrypt[String](interest.incomeSourceId),
          taxedUkInterest = interest.taxedUkInterest.map(encryptionService.encrypt[BigDecimal]),
          untaxedUkInterest = interest.untaxedUkInterest.map(encryptionService.encrypt[BigDecimal])
        )
    }
  }

  private def encryptGiftAid(giftAid: GiftAid)(implicit textAndKey: TextAndKey): EncryptedGiftAid = {

    val eGiftAidPayments: Option[EncryptedGiftAidPayments] = {
      giftAid.giftAidPayments.map {
        g =>
          EncryptedGiftAidPayments(
            nonUkCharitiesCharityNames = g.nonUkCharitiesCharityNames.map(_.map(encryptionService.encrypt)),
            currentYear = g.currentYear.map(encryptionService.encrypt),
            oneOffCurrentYear = g.oneOffCurrentYear.map(encryptionService.encrypt),
            currentYearTreatedAsPreviousYear = g.currentYearTreatedAsPreviousYear.map(encryptionService.encrypt),
            nextYearTreatedAsCurrentYear = g.nextYearTreatedAsCurrentYear.map(encryptionService.encrypt),
            nonUkCharities = g.nonUkCharities.map(encryptionService.encrypt)
          )
      }
    }
    val eGifts: Option[EncryptedGifts] = {
      giftAid.gifts.map {
        g =>
          EncryptedGifts(
            investmentsNonUkCharitiesCharityNames = g.investmentsNonUkCharitiesCharityNames.map(_.map(encryptionService.encrypt)),
            landAndBuildings = g.landAndBuildings.map(encryptionService.encrypt),
            sharesOrSecurities = g.sharesOrSecurities.map(encryptionService.encrypt),
            investmentsNonUkCharities = g.investmentsNonUkCharities.map(encryptionService.encrypt)
          )
      }
    }

    EncryptedGiftAid(giftAidPayments = eGiftAidPayments, gifts = eGifts)
  }

  private def encryptEmployment(employment: AllEmploymentData)(implicit textAndKey: TextAndKey): EncryptedAllEmploymentData = {

    val hmrcExpenses = employment.hmrcExpenses.map {
      e =>
        EncryptedEmploymentExpenses(
          submittedOn = e.submittedOn.map(encryptionService.encrypt),
          dateIgnored = e.dateIgnored.map(encryptionService.encrypt),
          totalExpenses = e.totalExpenses.map(encryptionService.encrypt),
          expenses = e.expenses.map(encryptExpenses)
        )
    }

    val customerExpenses = employment.customerExpenses.map {
      e =>
        EncryptedEmploymentExpenses(
          submittedOn = e.submittedOn.map(encryptionService.encrypt),
          dateIgnored = e.dateIgnored.map(encryptionService.encrypt),
          totalExpenses = e.totalExpenses.map(encryptionService.encrypt),
          expenses = e.expenses.map(encryptExpenses)
        )
    }

    val hmrcEmploymentData = employment.hmrcEmploymentData.map(encryptHmrcEmploymentSource)
    val customerEmploymentData = employment.customerEmploymentData.map(encryptEmploymentSource)

    EncryptedAllEmploymentData(
      hmrcEmploymentData = hmrcEmploymentData,
      hmrcExpenses = hmrcExpenses,
      customerEmploymentData = customerEmploymentData,
      customerExpenses = customerExpenses
    )
  }

  private def encryptHmrcEmploymentSource(e: HmrcEmploymentSource)
                                         (implicit textAndKey: TextAndKey): EncryptedHmrcEmploymentSource = EncryptedHmrcEmploymentSource(
    employmentId = encryptionService.encrypt(e.employmentId),
    employerName = encryptionService.encrypt(e.employerName),
    employerRef = e.employerRef.map(encryptionService.encrypt),
    payrollId = e.payrollId.map(encryptionService.encrypt),
    startDate = e.startDate.map(encryptionService.encrypt),
    cessationDate = e.cessationDate.map(encryptionService.encrypt),
    dateIgnored = e.dateIgnored.map(encryptionService.encrypt),
    submittedOn = e.submittedOn.map(encryptionService.encrypt),
    hmrcEmploymentFinancialData = e.hmrcEmploymentFinancialData.map(encryptEmploymentFinancialData),
    customerEmploymentFinancialData = e.customerEmploymentFinancialData.map(encryptEmploymentFinancialData),
    occupationalPension = e.occupationalPension.map(encryptionService.encrypt)
  )

  private def encryptEmploymentFinancialData(e: EmploymentFinancialData)(implicit textAndKey: TextAndKey): EncryptedEmploymentFinancialData = {

    EncryptedEmploymentFinancialData(
      employmentData = e.employmentData.map(encryptEmploymentData),
      employmentBenefits = e.employmentBenefits.map(encryptEmploymentBenefits)
    )
  }

  private def encryptEmploymentSource(e: EmploymentSource)
                                     (implicit textAndKey: TextAndKey): EncryptedEmploymentSource = EncryptedEmploymentSource(
    employmentId = encryptionService.encrypt(e.employmentId),
    employerName = encryptionService.encrypt(e.employerName),
    employerRef = e.employerRef.map(encryptionService.encrypt),
    payrollId = e.payrollId.map(encryptionService.encrypt),
    startDate = e.startDate.map(encryptionService.encrypt),
    cessationDate = e.cessationDate.map(encryptionService.encrypt),
    dateIgnored = e.dateIgnored.map(encryptionService.encrypt),
    submittedOn = e.submittedOn.map(encryptionService.encrypt),
    employmentData = e.employmentData.map(encryptEmploymentData),
    employmentBenefits = e.employmentBenefits.map(encryptEmploymentBenefits),
    occupationalPension = e.occupationalPension.map(encryptionService.encrypt)
  )

  private def decryptEmploymentSource(e: EncryptedEmploymentSource)
                                     (implicit textAndKey: TextAndKey): EmploymentSource = EmploymentSource(
    employmentId = encryptionService.decrypt[String](e.employmentId.value, e.employmentId.nonce),
    employerName = encryptionService.decrypt[String](e.employerName.value, e.employerName.nonce),
    employerRef = e.employerRef.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
    payrollId = e.payrollId.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
    startDate = e.startDate.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
    cessationDate = e.cessationDate.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
    dateIgnored = e.dateIgnored.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
    submittedOn = e.submittedOn.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
    employmentData = e.employmentData.map(decryptEmploymentData),
    employmentBenefits = e.employmentBenefits.map(decryptEmploymentBenefits),
    occupationalPension = e.occupationalPension.map(x => encryptionService.decrypt[Boolean](x.value, x.nonce))
  )

  private def decryptHmrcEmploymentSource(e: EncryptedHmrcEmploymentSource)
                                         (implicit textAndKey: TextAndKey): HmrcEmploymentSource = HmrcEmploymentSource(
    employmentId = encryptionService.decrypt[String](e.employmentId.value, e.employmentId.nonce),
    employerName = encryptionService.decrypt[String](e.employerName.value, e.employerName.nonce),
    employerRef = e.employerRef.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
    payrollId = e.payrollId.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
    startDate = e.startDate.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
    cessationDate = e.cessationDate.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
    dateIgnored = e.dateIgnored.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
    submittedOn = e.submittedOn.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
    hmrcEmploymentFinancialData = e.hmrcEmploymentFinancialData.map(decryptEmploymentFinancialData),
    customerEmploymentFinancialData = e.customerEmploymentFinancialData.map(decryptEmploymentFinancialData),
    occupationalPension = e.occupationalPension.map(x => encryptionService.decrypt[Boolean](x.value, x.nonce))
  )

  private def decryptEmploymentFinancialData(e: EncryptedEmploymentFinancialData)(implicit textAndKey: TextAndKey): EmploymentFinancialData = {

    EmploymentFinancialData(
      employmentData = e.employmentData.map(decryptEmploymentData),
      employmentBenefits = e.employmentBenefits.map(decryptEmploymentBenefits)
    )
  }

  private def encryptEmploymentData(e: EmploymentData)(implicit textAndKey: TextAndKey): EncryptedEmploymentData = {

    val pay = e.pay.map { p =>
      EncryptedPay(
        taxablePayToDate = p.taxablePayToDate.map(encryptionService.encrypt),
        totalTaxToDate = p.totalTaxToDate.map(encryptionService.encrypt),
        payFrequency = p.payFrequency.map(encryptionService.encrypt),
        paymentDate = p.paymentDate.map(encryptionService.encrypt),
        taxWeekNo = p.taxWeekNo.map(encryptionService.encrypt),
        taxMonthNo = p.taxMonthNo.map(encryptionService.encrypt)
      )
    }

    val deductions = e.deductions.map {
      d =>

        val studentLoans = d.studentLoans.map {
          s =>
            EncryptedStudentLoans(
              uglDeductionAmount = s.uglDeductionAmount.map(encryptionService.encrypt),
              pglDeductionAmount = s.pglDeductionAmount.map(encryptionService.encrypt))
        }

        EncryptedDeductions(studentLoans)
    }

    EncryptedEmploymentData(
      submittedOn = encryptionService.encrypt(e.submittedOn),
      employmentSequenceNumber = e.employmentSequenceNumber.map(encryptionService.encrypt),
      companyDirector = e.companyDirector.map(encryptionService.encrypt),
      closeCompany = e.closeCompany.map(encryptionService.encrypt),
      directorshipCeasedDate = e.directorshipCeasedDate.map(encryptionService.encrypt),
      occPen = e.occPen.map(encryptionService.encrypt),
      disguisedRemuneration = e.disguisedRemuneration.map(encryptionService.encrypt),
      pay = pay,
      deductions = deductions
    )
  }

  private def decryptEmploymentData(e: EncryptedEmploymentData)(implicit textAndKey: TextAndKey): EmploymentData = {

    val pay = e.pay.map { p =>
      Pay(
        taxablePayToDate = p.taxablePayToDate.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
        totalTaxToDate = p.totalTaxToDate.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
        payFrequency = p.payFrequency.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
        paymentDate = p.paymentDate.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
        taxWeekNo = p.taxWeekNo.map(x => encryptionService.decrypt[Int](x.value, x.nonce)),
        taxMonthNo = p.taxMonthNo.map(x => encryptionService.decrypt[Int](x.value, x.nonce))
      )
    }

    val deductions = e.deductions.map {
      d =>

        val studentLoans = d.studentLoans.map {
          s =>
            StudentLoans(uglDeductionAmount = s.uglDeductionAmount.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
              pglDeductionAmount = s.pglDeductionAmount.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)))
        }

        Deductions(studentLoans)
    }

    EmploymentData(
      submittedOn = encryptionService.decrypt[String](e.submittedOn.value, e.submittedOn.nonce),
      employmentSequenceNumber = e.employmentSequenceNumber.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
      companyDirector = e.companyDirector.map(x => encryptionService.decrypt[Boolean](x.value, x.nonce)),
      closeCompany = e.closeCompany.map(x => encryptionService.decrypt[Boolean](x.value, x.nonce)),
      directorshipCeasedDate = e.directorshipCeasedDate.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
      occPen = e.occPen.map(x => encryptionService.decrypt[Boolean](x.value, x.nonce)),
      disguisedRemuneration = e.disguisedRemuneration.map(x => encryptionService.decrypt[Boolean](x.value, x.nonce)),
      pay = pay,
      deductions = deductions
    )
  }

  private def encryptEmploymentBenefits(e: EmploymentBenefits)(implicit textAndKey: TextAndKey): EncryptedEmploymentBenefits = {

    val benefits = e.benefits.map {
      b =>
        EncryptedBenefits(
          accommodation = b.accommodation.map(encryptionService.encrypt),
          assets = b.assets.map(encryptionService.encrypt),
          assetTransfer = b.assetTransfer.map(encryptionService.encrypt),
          beneficialLoan = b.beneficialLoan.map(encryptionService.encrypt),
          car = b.car.map(encryptionService.encrypt),
          carFuel = b.carFuel.map(encryptionService.encrypt),
          educationalServices = b.educationalServices.map(encryptionService.encrypt),
          entertaining = b.entertaining.map(encryptionService.encrypt),
          expenses = b.expenses.map(encryptionService.encrypt),
          medicalInsurance = b.medicalInsurance.map(encryptionService.encrypt),
          telephone = b.telephone.map(encryptionService.encrypt),
          service = b.service.map(encryptionService.encrypt),
          taxableExpenses = b.taxableExpenses.map(encryptionService.encrypt),
          van = b.van.map(encryptionService.encrypt),
          vanFuel = b.vanFuel.map(encryptionService.encrypt),
          mileage = b.mileage.map(encryptionService.encrypt),
          nonQualifyingRelocationExpenses = b.nonQualifyingRelocationExpenses.map(encryptionService.encrypt),
          nurseryPlaces = b.nurseryPlaces.map(encryptionService.encrypt),
          otherItems = b.otherItems.map(encryptionService.encrypt),
          paymentsOnEmployeesBehalf = b.paymentsOnEmployeesBehalf.map(encryptionService.encrypt),
          personalIncidentalExpenses = b.personalIncidentalExpenses.map(encryptionService.encrypt),
          qualifyingRelocationExpenses = b.qualifyingRelocationExpenses.map(encryptionService.encrypt),
          employerProvidedProfessionalSubscriptions = b.employerProvidedProfessionalSubscriptions.map(encryptionService.encrypt),
          employerProvidedServices = b.employerProvidedServices.map(encryptionService.encrypt),
          incomeTaxPaidByDirector = b.incomeTaxPaidByDirector.map(encryptionService.encrypt),
          travelAndSubsistence = b.travelAndSubsistence.map(encryptionService.encrypt),
          vouchersAndCreditCards = b.vouchersAndCreditCards.map(encryptionService.encrypt),
          nonCash = b.nonCash.map(encryptionService.encrypt)
        )
    }

    EncryptedEmploymentBenefits(submittedOn = encryptionService.encrypt(e.submittedOn), benefits = benefits)
  }

  private def decryptEmploymentBenefits(e: EncryptedEmploymentBenefits)(implicit textAndKey: TextAndKey): EmploymentBenefits = {

    val benefits = e.benefits.map {
      b =>
        Benefits(
          accommodation = b.accommodation.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
          assets = b.assets.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
          assetTransfer = b.assetTransfer.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
          beneficialLoan = b.beneficialLoan.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
          car = b.car.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
          carFuel = b.carFuel.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
          educationalServices = b.educationalServices.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
          entertaining = b.entertaining.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
          expenses = b.expenses.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
          medicalInsurance = b.medicalInsurance.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
          telephone = b.telephone.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
          service = b.service.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
          taxableExpenses = b.taxableExpenses.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
          van = b.van.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
          vanFuel = b.vanFuel.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
          mileage = b.mileage.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
          nonQualifyingRelocationExpenses = b.nonQualifyingRelocationExpenses.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
          nurseryPlaces = b.nurseryPlaces.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
          otherItems = b.otherItems.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
          paymentsOnEmployeesBehalf = b.paymentsOnEmployeesBehalf.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
          personalIncidentalExpenses = b.personalIncidentalExpenses.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
          qualifyingRelocationExpenses = b.qualifyingRelocationExpenses.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
          employerProvidedProfessionalSubscriptions = b.employerProvidedProfessionalSubscriptions.map(
            x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
          employerProvidedServices = b.employerProvidedServices.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
          incomeTaxPaidByDirector = b.incomeTaxPaidByDirector.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
          travelAndSubsistence = b.travelAndSubsistence.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
          vouchersAndCreditCards = b.vouchersAndCreditCards.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
          nonCash = b.nonCash.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce))
        )
    }

    EmploymentBenefits(submittedOn = encryptionService.decrypt[String](e.submittedOn.value, e.submittedOn.nonce), benefits = benefits)
  }

  private def encryptExpenses(e: Expenses)(implicit textAndKey: TextAndKey): EncryptedExpenses = {
    EncryptedExpenses(
      businessTravelCosts = e.businessTravelCosts.map(encryptionService.encrypt),
      jobExpenses = e.jobExpenses.map(encryptionService.encrypt),
      flatRateJobExpenses = e.flatRateJobExpenses.map(encryptionService.encrypt),
      professionalSubscriptions = e.professionalSubscriptions.map(encryptionService.encrypt),
      hotelAndMealExpenses = e.hotelAndMealExpenses.map(encryptionService.encrypt),
      otherAndCapitalAllowances = e.otherAndCapitalAllowances.map(encryptionService.encrypt),
      vehicleExpenses = e.vehicleExpenses.map(encryptionService.encrypt),
      mileageAllowanceRelief = e.mileageAllowanceRelief.map(encryptionService.encrypt)
    )
  }

  private def decryptExpenses(e: EncryptedExpenses)(implicit textAndKey: TextAndKey): Expenses = {
    Expenses(
      businessTravelCosts = e.businessTravelCosts.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
      jobExpenses = e.jobExpenses.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
      flatRateJobExpenses = e.flatRateJobExpenses.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
      professionalSubscriptions = e.professionalSubscriptions.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
      hotelAndMealExpenses = e.hotelAndMealExpenses.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
      otherAndCapitalAllowances = e.otherAndCapitalAllowances.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
      vehicleExpenses = e.vehicleExpenses.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
      mileageAllowanceRelief = e.mileageAllowanceRelief.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce))
    )
  }

  def decryptUserData(userData: EncryptedUserData): UserData = {
    implicit val textAndKey: TextAndKey = TextAndKey(userData.mtdItId, appConfig.encryptionKey)

    UserData(
      sessionId = userData.sessionId,
      mtdItId = userData.mtdItId,
      nino = userData.nino,
      taxYear = userData.taxYear,
      dividends = userData.dividends.map(decryptDividends),
      interest = userData.interest.map(decryptInterest),
      giftAid = userData.giftAid.map(decryptGiftAid),
      employment = userData.employment.map(decryptEmployment),
      pensions = userData.pensions.map(decryptPensions),
      cis = userData.cis.map(_.decrypted),
      lastUpdated = userData.lastUpdated
    )
  }

  private def decryptDividends(dividends: EncryptedDividends)(implicit textAndKey: TextAndKey): Dividends = {
    Dividends(
      dividends.ukDividends.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
      dividends.otherUkDividends.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce))
    )
  }

  private def decryptInterest(interest: Seq[EncryptedInterest])(implicit textAndKey: TextAndKey): Seq[Interest] = {
    interest.map {
      interest =>
        Interest(
          accountName = encryptionService.decrypt[String](interest.accountName.value, interest.accountName.nonce),
          incomeSourceId = encryptionService.decrypt[String](interest.incomeSourceId.value, interest.incomeSourceId.nonce),
          taxedUkInterest = interest.taxedUkInterest.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
          untaxedUkInterest = interest.untaxedUkInterest.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce))
        )
    }
  }

  private def decryptGiftAid(giftAid: EncryptedGiftAid)(implicit textAndKey: TextAndKey): GiftAid = {

    val dGiftAidPayments: Option[GiftAidPayments] = {
      giftAid.giftAidPayments.map {
        g =>
          GiftAidPayments(
            nonUkCharitiesCharityNames = g.nonUkCharitiesCharityNames.map(_.map(x => encryptionService.decrypt[String](x.value, x.nonce))),
            currentYear = g.currentYear.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
            oneOffCurrentYear = g.oneOffCurrentYear.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
            currentYearTreatedAsPreviousYear = g.currentYearTreatedAsPreviousYear.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
            nextYearTreatedAsCurrentYear = g.nextYearTreatedAsCurrentYear.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
            nonUkCharities = g.nonUkCharities.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce))
          )
      }
    }

    val dGifts: Option[Gifts] = {
      giftAid.gifts.map {
        g =>
          Gifts(
            investmentsNonUkCharitiesCharityNames = g.investmentsNonUkCharitiesCharityNames.map(_.map(x => encryptionService.decrypt[String](x.value, x.nonce))),
            landAndBuildings = g.landAndBuildings.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
            sharesOrSecurities = g.sharesOrSecurities.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
            investmentsNonUkCharities = g.investmentsNonUkCharities.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce))
          )
      }
    }

    GiftAid(giftAidPayments = dGiftAidPayments, gifts = dGifts)
  }

  private def decryptEmployment(employment: EncryptedAllEmploymentData)(implicit textAndKey: TextAndKey): AllEmploymentData = {
    val hmrcExpenses = employment.hmrcExpenses.map {
      e =>
        EmploymentExpenses(
          submittedOn = e.submittedOn.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
          dateIgnored = e.dateIgnored.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
          totalExpenses = e.totalExpenses.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
          expenses = e.expenses.map(decryptExpenses)
        )
    }

    val customerExpenses = employment.customerExpenses.map {
      e =>
        EmploymentExpenses(
          submittedOn = e.submittedOn.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
          dateIgnored = e.dateIgnored.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
          totalExpenses = e.totalExpenses.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
          expenses = e.expenses.map(decryptExpenses)
        )
    }
    val hmrcEmploymentData = employment.hmrcEmploymentData.map(decryptHmrcEmploymentSource)
    val customerEmploymentData = employment.customerEmploymentData.map(decryptEmploymentSource)

    AllEmploymentData(
      hmrcEmploymentData = hmrcEmploymentData,
      hmrcExpenses = hmrcExpenses,
      customerEmploymentData = customerEmploymentData,
      customerExpenses = customerExpenses
    )
  }

  private def encryptPensions(pensions: Pensions)(implicit textAndKey: TextAndKey): EncryptedPensions = {
    val ePensionReliefs: Option[EncryptedPensionReliefs] = {
      pensions.pensionReliefs.map {
        r =>
          EncryptedPensionReliefs(
            submittedOn = encryptionService.encrypt(r.submittedOn),
            deletedOn = r.deletedOn.map(encryptionService.encrypt),
            pensionReliefs = encryptReliefs(r.pensionReliefs)
          )
      }
    }

    val ePensionCharges: Option[EncryptedPensionCharges] = {
      pensions.pensionCharges.map {
        c =>
          EncryptedPensionCharges(
            submittedOn = encryptionService.encrypt(c.submittedOn),
            pensionSavingsTaxCharges = c.pensionSavingsTaxCharges.map(encryptPensionSavingsTaxCharges),
            pensionSchemeOverseasTransfers = c.pensionSchemeOverseasTransfers.map(encryptPensionSchemeOverseasTransfers),
            pensionSchemeUnauthorisedPayments = c.pensionSchemeUnauthorisedPayments.map(encryptPensionSchemeUnauthorisedPayments),
            pensionContributions = c.pensionContributions.map(encryptPensionContributions),
            overseasPensionContributions = c.overseasPensionContributions.map(encryptOverseasPensionContributions)
          )
      }
    }

    val eStateBenefitsModel: Option[EncryptedStateBenefitsModel] = {
      pensions.stateBenefits.map {
        s =>
          EncryptedStateBenefitsModel(
            stateBenefits = s.stateBenefits.map(encryptStateBenefits),
            customerAddedStateBenefits = s.customerAddedStateBenefits.map(encryptStateBenefits)
          )
      }
    }

    val eEmploymentPensions: Option[EncryptedEmploymentPensions] = {
      pensions.employmentPensions.map {
        e =>
          EncryptedEmploymentPensions(
            employmentData = e.employmentData.map(encryptEmploymentPensionModel)
          )
      }
    }

    val ePensionIncomeModel: Option[EncryptedPensionIncomeModel] = {
      pensions.pensionIncome.map {
        pI =>
          EncryptedPensionIncomeModel(
            submittedOn = encryptionService.encrypt(pI.submittedOn),
            deletedOn = pI.deletedOn.map(encryptionService.encrypt),
            foreignPension = pI.foreignPension.map(encryptForeignPension),
            overseasPensionContribution = pI.overseasPensionContribution.map(encryptOverseasPensionContribution)
          )
      }
    }

    EncryptedPensions(
      pensionReliefs = ePensionReliefs,
      pensionCharges = ePensionCharges,
      stateBenefits = eStateBenefitsModel,
      employmentPensions = eEmploymentPensions,
      pensionIncome = ePensionIncomeModel
    )
  }

  private def encryptEmploymentPensionModel(e: EmploymentPensionModel)(implicit textAndKey: TextAndKey): EncryptedEmploymentPensionModel = {
    EncryptedEmploymentPensionModel(
      employmentId = encryptionService.encrypt(e.employmentId),
      pensionSchemeName = encryptionService.encrypt(e.pensionSchemeName),
      pensionSchemeRef = e.pensionSchemeRef.map(encryptionService.encrypt),
      pensionId = e.pensionId.map(encryptionService.encrypt),
      startDate = e.startDate.map(encryptionService.encrypt),
      endDate = e.endDate.map(encryptionService.encrypt),
      amount = e.amount.map(encryptionService.encrypt),
      taxPaid = e.taxPaid.map(encryptionService.encrypt),
      isCustomerEmploymentData = e.isCustomerEmploymentData.map(encryptionService.encrypt)
    )
  }

  private def decryptEmploymentPensions(e: EncryptedEmploymentPensions)(implicit textAndKey: TextAndKey): EmploymentPensions = {
    EmploymentPensions(
      employmentData = e.employmentData.map(decryptEmploymentPensionModel)
    )
  }

  private def decryptEmploymentPensionModel(e: EncryptedEmploymentPensionModel)(implicit textAndKey: TextAndKey): EmploymentPensionModel = {
    EmploymentPensionModel(
      employmentId = encryptionService.decrypt[String](e.employmentId.value, e.employmentId.nonce),
      pensionSchemeName = encryptionService.decrypt[String](e.pensionSchemeName.value, e.pensionSchemeName.nonce),
      pensionSchemeRef = e.pensionSchemeRef.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
      pensionId = e.pensionId.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
      startDate = e.startDate.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
      endDate = e.endDate.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
      amount = e.amount.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
      taxPaid = e.taxPaid.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
      isCustomerEmploymentData = e.isCustomerEmploymentData.map(x => encryptionService.decrypt[Boolean](x.value, x.nonce))
    )
  }

  private def encryptForeignPension(f: ForeignPension)(implicit textAndKey: TextAndKey): EncryptedForeignPension = {
    EncryptedForeignPension(
      countryCode = encryptionService.encrypt(f.countryCode),
      taxableAmount = encryptionService.encrypt(f.taxableAmount),
      amountBeforeTax = f.amountBeforeTax.map(encryptionService.encrypt),
      taxTakenOff = f.taxTakenOff.map(encryptionService.encrypt),
      specialWithholdingTax = f.specialWithholdingTax.map(encryptionService.encrypt),
      foreignTaxCreditRelief = f.foreignTaxCreditRelief.map(encryptionService.encrypt)
    )
  }

  private def encryptOverseasPensionContribution(o: OverseasPensionContribution)(implicit textAndKey: TextAndKey): EncryptedOverseasPensionContribution = {
    EncryptedOverseasPensionContribution(
      customerReference = o.customerReference.map(encryptionService.encrypt),
      exemptEmployersPensionContribs = encryptionService.encrypt(o.exemptEmployersPensionContribs),
      migrantMemReliefQopsRefNo = o.migrantMemReliefQopsRefNo.map(encryptionService.encrypt),
      dblTaxationRelief = o.dblTaxationRelief.map(encryptionService.encrypt),
      dblTaxationCountry = o.dblTaxationCountry.map(encryptionService.encrypt),
      dblTaxationArticle = o.dblTaxationArticle.map(encryptionService.encrypt),
      dblTaxationTreaty = o.dblTaxationTreaty.map(encryptionService.encrypt),
      sf74Reference = o.sf74Reference.map(encryptionService.encrypt)
    )
  }

  private def encryptStateBenefits(s: StateBenefits)(implicit textAndKey: TextAndKey): EncryptedStateBenefits = {
    EncryptedStateBenefits(
      incapacityBenefit = s.incapacityBenefit.map(_.map(encryptStateBenefit)),
      statePension = s.statePension.map(encryptStateBenefit),
      statePensionLumpSum = s.statePensionLumpSum.map(encryptStateBenefit),
      employmentSupportAllowance = s.employmentSupportAllowance.map(_.map(encryptStateBenefit)),
      jobSeekersAllowance = s.jobSeekersAllowance.map(_.map(encryptStateBenefit)),
      bereavementAllowance = s.bereavementAllowance.map(encryptStateBenefit),
      otherStateBenefits = s.otherStateBenefits.map(encryptStateBenefit)
    )
  }

  private def encryptStateBenefit(s: StateBenefit)(implicit textAndKey: TextAndKey): EncryptedStateBenefit = {
    EncryptedStateBenefit(
      benefitId = encryptionService.encrypt(s.benefitId),
      startDate = encryptionService.encrypt(s.startDate),
      dateIgnored = s.dateIgnored.map(encryptionService.encrypt),
      submittedOn = s.submittedOn.map(encryptionService.encrypt),
      endDate = s.endDate.map(encryptionService.encrypt),
      amount = s.amount.map(encryptionService.encrypt),
      taxPaid = s.taxPaid.map(encryptionService.encrypt)
    )
  }

  private def encryptPensionContributions(p: PensionContributions)(implicit textAndKey: TextAndKey): EncryptedPensionContributions = {
    EncryptedPensionContributions(
      pensionSchemeTaxReference = p.pensionSchemeTaxReference.map(encryptionService.encrypt),
      inExcessOfTheAnnualAllowance = encryptionService.encrypt(p.inExcessOfTheAnnualAllowance),
      annualAllowanceTaxPaid = encryptionService.encrypt(p.annualAllowanceTaxPaid)
    )
  }

  private def encryptPensionSchemeUnauthorisedPayments(p: PensionSchemeUnauthorisedPayments)
                                                      (implicit textAndKey: TextAndKey): EncryptedPensionSchemeUnauthorisedPayments = {
    EncryptedPensionSchemeUnauthorisedPayments(
      pensionSchemeTaxReference = p.pensionSchemeTaxReference.map(encryptionService.encrypt),
      surcharge = p.surcharge.map(encryptCharge),
      noSurcharge = p.noSurcharge.map(encryptCharge)
    )
  }

  private def encryptPensionSchemeOverseasTransfers(p: PensionSchemeOverseasTransfers)
                                                   (implicit textAndKey: TextAndKey): EncryptedPensionSchemeOverseasTransfers = {
    EncryptedPensionSchemeOverseasTransfers(
      overseasSchemeProvider = p.overseasSchemeProvider.map(encryptOverseasSchemeProvider),
      transferCharge = encryptionService.encrypt(p.transferCharge),
      transferChargeTaxPaid = encryptionService.encrypt(p.transferChargeTaxPaid)
    )
  }

  private def encryptPensionSavingsTaxCharges(p: PensionSavingsTaxCharges)(implicit textAndKey: TextAndKey): EncryptedPensionSavingsTaxCharges = {
    EncryptedPensionSavingsTaxCharges(
      pensionSchemeTaxReference = p.pensionSchemeTaxReference.map(encryptionService.encrypt),
      lumpSumBenefitTakenInExcessOfLifetimeAllowance = p.lumpSumBenefitTakenInExcessOfLifetimeAllowance.map(encryptLifeTimeAllowance),
      benefitInExcessOfLifetimeAllowance = p.benefitInExcessOfLifetimeAllowance.map(encryptLifeTimeAllowance),
      isAnnualAllowanceReduced = encryptionService.encrypt(p.isAnnualAllowanceReduced),
      taperedAnnualAllowance = p.taperedAnnualAllowance.map(encryptionService.encrypt),
      moneyPurchasedAllowance = p.moneyPurchasedAllowance.map(encryptionService.encrypt)
    )
  }

  private def encryptOverseasPensionContributions(o: OverseasPensionContributions)(implicit textAndKey: TextAndKey): EncryptedOverseasPensionContributions = {
    EncryptedOverseasPensionContributions(
      overseasSchemeProvider = o.overseasSchemeProvider.map(encryptOverseasSchemeProvider),
      shortServiceRefund = encryptionService.encrypt(o.shortServiceRefund),
      shortServiceRefundTaxPaid = encryptionService.encrypt(o.shortServiceRefundTaxPaid)
    )
  }

  private def encryptCharge(charge: Charge)(implicit textAndKey: TextAndKey): EncryptedCharge = {
    EncryptedCharge(
      amount = encryptionService.encrypt(charge.amount),
      foreignTaxPaid = encryptionService.encrypt(charge.foreignTaxPaid)
    )
  }

  private def encryptOverseasSchemeProvider(overseasSchemeProvider: OverseasSchemeProvider)
                                           (implicit textAndKey: TextAndKey): EncryptedOverseasSchemeProvider = {
    EncryptedOverseasSchemeProvider(
      providerName = encryptionService.encrypt(overseasSchemeProvider.providerName),
      providerAddress = encryptionService.encrypt(overseasSchemeProvider.providerAddress),
      providerCountryCode = encryptionService.encrypt(overseasSchemeProvider.providerCountryCode),
      qualifyingRecognisedOverseasPensionScheme = overseasSchemeProvider.qualifyingRecognisedOverseasPensionScheme.map(_.map(
        x => encryptionService.encrypt(x))),
      pensionSchemeTaxReference = overseasSchemeProvider.pensionSchemeTaxReference.map(_.map(x => encryptionService.encrypt(x)))
    )
  }

  private def encryptLifeTimeAllowance(lifeTimeAllowance: LifetimeAllowance)(implicit textAndKey: TextAndKey): EncryptedLifetimeAllowance = {
    EncryptedLifetimeAllowance(
      amount = encryptionService.encrypt(lifeTimeAllowance.amount),
      taxPaid = encryptionService.encrypt(lifeTimeAllowance.taxPaid)
    )
  }

  private def encryptReliefs(reliefs: Reliefs)(implicit textAndKey: TextAndKey): EncryptedReliefs = {
    EncryptedReliefs(
      regularPensionContributions = reliefs.regularPensionContributions.map(encryptionService.encrypt),
      oneOffPensionContributionsPaid = reliefs.oneOffPensionContributionsPaid.map(encryptionService.encrypt),
      retirementAnnuityPayments = reliefs.retirementAnnuityPayments.map(encryptionService.encrypt),
      paymentToEmployersSchemeNoTaxRelief = reliefs.paymentToEmployersSchemeNoTaxRelief.map(encryptionService.encrypt),
      overseasPensionSchemeContributions = reliefs.overseasPensionSchemeContributions.map(encryptionService.encrypt)
    )
  }

  private def decryptReliefs(reliefs: EncryptedReliefs)(implicit textAndKey: TextAndKey): Reliefs = {
    Reliefs(
      regularPensionContributions = reliefs.regularPensionContributions.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
      oneOffPensionContributionsPaid = reliefs.oneOffPensionContributionsPaid.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
      retirementAnnuityPayments = reliefs.retirementAnnuityPayments.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
      paymentToEmployersSchemeNoTaxRelief = reliefs.paymentToEmployersSchemeNoTaxRelief.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
      overseasPensionSchemeContributions = reliefs.overseasPensionSchemeContributions.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce))
    )
  }

  private def decryptPensionSavingsTaxCharges(p: EncryptedPensionSavingsTaxCharges)(implicit textAndKey: TextAndKey): PensionSavingsTaxCharges = {
    PensionSavingsTaxCharges(
      pensionSchemeTaxReference = p.pensionSchemeTaxReference.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
      lumpSumBenefitTakenInExcessOfLifetimeAllowance = p.lumpSumBenefitTakenInExcessOfLifetimeAllowance.map(decryptLifeTimeAllowance),
      benefitInExcessOfLifetimeAllowance = p.benefitInExcessOfLifetimeAllowance.map(decryptLifeTimeAllowance),
      isAnnualAllowanceReduced = encryptionService.decrypt[Boolean](p.isAnnualAllowanceReduced.value, p.isAnnualAllowanceReduced.nonce),
      taperedAnnualAllowance = p.taperedAnnualAllowance.map(x => encryptionService.decrypt[Boolean](x.value, x.nonce)),
      moneyPurchasedAllowance = p.moneyPurchasedAllowance.map(x => encryptionService.decrypt[Boolean](x.value, x.nonce))
    )
  }

  private def decryptLifeTimeAllowance(lifeTimeAllowance: EncryptedLifetimeAllowance)(implicit textAndKey: TextAndKey): LifetimeAllowance = {
    LifetimeAllowance(
      amount = encryptionService.decrypt[BigDecimal](lifeTimeAllowance.amount.value, lifeTimeAllowance.amount.nonce),
      taxPaid = encryptionService.decrypt[BigDecimal](lifeTimeAllowance.taxPaid.value, lifeTimeAllowance.taxPaid.nonce)
    )
  }

  private def decryptPensionSchemeOverseasTransfers(p: EncryptedPensionSchemeOverseasTransfers)
                                                   (implicit textAndKey: TextAndKey): PensionSchemeOverseasTransfers = {
    PensionSchemeOverseasTransfers(
      overseasSchemeProvider = p.overseasSchemeProvider.map(decryptOverseasSchemeProvider),
      transferCharge = encryptionService.decrypt[BigDecimal](p.transferCharge.value, p.transferCharge.nonce),
      transferChargeTaxPaid = encryptionService.decrypt[BigDecimal](p.transferChargeTaxPaid.value, p.transferChargeTaxPaid.nonce)
    )
  }

  private def decryptOverseasSchemeProvider(overseasSchemeProvider: EncryptedOverseasSchemeProvider)
                                           (implicit textAndKey: TextAndKey): OverseasSchemeProvider = {
    OverseasSchemeProvider(
      providerName = encryptionService.decrypt[String](overseasSchemeProvider.providerName.value, overseasSchemeProvider.providerName.nonce),
      providerAddress = encryptionService.decrypt[String](overseasSchemeProvider.providerAddress.value, overseasSchemeProvider.providerAddress.nonce),
      providerCountryCode = encryptionService.decrypt[String](
        overseasSchemeProvider.providerCountryCode.value, overseasSchemeProvider.providerCountryCode.nonce),
      qualifyingRecognisedOverseasPensionScheme = overseasSchemeProvider.qualifyingRecognisedOverseasPensionScheme.map(_.map(
        x => encryptionService.decrypt[String](x.value, x.nonce))),
      pensionSchemeTaxReference = overseasSchemeProvider.pensionSchemeTaxReference.map(_.map(x => encryptionService.decrypt[String](x.value, x.nonce)))
    )
  }

  private def decryptPensionSchemeUnauthorisedPayments(p: EncryptedPensionSchemeUnauthorisedPayments)
                                                      (implicit textAndKey: TextAndKey): PensionSchemeUnauthorisedPayments = {
    PensionSchemeUnauthorisedPayments(
      pensionSchemeTaxReference = p.pensionSchemeTaxReference.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
      surcharge = p.surcharge.map(decryptCharge),
      noSurcharge = p.noSurcharge.map(decryptCharge)
    )
  }

  private def decryptCharge(charge: EncryptedCharge)(implicit textAndKey: TextAndKey): Charge = {
    Charge(
      amount = encryptionService.decrypt[BigDecimal](charge.amount.value, charge.amount.nonce),
      foreignTaxPaid = encryptionService.decrypt[BigDecimal](charge.foreignTaxPaid.value, charge.foreignTaxPaid.nonce)
    )
  }

  private def decryptPensionContributions(p: EncryptedPensionContributions)(implicit textAndKey: TextAndKey): PensionContributions = {
    PensionContributions(
      pensionSchemeTaxReference = p.pensionSchemeTaxReference.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
      inExcessOfTheAnnualAllowance = encryptionService.decrypt[BigDecimal](p.inExcessOfTheAnnualAllowance.value, p.inExcessOfTheAnnualAllowance.nonce),
      annualAllowanceTaxPaid = encryptionService.decrypt[BigDecimal](p.annualAllowanceTaxPaid.value, p.annualAllowanceTaxPaid.nonce)
    )
  }

  private def decryptOverseasPensionContributions(o: EncryptedOverseasPensionContributions)(implicit textAndKey: TextAndKey): OverseasPensionContributions = {
    OverseasPensionContributions(
      overseasSchemeProvider = o.overseasSchemeProvider.map(decryptOverseasSchemeProvider),
      shortServiceRefund = encryptionService.decrypt[BigDecimal](o.shortServiceRefund.value, o.shortServiceRefund.nonce),
      shortServiceRefundTaxPaid = encryptionService.decrypt[BigDecimal](o.shortServiceRefundTaxPaid.value, o.shortServiceRefundTaxPaid.nonce)
    )
  }

  private def decryptStateBenefits(s: EncryptedStateBenefits)(implicit textAndKey: TextAndKey): StateBenefits = {
    StateBenefits(
      incapacityBenefit = s.incapacityBenefit.map(_.map(decryptStateBenefit)),
      statePension = s.statePension.map(decryptStateBenefit),
      statePensionLumpSum = s.statePensionLumpSum.map(decryptStateBenefit),
      employmentSupportAllowance = s.employmentSupportAllowance.map(_.map(decryptStateBenefit)),
      jobSeekersAllowance = s.jobSeekersAllowance.map(_.map(decryptStateBenefit)),
      bereavementAllowance = s.bereavementAllowance.map(decryptStateBenefit),
      otherStateBenefits = s.otherStateBenefits.map(decryptStateBenefit)
    )
  }

  private def decryptStateBenefit(s: EncryptedStateBenefit)(implicit textAndKey: TextAndKey): StateBenefit = {
    StateBenefit(
      benefitId = encryptionService.decrypt[String](s.benefitId.value, s.benefitId.nonce),
      startDate = encryptionService.decrypt[String](s.startDate.value, s.startDate.nonce),
      dateIgnored = s.dateIgnored.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
      submittedOn = s.submittedOn.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
      endDate = s.endDate.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
      amount = s.amount.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
      taxPaid = s.taxPaid.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce))
    )
  }

  private def decryptForeignPension(f: EncryptedForeignPension)(implicit textAndKey: TextAndKey): ForeignPension = {
    ForeignPension(
      countryCode = encryptionService.decrypt[String](f.countryCode.value, f.countryCode.nonce),
      taxableAmount = encryptionService.decrypt[BigDecimal](f.taxableAmount.value, f.taxableAmount.nonce),
      amountBeforeTax = f.amountBeforeTax.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
      taxTakenOff = f.taxTakenOff.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
      specialWithholdingTax = f.specialWithholdingTax.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
      foreignTaxCreditRelief = f.foreignTaxCreditRelief.map(x => encryptionService.decrypt[Boolean](x.value, x.nonce))
    )
  }

  private def decryptOverseasPensionContribution(o: EncryptedOverseasPensionContribution)(implicit textAndKey: TextAndKey): OverseasPensionContribution = {
    OverseasPensionContribution(
      customerReference = o.customerReference.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
      exemptEmployersPensionContribs = encryptionService.decrypt[BigDecimal](o.exemptEmployersPensionContribs.value, o.exemptEmployersPensionContribs.nonce),
      migrantMemReliefQopsRefNo = o.migrantMemReliefQopsRefNo.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
      dblTaxationRelief = o.dblTaxationRelief.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
      dblTaxationCountry = o.dblTaxationCountry.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
      dblTaxationArticle = o.dblTaxationArticle.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
      dblTaxationTreaty = o.dblTaxationTreaty.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
      sf74Reference = o.sf74Reference.map(x => encryptionService.decrypt[String](x.value, x.nonce))
    )
  }

  private def decryptPensions(pensions: EncryptedPensions)(implicit textAndKey: TextAndKey): Pensions = {
    val pensionReliefs: Option[PensionReliefs] = pensions.pensionReliefs.map {
      r =>
        PensionReliefs(
          submittedOn = encryptionService.decrypt[String](r.submittedOn.value, r.submittedOn.nonce),
          deletedOn = r.deletedOn.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
          pensionReliefs = decryptReliefs(r.pensionReliefs)
        )
    }

    val pensionCharges: Option[PensionCharges] = pensions.pensionCharges.map {
      c =>
        PensionCharges(
          submittedOn = encryptionService.decrypt[String](c.submittedOn.value, c.submittedOn.nonce),
          pensionSavingsTaxCharges = c.pensionSavingsTaxCharges.map(decryptPensionSavingsTaxCharges),
          pensionSchemeOverseasTransfers = c.pensionSchemeOverseasTransfers.map(decryptPensionSchemeOverseasTransfers),
          pensionSchemeUnauthorisedPayments = c.pensionSchemeUnauthorisedPayments.map(decryptPensionSchemeUnauthorisedPayments),
          pensionContributions = c.pensionContributions.map(decryptPensionContributions),
          overseasPensionContributions = c.overseasPensionContributions.map(decryptOverseasPensionContributions)
        )
    }

    val stateBenefitsModel: Option[StateBenefitsModel] = pensions.stateBenefits.map {
      s =>
        StateBenefitsModel(
          stateBenefits = s.stateBenefits.map(decryptStateBenefits),
          customerAddedStateBenefits = s.customerAddedStateBenefits.map(decryptStateBenefits)
        )
    }

    val employmentPensions: Option[EmploymentPensions] = pensions.employmentPensions.map {
      e =>
        EmploymentPensions(
          employmentData = e.employmentData.map(decryptEmploymentPensionModel)
        )
    }

    val pensionIncome: Option[PensionIncomeModel] = pensions.pensionIncome.map {
        s =>
          PensionIncomeModel(
            submittedOn = encryptionService.decrypt[String](s.submittedOn.value, s.submittedOn.nonce),
            deletedOn = s.deletedOn.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
            foreignPension = s.foreignPension.map(decryptForeignPension),
            overseasPensionContribution = s.overseasPensionContribution.map(decryptOverseasPensionContribution)
          )
      }

    Pensions(
      pensionReliefs = pensionReliefs,
      pensionCharges = pensionCharges,
      stateBenefits = stateBenefitsModel,
      employmentPensions = employmentPensions,
      pensionIncome = pensionIncome
    )
  }
}
