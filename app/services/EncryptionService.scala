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

import config.AppConfig
import javax.inject.Inject
import models.{DividendsModel, EncryptedDividendsModel, EncryptedInterestModel, InterestModel}
import models.employment.frontend.{AllEmploymentData, EmploymentBenefits, EmploymentData, EmploymentExpenses,
  EmploymentSource, EncryptedAllEmploymentData, EncryptedEmploymentBenefits, EncryptedEmploymentData,
  EncryptedEmploymentExpenses, EncryptedEmploymentSource}
import models.employment.shared.{Benefits, Deductions, EncryptedBenefits, EncryptedDeductions, EncryptedExpenses,
  EncryptedPay, EncryptedStudentLoans, Expenses, Pay, StudentLoans}
import models.giftAid.{EncryptedGiftAidModel, EncryptedGiftAidPaymentsModel, EncryptedGiftsModel, GiftAidModel, GiftAidPaymentsModel, GiftsModel}
import models.mongo.{EncryptedUserData, TextAndKey, UserData}
import utils.SecureGCMCipher

class EncryptionService @Inject()(encryptionService: SecureGCMCipher, appConfig: AppConfig) {

  def encryptUserData(userData: UserData): EncryptedUserData ={
    implicit val textAndKey: TextAndKey = TextAndKey(userData.mtdItId,appConfig.encryptionKey)

    EncryptedUserData(
      sessionId = userData.sessionId,
      mtdItId = userData.mtdItId,
      nino = userData.nino,
      taxYear = userData.taxYear,
      dividends = userData.dividends.map(encryptDividends),
      interest = userData.interest.map(encryptInterest),
      giftAid = userData.giftAid.map(encryptGiftAid),
      employment = userData.employment.map(encryptEmployment),
      lastUpdated = userData.lastUpdated
    )
  }

  private def encryptDividends(dividends: DividendsModel)(implicit textAndKey: TextAndKey): EncryptedDividendsModel ={
    EncryptedDividendsModel(
      ukDividends = dividends.ukDividends.map(encryptionService.encrypt[BigDecimal]),
      otherUkDividends = dividends.otherUkDividends.map(encryptionService.encrypt[BigDecimal])
    )
  }

  private def encryptInterest(interest: Seq[InterestModel])(implicit textAndKey: TextAndKey): Seq[EncryptedInterestModel] ={
    interest.map{
      interest =>
        EncryptedInterestModel(
          accountName = encryptionService.encrypt[String](interest.accountName),
          incomeSourceId = encryptionService.encrypt[String](interest.incomeSourceId),
          taxedUkInterest = interest.taxedUkInterest.map(encryptionService.encrypt[BigDecimal]),
          untaxedUkInterest = interest.untaxedUkInterest.map(encryptionService.encrypt[BigDecimal])
        )
    }
  }

  private def encryptGiftAid(giftAid: GiftAidModel)(implicit textAndKey: TextAndKey): EncryptedGiftAidModel ={

    val eGiftAidPayments: Option[EncryptedGiftAidPaymentsModel] = {
      giftAid.giftAidPayments.map{
        g =>
          EncryptedGiftAidPaymentsModel(
            nonUkCharitiesCharityNames = g.nonUkCharitiesCharityNames.map(_.map(encryptionService.encrypt)),
            currentYear = g.currentYear.map(encryptionService.encrypt),
            oneOffCurrentYear = g.oneOffCurrentYear.map(encryptionService.encrypt),
            currentYearTreatedAsPreviousYear = g.currentYearTreatedAsPreviousYear.map(encryptionService.encrypt),
            nextYearTreatedAsCurrentYear = g.nextYearTreatedAsCurrentYear.map(encryptionService.encrypt),
            nonUkCharities = g.nonUkCharities.map(encryptionService.encrypt)
          )
      }
    }
    val eGifts: Option[EncryptedGiftsModel] = {
      giftAid.gifts.map {
        g =>
          EncryptedGiftsModel(
            investmentsNonUkCharitiesCharityNames = g.investmentsNonUkCharitiesCharityNames.map(_.map(encryptionService.encrypt)),
            landAndBuildings = g.landAndBuildings.map(encryptionService.encrypt),
            sharesOrSecurities = g.sharesOrSecurities.map(encryptionService.encrypt),
            investmentsNonUkCharities = g.investmentsNonUkCharities.map(encryptionService.encrypt)
          )
      }
    }

    EncryptedGiftAidModel(giftAidPayments = eGiftAidPayments, gifts = eGifts)
  }

  private def encryptEmployment(employment: AllEmploymentData)(implicit textAndKey: TextAndKey): EncryptedAllEmploymentData ={

    val hmrcExpenses = employment.hmrcExpenses.map{
      e =>
        EncryptedEmploymentExpenses(
          submittedOn = e.submittedOn.map(encryptionService.encrypt),
          dateIgnored = e.dateIgnored.map(encryptionService.encrypt),
          totalExpenses = e.totalExpenses.map(encryptionService.encrypt),
          expenses = e.expenses.map(encryptExpenses)
        )
    }

    val customerExpenses = employment.customerExpenses.map{
      e =>
        EncryptedEmploymentExpenses(
          submittedOn = e.submittedOn.map(encryptionService.encrypt),
          dateIgnored = e.dateIgnored.map(encryptionService.encrypt),
          totalExpenses = e.totalExpenses.map(encryptionService.encrypt),
          expenses = e.expenses.map(encryptExpenses)
        )
    }

    val hmrcEmploymentData = employment.hmrcEmploymentData.map(encryptEmploymentSource)
    val customerEmploymentData = employment.customerEmploymentData.map(encryptEmploymentSource)

    EncryptedAllEmploymentData(
      hmrcEmploymentData = hmrcEmploymentData,
      hmrcExpenses = hmrcExpenses,
      customerEmploymentData = customerEmploymentData,
      customerExpenses = customerExpenses
    )
  }

  private def encryptEmploymentSource(e: EmploymentSource)(implicit textAndKey: TextAndKey): EncryptedEmploymentSource ={

    EncryptedEmploymentSource(
      employmentId = encryptionService.encrypt(e.employmentId),
      employerName = encryptionService.encrypt(e.employerName),
      employerRef = e.employerRef.map(encryptionService.encrypt),
      payrollId = e.payrollId.map(encryptionService.encrypt),
      startDate = e.startDate.map(encryptionService.encrypt),
      cessationDate = e.cessationDate.map(encryptionService.encrypt),
      dateIgnored = e.dateIgnored.map(encryptionService.encrypt),
      submittedOn = e.submittedOn.map(encryptionService.encrypt),
      employmentData = e.employmentData.map(encryptEmploymentData),
      employmentBenefits = e.employmentBenefits.map(encryptEmploymentBenefits)
    )
  }

  private def decryptEmploymentSource(e: EncryptedEmploymentSource)(implicit textAndKey: TextAndKey): EmploymentSource ={

    EmploymentSource(
      employmentId = encryptionService.decrypt[String](e.employmentId.value,e.employmentId.nonce),
      employerName = encryptionService.decrypt[String](e.employerName.value,e.employerName.nonce),
      employerRef = e.employerRef.map(x => encryptionService.decrypt[String](x.value,x.nonce)),
      payrollId = e.payrollId.map(x => encryptionService.decrypt[String](x.value,x.nonce)),
      startDate = e.startDate.map(x => encryptionService.decrypt[String](x.value,x.nonce)),
      cessationDate = e.cessationDate.map(x => encryptionService.decrypt[String](x.value,x.nonce)),
      dateIgnored = e.dateIgnored.map(x => encryptionService.decrypt[String](x.value,x.nonce)),
      submittedOn = e.submittedOn.map(x => encryptionService.decrypt[String](x.value,x.nonce)),
      employmentData = e.employmentData.map(decryptEmploymentData),
      employmentBenefits = e.employmentBenefits.map(decryptEmploymentBenefits)
    )
  }

  private def encryptEmploymentData(e: EmploymentData)(implicit textAndKey: TextAndKey): EncryptedEmploymentData ={

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

    val deductions = e.deductions.map{
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

  private def decryptEmploymentData(e: EncryptedEmploymentData)(implicit textAndKey: TextAndKey): EmploymentData ={

    val pay = e.pay.map { p =>
      Pay(
        taxablePayToDate = p.taxablePayToDate.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
        totalTaxToDate = p.totalTaxToDate.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
        payFrequency = p.payFrequency.map(x => encryptionService.decrypt[String](x.value,x.nonce)),
        paymentDate = p.paymentDate.map(x => encryptionService.decrypt[String](x.value,x.nonce)),
        taxWeekNo = p.taxWeekNo.map(x => encryptionService.decrypt[Int](x.value,x.nonce)),
        taxMonthNo = p.taxMonthNo.map(x => encryptionService.decrypt[Int](x.value,x.nonce))
      )
    }

    val deductions = e.deductions.map{
      d =>

        val studentLoans = d.studentLoans.map {
          s =>
            StudentLoans(uglDeductionAmount = s.uglDeductionAmount.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
              pglDeductionAmount = s.pglDeductionAmount.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)))
        }

        Deductions(studentLoans)
    }

    EmploymentData(
      submittedOn = encryptionService.decrypt[String](e.submittedOn.value,e.submittedOn.nonce),
      employmentSequenceNumber = e.employmentSequenceNumber.map(x => encryptionService.decrypt[String](x.value,x.nonce)),
      companyDirector = e.companyDirector.map(x => encryptionService.decrypt[Boolean](x.value,x.nonce)),
      closeCompany = e.closeCompany.map(x => encryptionService.decrypt[Boolean](x.value,x.nonce)),
      directorshipCeasedDate = e.directorshipCeasedDate.map(x => encryptionService.decrypt[String](x.value,x.nonce)),
      occPen = e.occPen.map(x => encryptionService.decrypt[Boolean](x.value,x.nonce)),
      disguisedRemuneration = e.disguisedRemuneration.map(x => encryptionService.decrypt[Boolean](x.value,x.nonce)),
      pay = pay,
      deductions = deductions
    )
  }

  private def encryptEmploymentBenefits(e: EmploymentBenefits)(implicit textAndKey: TextAndKey): EncryptedEmploymentBenefits ={

    val benefits = e.benefits.map{
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

  private def decryptEmploymentBenefits(e: EncryptedEmploymentBenefits)(implicit textAndKey: TextAndKey): EmploymentBenefits ={

    val benefits = e.benefits.map{
      b =>
        Benefits(
          accommodation = b.accommodation.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
          assets = b.assets.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
          assetTransfer = b.assetTransfer.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
          beneficialLoan = b.beneficialLoan.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
          car = b.car.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
          carFuel = b.carFuel.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
          educationalServices = b.educationalServices.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
          entertaining = b.entertaining.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
          expenses = b.expenses.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
          medicalInsurance = b.medicalInsurance.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
          telephone = b.telephone.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
          service = b.service.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
          taxableExpenses = b.taxableExpenses.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
          van = b.van.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
          vanFuel = b.vanFuel.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
          mileage = b.mileage.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
          nonQualifyingRelocationExpenses = b.nonQualifyingRelocationExpenses.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
          nurseryPlaces = b.nurseryPlaces.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
          otherItems = b.otherItems.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
          paymentsOnEmployeesBehalf = b.paymentsOnEmployeesBehalf.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
          personalIncidentalExpenses = b.personalIncidentalExpenses.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
          qualifyingRelocationExpenses = b.qualifyingRelocationExpenses.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
          employerProvidedProfessionalSubscriptions = b.employerProvidedProfessionalSubscriptions.map(
            x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
          employerProvidedServices = b.employerProvidedServices.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
          incomeTaxPaidByDirector = b.incomeTaxPaidByDirector.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
          travelAndSubsistence = b.travelAndSubsistence.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
          vouchersAndCreditCards = b.vouchersAndCreditCards.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
          nonCash = b.nonCash.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce))
        )
    }

    EmploymentBenefits(submittedOn = encryptionService.decrypt[String](e.submittedOn.value,e.submittedOn.nonce), benefits = benefits)
  }

  private def encryptExpenses(e: Expenses)(implicit textAndKey: TextAndKey): EncryptedExpenses ={
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

  private def decryptExpenses(e: EncryptedExpenses)(implicit textAndKey: TextAndKey): Expenses ={
    Expenses(
      businessTravelCosts = e.businessTravelCosts.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
      jobExpenses = e.jobExpenses.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
      flatRateJobExpenses = e.flatRateJobExpenses.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
      professionalSubscriptions = e.professionalSubscriptions.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
      hotelAndMealExpenses = e.hotelAndMealExpenses.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
      otherAndCapitalAllowances = e.otherAndCapitalAllowances.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
      vehicleExpenses = e.vehicleExpenses.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
      mileageAllowanceRelief = e.mileageAllowanceRelief.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce))
    )
  }

  def decryptUserData(userData: EncryptedUserData): UserData ={
    implicit val textAndKey: TextAndKey = TextAndKey(userData.mtdItId,appConfig.encryptionKey)

    UserData(
      sessionId = userData.sessionId,
      mtdItId = userData.mtdItId,
      nino = userData.nino,
      taxYear = userData.taxYear,
      dividends = userData.dividends.map(decryptDividends),
      interest = userData.interest.map(decryptInterest),
      giftAid = userData.giftAid.map(decryptGiftAid),
      employment = userData.employment.map(decryptEmployment),
      lastUpdated = userData.lastUpdated
    )
  }

  private def decryptDividends(dividends: EncryptedDividendsModel)(implicit textAndKey: TextAndKey): DividendsModel ={
    DividendsModel(
      dividends.ukDividends.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
      dividends.otherUkDividends.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce))
    )
  }

  private def decryptInterest(interest: Seq[EncryptedInterestModel])(implicit textAndKey: TextAndKey): Seq[InterestModel] ={
    interest.map{
      interest =>
        InterestModel(
          accountName = encryptionService.decrypt[String](interest.accountName.value,interest.accountName.nonce),
          incomeSourceId = encryptionService.decrypt[String](interest.incomeSourceId.value,interest.incomeSourceId.nonce),
          taxedUkInterest = interest.taxedUkInterest.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
          untaxedUkInterest = interest.untaxedUkInterest.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce))
        )
    }
  }

  private def decryptGiftAid(giftAid: EncryptedGiftAidModel)(implicit textAndKey: TextAndKey): GiftAidModel ={

    val dGiftAidPayments: Option[GiftAidPaymentsModel] = {
      giftAid.giftAidPayments.map{
        g =>
          GiftAidPaymentsModel(
            nonUkCharitiesCharityNames = g.nonUkCharitiesCharityNames.map(_.map(x => encryptionService.decrypt[String](x.value,x.nonce))),
            currentYear = g.currentYear.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
            oneOffCurrentYear = g.oneOffCurrentYear.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
            currentYearTreatedAsPreviousYear = g.currentYearTreatedAsPreviousYear.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
            nextYearTreatedAsCurrentYear = g.nextYearTreatedAsCurrentYear.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
            nonUkCharities = g.nonUkCharities.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce))
          )
      }
    }

    val dGifts: Option[GiftsModel] = {
      giftAid.gifts.map {
        g =>
          GiftsModel(
            investmentsNonUkCharitiesCharityNames = g.investmentsNonUkCharitiesCharityNames.map(_.map(x => encryptionService.decrypt[String](x.value,x.nonce))),
            landAndBuildings = g.landAndBuildings.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
            sharesOrSecurities = g.sharesOrSecurities.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
            investmentsNonUkCharities = g.investmentsNonUkCharities.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce))
          )
      }
    }

    GiftAidModel(giftAidPayments = dGiftAidPayments, gifts = dGifts)
  }

  private def decryptEmployment(employment: EncryptedAllEmploymentData)(implicit textAndKey: TextAndKey): AllEmploymentData={
    val hmrcExpenses = employment.hmrcExpenses.map{
      e =>
        EmploymentExpenses(
          submittedOn = e.submittedOn.map(x => encryptionService.decrypt[String](x.value,x.nonce)),
          dateIgnored = e.dateIgnored.map(x => encryptionService.decrypt[String](x.value,x.nonce)),
          totalExpenses = e.totalExpenses.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
          expenses = e.expenses.map(decryptExpenses)
        )
    }

    val customerExpenses = employment.customerExpenses.map{
      e =>
        EmploymentExpenses(
          submittedOn = e.submittedOn.map(x => encryptionService.decrypt[String](x.value,x.nonce)),
          dateIgnored = e.dateIgnored.map(x => encryptionService.decrypt[String](x.value,x.nonce)),
          totalExpenses = e.totalExpenses.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
          expenses = e.expenses.map(decryptExpenses)
        )
    }

    val hmrcEmploymentData = employment.hmrcEmploymentData.map(decryptEmploymentSource)
    val customerEmploymentData = employment.customerEmploymentData.map(decryptEmploymentSource)

    AllEmploymentData(
      hmrcEmploymentData = hmrcEmploymentData,
      hmrcExpenses = hmrcExpenses,
      customerEmploymentData = customerEmploymentData,
      customerExpenses = customerExpenses
    )
  }
}
