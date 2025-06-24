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

import models._
import models.employment._
import models.gains.{EncryptedInsurancePoliciesModel, EncryptedLifeAnnuityModel, InsurancePoliciesModel, LifeAnnuityModel}
import models.gifts._
import models.mongo.{EncryptedUserData, UserData}
import models.otheremployment.EncryptedOtherEmploymentIncome
import models.pensions._
import models.pensions.charges._
import models.pensions.employmentPensions.{EmploymentPensionModel, EmploymentPensions, EncryptedEmploymentPensionModel, EncryptedEmploymentPensions}
import models.pensions.income._
import models.pensions.reliefs.{EncryptedPensionReliefs, EncryptedReliefs, PensionReliefs, Reliefs}
import models.statebenefits.{AllStateBenefitsData, EncryptedAllStateBenefitsData}
import utils.AesGcmAdCrypto
import utils.CypherSyntax.{DecryptableOps, EncryptableOps}

import javax.inject.Inject

//scalastyle:off
class EncryptionService @Inject()(implicit val aesGcmAdCrypto: AesGcmAdCrypto) {

  def encryptUserData(userData: UserData): EncryptedUserData = {
    implicit val associatedText: String = userData.mtdItId

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
      cis = userData.cis.map(_.encrypted()),
      stateBenefits = userData.stateBenefits.map(_.encrypted()),
      interestSavings = userData.interestSavings.map(encryptSavingsIncome),
      gains = userData.gains.map(encryptGains),
      stockDividends = userData.stockDividends.map(encryptStockDividends),
      lastUpdated = userData.lastUpdated
    )
  }

  private def encryptDividends(dividends: Dividends)(implicit associatedText: String): EncryptedDividends = {
    EncryptedDividends(
      ukDividends = dividends.ukDividends.map(_.encrypted),
      otherUkDividends = dividends.otherUkDividends.map(_.encrypted)
    )
  }

  private def encryptStockDividends(stockDividends: StockDividends)(implicit associatedText: String): EncryptedStockDividends = {
    def getEncryptedForeignInterestModel(modelData: Option[Seq[ForeignInterestModel]]): Option[Seq[EncryptedForeignInterestModel]] = {
      modelData.map {
        g =>
          g.map(foreignInterestsModel =>
            EncryptedForeignInterestModel(
              countryCode = foreignInterestsModel.countryCode.encrypted,
              amountBeforeTax = foreignInterestsModel.amountBeforeTax.map(_.encrypted),
              taxTakenOff = foreignInterestsModel.taxTakenOff.map(_.encrypted),
              specialWithholdingTax = foreignInterestsModel.specialWithholdingTax.map(_.encrypted),
              foreignTaxCreditRelief = foreignInterestsModel.foreignTaxCreditRelief.map(_.encrypted),
              taxableAmount = foreignInterestsModel.taxableAmount.encrypted
            )
          )
      }
    }

    def getEncryptedDividend(modelData:Option[Dividend]): Option[EncryptedDividend] = {
      modelData.map {
        g =>
          EncryptedDividend(
            customerReference = g.customerReference.map(_.encrypted),
            grossAmount = g.grossAmount.map(_.encrypted)
          )
      }
    }
    EncryptedStockDividends(
      submittedOn = stockDividends.submittedOn.map(_.encrypted),
      foreignDividends = getEncryptedForeignInterestModel(stockDividends.foreignDividend),
      dividendIncomeReceivedWhilstAbroad = getEncryptedForeignInterestModel(stockDividends.dividendIncomeReceivedWhilstAbroad),
      stockDividends = getEncryptedDividend(stockDividends.stockDividend),
      redeemableShares = getEncryptedDividend(stockDividends.redeemableShares),
      bonusIssuesOfSecurities = getEncryptedDividend(stockDividends.bonusIssuesOfSecurities),
      closeCompanyLoansWrittenOff = getEncryptedDividend(stockDividends.closeCompanyLoansWrittenOff)
    )
  }
  private def encryptInterest(interest: Seq[Interest])(implicit associatedText: String): Seq[EncryptedInterest] = {
    interest.map {
      interest =>
        EncryptedInterest(
          accountName = interest.accountName.encrypted,
          incomeSourceId = interest.incomeSourceId.encrypted,
          taxedUkInterest = interest.taxedUkInterest.map(_.encrypted),
          untaxedUkInterest = interest.untaxedUkInterest.map(_.encrypted)
        )
    }
  }

  private def encryptGiftAid(giftAid: GiftAid)(implicit associatedText: String): EncryptedGiftAid = {

    val eGiftAidPayments: Option[EncryptedGiftAidPayments] = {
      giftAid.giftAidPayments.map {
        g =>
          EncryptedGiftAidPayments(
            nonUkCharitiesCharityNames = g.nonUkCharitiesCharityNames.map(_.map(_.encrypted)),
            currentYear = g.currentYear.map(_.encrypted),
            oneOffCurrentYear = g.oneOffCurrentYear.map(_.encrypted),
            currentYearTreatedAsPreviousYear = g.currentYearTreatedAsPreviousYear.map(_.encrypted),
            nextYearTreatedAsCurrentYear = g.nextYearTreatedAsCurrentYear.map(_.encrypted),
            nonUkCharities = g.nonUkCharities.map(_.encrypted)
          )
      }
    }
    val eGifts: Option[EncryptedGifts] = {
      giftAid.gifts.map {
        g =>
          EncryptedGifts(
            investmentsNonUkCharitiesCharityNames = g.investmentsNonUkCharitiesCharityNames.map(_.map(_.encrypted)),
            landAndBuildings = g.landAndBuildings.map(_.encrypted),
            sharesOrSecurities = g.sharesOrSecurities.map(_.encrypted),
            investmentsNonUkCharities = g.investmentsNonUkCharities.map(_.encrypted)
          )
      }
    }

    EncryptedGiftAid(giftAidPayments = eGiftAidPayments, gifts = eGifts)
  }
  private def encryptSavingsIncome(savingsIncome: SavingsIncomeDataModel)(implicit associatedText: String): EncryptedSavingsIncomeDataModel = {

    val eSecurities: Option[EncryptedSecuritiesModel] = {
      savingsIncome.securities.map {
        g =>
          EncryptedSecuritiesModel(
            taxTakenOff = g.taxTakenOff.map(_.encrypted),
            grossAmount = g.grossAmount.encrypted,
            netAmount = g.netAmount.map(_.encrypted)
          )
      }
    }
    val eForeignSavings: Option[Seq[EncryptedForeignInterestModel]] = {
      savingsIncome.foreignInterest.map {
        g =>
          g.map(foreignInterests =>
            EncryptedForeignInterestModel(
              countryCode = foreignInterests.countryCode.encrypted,
              amountBeforeTax = foreignInterests.amountBeforeTax.map(_.encrypted),
              taxTakenOff = foreignInterests.taxTakenOff.map(_.encrypted),
              specialWithholdingTax = foreignInterests.specialWithholdingTax.map(_.encrypted),
              foreignTaxCreditRelief = foreignInterests.foreignTaxCreditRelief.map(_.encrypted),
              taxableAmount = foreignInterests.taxableAmount.encrypted
            )
          )
      }
    }

    EncryptedSavingsIncomeDataModel(
      submittedOn = savingsIncome.submittedOn.map(_.encrypted),
      securities = eSecurities,
      foreignInterest = eForeignSavings
    )
  }
  private def decryptSavingsIncome(savingsIncome: EncryptedSavingsIncomeDataModel)(implicit associatedText: String): SavingsIncomeDataModel = {

    val eSecurities: Option[SecuritiesModel] = {
      savingsIncome.securities.map {
        g =>
          SecuritiesModel(
            taxTakenOff = g.taxTakenOff.map(_.decrypted[BigDecimal]),
            grossAmount = g.grossAmount.decrypted[BigDecimal],
            netAmount = g.netAmount.map(_.decrypted[BigDecimal])
          )
      }
    }
    val eForeignSavings: Option[Seq[ForeignInterestModel]] = {
      savingsIncome.foreignInterest.map {
        g =>
          g.map(foreignInterests =>
            ForeignInterestModel(
              countryCode = foreignInterests.countryCode.decrypted[String],
              amountBeforeTax = foreignInterests.amountBeforeTax.map(_.decrypted[BigDecimal]),
              taxTakenOff = foreignInterests.taxTakenOff.map(_.decrypted[BigDecimal]),
              specialWithholdingTax = foreignInterests.specialWithholdingTax.map(_.decrypted[BigDecimal]),
              foreignTaxCreditRelief = foreignInterests.foreignTaxCreditRelief.map(_.decrypted[Boolean]),
              taxableAmount = foreignInterests.taxableAmount.decrypted[BigDecimal]
            )
          )
      }
    }

    SavingsIncomeDataModel(
      submittedOn = savingsIncome.submittedOn.map(_.decrypted[String]),
      securities = eSecurities,
      foreignInterest = eForeignSavings
    )
  }

  private def encryptEmployment(employment: AllEmploymentData)(implicit associatedText: String): EncryptedAllEmploymentData = {

    val hmrcExpenses = employment.hmrcExpenses.map {
      e =>
        EncryptedEmploymentExpenses(
          submittedOn = e.submittedOn.map(_.encrypted),
          dateIgnored = e.dateIgnored.map(_.encrypted),
          totalExpenses = e.totalExpenses.map(_.encrypted),
          expenses = e.expenses.map(encryptExpenses)
        )
    }

    val customerExpenses = employment.customerExpenses.map {
      e =>
        EncryptedEmploymentExpenses(
          submittedOn = e.submittedOn.map(_.encrypted),
          dateIgnored = e.dateIgnored.map(_.encrypted),
          totalExpenses = e.totalExpenses.map(_.encrypted),
          expenses = e.expenses.map(encryptExpenses)
        )
    }

    val hmrcEmploymentData = employment.hmrcEmploymentData.map(encryptHmrcEmploymentSource)
    val customerEmploymentData: Seq[EncryptedEmploymentSource] = employment.customerEmploymentData.map(encryptEmploymentSource)

    val otherEmploymentIncome: Option[EncryptedOtherEmploymentIncome] = employment.otherEmploymentIncome.map(_.encrypted())

    EncryptedAllEmploymentData(
      hmrcEmploymentData = hmrcEmploymentData,
      hmrcExpenses = hmrcExpenses,
      customerEmploymentData = customerEmploymentData,
      customerExpenses = customerExpenses,
      otherEmploymentIncome = otherEmploymentIncome
    )
  }

  private def encryptHmrcEmploymentSource(e: HmrcEmploymentSource)
                                         (implicit associatedText: String): EncryptedHmrcEmploymentSource = EncryptedHmrcEmploymentSource(
    employmentId = e.employmentId.encrypted,
    employerName = e.employerName.encrypted,
    employerRef = e.employerRef.map(_.encrypted),
    payrollId = e.payrollId.map(_.encrypted),
    startDate = e.startDate.map(_.encrypted),
    cessationDate = e.cessationDate.map(_.encrypted),
    dateIgnored = e.dateIgnored.map(_.encrypted),
    submittedOn = e.submittedOn.map(_.encrypted),
    hmrcEmploymentFinancialData = e.hmrcEmploymentFinancialData.map(encryptEmploymentFinancialData),
    customerEmploymentFinancialData = e.customerEmploymentFinancialData.map(encryptEmploymentFinancialData),
    occupationalPension = e.occupationalPension.map(_.encrypted)
  )

  private def encryptEmploymentFinancialData(e: EmploymentFinancialData)(implicit associatedText: String): EncryptedEmploymentFinancialData = {

    EncryptedEmploymentFinancialData(
      employmentData = e.employmentData.map(encryptEmploymentData),
      employmentBenefits = e.employmentBenefits.map(encryptEmploymentBenefits)
    )
  }

  private def encryptEmploymentSource(e: EmploymentSource)
                                     (implicit associatedText: String): EncryptedEmploymentSource = EncryptedEmploymentSource(
    employmentId = e.employmentId.encrypted,
    employerName = e.employerName.encrypted,
    employerRef = e.employerRef.map(_.encrypted),
    payrollId = e.payrollId.map(_.encrypted),
    startDate = e.startDate.map(_.encrypted),
    cessationDate = e.cessationDate.map(_.encrypted),
    dateIgnored = e.dateIgnored.map(_.encrypted),
    submittedOn = e.submittedOn.map(_.encrypted),
    employmentData = e.employmentData.map(encryptEmploymentData),
    employmentBenefits = e.employmentBenefits.map(encryptEmploymentBenefits),
    occupationalPension = e.occupationalPension.map(_.encrypted)
  )

  private def decryptEmploymentSource(e: EncryptedEmploymentSource)
                                     (implicit associatedText: String): EmploymentSource = EmploymentSource(
    employmentId = e.employmentId.decrypted[String],
    employerName = e.employerName.decrypted[String],
    employerRef = e.employerRef.map(_.decrypted[String]),
    payrollId = e.payrollId.map(_.decrypted[String]),
    startDate = e.startDate.map(_.decrypted[String]),
    cessationDate = e.cessationDate.map(_.decrypted[String]),
    dateIgnored = e.dateIgnored.map(_.decrypted[String]),
    submittedOn = e.submittedOn.map(_.decrypted[String]),
    employmentData = e.employmentData.map(decryptEmploymentData),
    employmentBenefits = e.employmentBenefits.map(decryptEmploymentBenefits),
    occupationalPension = e.occupationalPension.map(_.decrypted[Boolean])
  )

  private def decryptHmrcEmploymentSource(e: EncryptedHmrcEmploymentSource)
                                         (implicit associatedText: String): HmrcEmploymentSource = HmrcEmploymentSource(
    employmentId = e.employmentId.decrypted[String],
    employerName = e.employerName.decrypted[String],
    employerRef = e.employerRef.map(_.decrypted[String]),
    payrollId = e.payrollId.map(_.decrypted[String]),
    startDate = e.startDate.map(_.decrypted[String]),
    cessationDate = e.cessationDate.map(_.decrypted[String]),
    dateIgnored = e.dateIgnored.map(_.decrypted[String]),
    submittedOn = e.submittedOn.map(_.decrypted[String]),
    hmrcEmploymentFinancialData = e.hmrcEmploymentFinancialData.map(decryptEmploymentFinancialData),
    customerEmploymentFinancialData = e.customerEmploymentFinancialData.map(decryptEmploymentFinancialData),
    occupationalPension = e.occupationalPension.map(_.decrypted[Boolean])
  )

  private def decryptEmploymentFinancialData(e: EncryptedEmploymentFinancialData)(implicit associatedText: String): EmploymentFinancialData = {

    EmploymentFinancialData(
      employmentData = e.employmentData.map(decryptEmploymentData),
      employmentBenefits = e.employmentBenefits.map(decryptEmploymentBenefits)
    )
  }

  private def encryptEmploymentData(e: EmploymentData)(implicit associatedText: String): EncryptedEmploymentData = {

    val pay = e.pay.map { p =>
      EncryptedPay(
        taxablePayToDate = p.taxablePayToDate.map(_.encrypted),
        totalTaxToDate = p.totalTaxToDate.map(_.encrypted),
        payFrequency = p.payFrequency.map(_.encrypted),
        paymentDate = p.paymentDate.map(_.encrypted),
        taxWeekNo = p.taxWeekNo.map(_.toString.encrypted),
        taxMonthNo = p.taxMonthNo.map(_.toString.encrypted)
      )
    }

    val deductions = e.deductions.map {
      d =>

        val studentLoans = d.studentLoans.map {
          s =>
            EncryptedStudentLoans(
              uglDeductionAmount = s.uglDeductionAmount.map(_.encrypted),
              pglDeductionAmount = s.pglDeductionAmount.map(_.encrypted))
        }

        EncryptedDeductions(studentLoans)
    }

    EncryptedEmploymentData(
      submittedOn = e.submittedOn.encrypted,
      employmentSequenceNumber = e.employmentSequenceNumber.map(_.encrypted),
      companyDirector = e.companyDirector.map(_.encrypted),
      closeCompany = e.closeCompany.map(_.encrypted),
      directorshipCeasedDate = e.directorshipCeasedDate.map(_.encrypted),
      occPen = e.occPen.map(_.encrypted),
      disguisedRemuneration = e.disguisedRemuneration.map(_.encrypted),
      offPayrollWorker = e.offPayrollWorker.map(_.encrypted),
      pay = pay,
      deductions = deductions
    )
  }

  private def decryptEmploymentData(e: EncryptedEmploymentData)(implicit associatedText: String): EmploymentData = {

    val pay = e.pay.map { p =>
      Pay(
        taxablePayToDate = p.taxablePayToDate.map(_.decrypted[BigDecimal]),
        totalTaxToDate = p.totalTaxToDate.map(_.decrypted[BigDecimal]),
        payFrequency = p.payFrequency.map(_.decrypted[String]),
        paymentDate = p.paymentDate.map(_.decrypted[String]),
        taxWeekNo = p.taxWeekNo.map(_.decrypted[Int]),
        taxMonthNo = p.taxMonthNo.map(_.decrypted[Int])
      )
    }

    val deductions = e.deductions.map {
      d =>

        val studentLoans = d.studentLoans.map {
          s =>
            StudentLoans(uglDeductionAmount = s.uglDeductionAmount.map(_.decrypted[BigDecimal]),
              pglDeductionAmount = s.pglDeductionAmount.map(_.decrypted[BigDecimal]))
        }

        Deductions(studentLoans)
    }

    EmploymentData(
      submittedOn = e.submittedOn.decrypted[String],
      employmentSequenceNumber = e.employmentSequenceNumber.map(_.decrypted[String]),
      companyDirector = e.companyDirector.map(_.decrypted[Boolean]),
      closeCompany = e.closeCompany.map(_.decrypted[Boolean]),
      directorshipCeasedDate = e.directorshipCeasedDate.map(_.decrypted[String]),
      occPen = e.occPen.map(_.decrypted[Boolean]),
      disguisedRemuneration = e.disguisedRemuneration.map(_.decrypted[Boolean]),
      offPayrollWorker = e.offPayrollWorker.map(_.decrypted[Boolean]),
      pay = pay,
      deductions = deductions
    )
  }

  private def encryptEmploymentBenefits(e: EmploymentBenefits)(implicit associatedText: String): EncryptedEmploymentBenefits = {

    val benefits = e.benefits.map {
      b =>
        EncryptedBenefits(
          accommodation = b.accommodation.map(_.encrypted),
          assets = b.assets.map(_.encrypted),
          assetTransfer = b.assetTransfer.map(_.encrypted),
          beneficialLoan = b.beneficialLoan.map(_.encrypted),
          car = b.car.map(_.encrypted),
          carFuel = b.carFuel.map(_.encrypted),
          educationalServices = b.educationalServices.map(_.encrypted),
          entertaining = b.entertaining.map(_.encrypted),
          expenses = b.expenses.map(_.encrypted),
          medicalInsurance = b.medicalInsurance.map(_.encrypted),
          telephone = b.telephone.map(_.encrypted),
          service = b.service.map(_.encrypted),
          taxableExpenses = b.taxableExpenses.map(_.encrypted),
          van = b.van.map(_.encrypted),
          vanFuel = b.vanFuel.map(_.encrypted),
          mileage = b.mileage.map(_.encrypted),
          nonQualifyingRelocationExpenses = b.nonQualifyingRelocationExpenses.map(_.encrypted),
          nurseryPlaces = b.nurseryPlaces.map(_.encrypted),
          otherItems = b.otherItems.map(_.encrypted),
          paymentsOnEmployeesBehalf = b.paymentsOnEmployeesBehalf.map(_.encrypted),
          personalIncidentalExpenses = b.personalIncidentalExpenses.map(_.encrypted),
          qualifyingRelocationExpenses = b.qualifyingRelocationExpenses.map(_.encrypted),
          employerProvidedProfessionalSubscriptions = b.employerProvidedProfessionalSubscriptions.map(_.encrypted),
          employerProvidedServices = b.employerProvidedServices.map(_.encrypted),
          incomeTaxPaidByDirector = b.incomeTaxPaidByDirector.map(_.encrypted),
          travelAndSubsistence = b.travelAndSubsistence.map(_.encrypted),
          vouchersAndCreditCards = b.vouchersAndCreditCards.map(_.encrypted),
          nonCash = b.nonCash.map(_.encrypted)
        )
    }

    EncryptedEmploymentBenefits(submittedOn = e.submittedOn.encrypted, benefits = benefits)
  }

  private def decryptEmploymentBenefits(e: EncryptedEmploymentBenefits)(implicit associatedText: String): EmploymentBenefits = {

    val benefits = e.benefits.map {
      b =>
        Benefits(
          accommodation = b.accommodation.map(_.decrypted[BigDecimal]),
          assets = b.assets.map(_.decrypted[BigDecimal]),
          assetTransfer = b.assetTransfer.map(_.decrypted[BigDecimal]),
          beneficialLoan = b.beneficialLoan.map(_.decrypted[BigDecimal]),
          car = b.car.map(_.decrypted[BigDecimal]),
          carFuel = b.carFuel.map(_.decrypted[BigDecimal]),
          educationalServices = b.educationalServices.map(_.decrypted[BigDecimal]),
          entertaining = b.entertaining.map(_.decrypted[BigDecimal]),
          expenses = b.expenses.map(_.decrypted[BigDecimal]),
          medicalInsurance = b.medicalInsurance.map(_.decrypted[BigDecimal]),
          telephone = b.telephone.map(_.decrypted[BigDecimal]),
          service = b.service.map(_.decrypted[BigDecimal]),
          taxableExpenses = b.taxableExpenses.map(_.decrypted[BigDecimal]),
          van = b.van.map(_.decrypted[BigDecimal]),
          vanFuel = b.vanFuel.map(_.decrypted[BigDecimal]),
          mileage = b.mileage.map(_.decrypted[BigDecimal]),
          nonQualifyingRelocationExpenses = b.nonQualifyingRelocationExpenses.map(_.decrypted[BigDecimal]),
          nurseryPlaces = b.nurseryPlaces.map(_.decrypted[BigDecimal]),
          otherItems = b.otherItems.map(_.decrypted[BigDecimal]),
          paymentsOnEmployeesBehalf = b.paymentsOnEmployeesBehalf.map(_.decrypted[BigDecimal]),
          personalIncidentalExpenses = b.personalIncidentalExpenses.map(_.decrypted[BigDecimal]),
          qualifyingRelocationExpenses = b.qualifyingRelocationExpenses.map(_.decrypted[BigDecimal]),
          employerProvidedProfessionalSubscriptions = b.employerProvidedProfessionalSubscriptions.map(
            _.decrypted[BigDecimal]),
          employerProvidedServices = b.employerProvidedServices.map(_.decrypted[BigDecimal]),
          incomeTaxPaidByDirector = b.incomeTaxPaidByDirector.map(_.decrypted[BigDecimal]),
          travelAndSubsistence = b.travelAndSubsistence.map(_.decrypted[BigDecimal]),
          vouchersAndCreditCards = b.vouchersAndCreditCards.map(_.decrypted[BigDecimal]),
          nonCash = b.nonCash.map(_.decrypted[BigDecimal])
        )
    }

    EmploymentBenefits(submittedOn = e.submittedOn.decrypted[String], benefits = benefits)
  }

  private def encryptExpenses(e: Expenses)(implicit associatedText: String): EncryptedExpenses = {
    EncryptedExpenses(
      businessTravelCosts = e.businessTravelCosts.map(_.encrypted),
      jobExpenses = e.jobExpenses.map(_.encrypted),
      flatRateJobExpenses = e.flatRateJobExpenses.map(_.encrypted),
      professionalSubscriptions = e.professionalSubscriptions.map(_.encrypted),
      hotelAndMealExpenses = e.hotelAndMealExpenses.map(_.encrypted),
      otherAndCapitalAllowances = e.otherAndCapitalAllowances.map(_.encrypted),
      vehicleExpenses = e.vehicleExpenses.map(_.encrypted),
      mileageAllowanceRelief = e.mileageAllowanceRelief.map(_.encrypted)
    )
  }

  private def decryptExpenses(e: EncryptedExpenses)(implicit associatedText: String): Expenses = {
    Expenses(
      businessTravelCosts = e.businessTravelCosts.map(_.decrypted[BigDecimal]),
      jobExpenses = e.jobExpenses.map(_.decrypted[BigDecimal]),
      flatRateJobExpenses = e.flatRateJobExpenses.map(_.decrypted[BigDecimal]),
      professionalSubscriptions = e.professionalSubscriptions.map(_.decrypted[BigDecimal]),
      hotelAndMealExpenses = e.hotelAndMealExpenses.map(_.decrypted[BigDecimal]),
      otherAndCapitalAllowances = e.otherAndCapitalAllowances.map(_.decrypted[BigDecimal]),
      vehicleExpenses = e.vehicleExpenses.map(_.decrypted[BigDecimal]),
      mileageAllowanceRelief = e.mileageAllowanceRelief.map(_.decrypted[BigDecimal])
    )
  }

  def decryptUserData(userData: EncryptedUserData): UserData = {
    implicit val associatedText: String = userData.mtdItId

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
      cis = userData.cis.map(_.decrypted()),
      stateBenefits = userData.stateBenefits.map(_.decrypted()),
      interestSavings = userData.interestSavings.map(decryptSavingsIncome),
      gains = userData.gains.map(decryptGains),
      stockDividends = userData.stockDividends.map(decryptStockDividends),
      lastUpdated = userData.lastUpdated
    )
  }

  private def decryptDividends(dividends: EncryptedDividends)(implicit associatedText: String): Dividends = {
    Dividends(
      dividends.ukDividends.map(_.decrypted[BigDecimal]),
      dividends.otherUkDividends.map(_.decrypted[BigDecimal])
    )
  }

  private def decryptStockDividends(estockDividends: EncryptedStockDividends)(implicit associatedText: String): StockDividends = {
    def getForeignInterestModel(modelData: Option[Seq[EncryptedForeignInterestModel]]): Option[Seq[ForeignInterestModel]] = {
      modelData.map {
        g =>
          g.map(eforeignInterestsModel =>
            ForeignInterestModel(
              countryCode = eforeignInterestsModel.countryCode.decrypted[String],
              amountBeforeTax = eforeignInterestsModel.amountBeforeTax.map(_.decrypted[BigDecimal]),
              taxTakenOff = eforeignInterestsModel.taxTakenOff.map(_.decrypted[BigDecimal]),
              specialWithholdingTax = eforeignInterestsModel.specialWithholdingTax.map(_.decrypted[BigDecimal]),
              foreignTaxCreditRelief = eforeignInterestsModel.foreignTaxCreditRelief.map(_.decrypted[Boolean]),
              taxableAmount = eforeignInterestsModel.taxableAmount.decrypted[BigDecimal]
            )
          )
      }
    }

    def getDividend(modelData: Option[EncryptedDividend]): Option[Dividend] = {
      modelData.map {
        g =>
          Dividend(
            customerReference = g.customerReference.map(_.decrypted[String]),
            grossAmount = g.grossAmount.map(_.decrypted[BigDecimal])
          )
      }
    }

    StockDividends(
      submittedOn = estockDividends.submittedOn.map(_.decrypted[String]),
      foreignDividend = getForeignInterestModel(estockDividends.foreignDividends),
      dividendIncomeReceivedWhilstAbroad = getForeignInterestModel(estockDividends.dividendIncomeReceivedWhilstAbroad),
      stockDividend = getDividend(estockDividends.stockDividends),
      redeemableShares = getDividend(estockDividends.redeemableShares),
      bonusIssuesOfSecurities = getDividend(estockDividends.bonusIssuesOfSecurities),
      closeCompanyLoansWrittenOff = getDividend(estockDividends.closeCompanyLoansWrittenOff)
    )
  }

  private def decryptInterest(interest: Seq[EncryptedInterest])(implicit associatedText: String): Seq[Interest] = {
    interest.map {
      interest =>
        Interest(
          accountName = interest.accountName.decrypted[String],
          incomeSourceId = interest.incomeSourceId.decrypted[String],
          taxedUkInterest = interest.taxedUkInterest.map(_.decrypted[BigDecimal]),
          untaxedUkInterest = interest.untaxedUkInterest.map(_.decrypted[BigDecimal])
        )
    }
  }

  private def decryptGiftAid(giftAid: EncryptedGiftAid)(implicit associatedText: String): GiftAid = {

    val dGiftAidPayments: Option[GiftAidPayments] = {
      giftAid.giftAidPayments.map {
        g =>
          GiftAidPayments(
            nonUkCharitiesCharityNames = g.nonUkCharitiesCharityNames.map(_.map(_.decrypted[String])),
            currentYear = g.currentYear.map(_.decrypted[BigDecimal]),
            oneOffCurrentYear = g.oneOffCurrentYear.map(_.decrypted[BigDecimal]),
            currentYearTreatedAsPreviousYear = g.currentYearTreatedAsPreviousYear.map(_.decrypted[BigDecimal]),
            nextYearTreatedAsCurrentYear = g.nextYearTreatedAsCurrentYear.map(_.decrypted[BigDecimal]),
            nonUkCharities = g.nonUkCharities.map(_.decrypted[BigDecimal])
          )
      }
    }

    val dGifts: Option[Gifts] = {
      giftAid.gifts.map {
        g =>
          Gifts(
            investmentsNonUkCharitiesCharityNames = g.investmentsNonUkCharitiesCharityNames.map(_.map(_.decrypted[String])),
            landAndBuildings = g.landAndBuildings.map(_.decrypted[BigDecimal]),
            sharesOrSecurities = g.sharesOrSecurities.map(_.decrypted[BigDecimal]),
            investmentsNonUkCharities = g.investmentsNonUkCharities.map(_.decrypted[BigDecimal])
          )
      }
    }

    GiftAid(giftAidPayments = dGiftAidPayments, gifts = dGifts)
  }

  private def decryptEmployment(employment: EncryptedAllEmploymentData)(implicit associatedText: String): AllEmploymentData = {
    val hmrcExpenses = employment.hmrcExpenses.map {
      e =>
        EmploymentExpenses(
          submittedOn = e.submittedOn.map(_.decrypted[String]),
          dateIgnored = e.dateIgnored.map(_.decrypted[String]),
          totalExpenses = e.totalExpenses.map(_.decrypted[BigDecimal]),
          expenses = e.expenses.map(decryptExpenses)
        )
    }

    val customerExpenses = employment.customerExpenses.map {
      e =>
        EmploymentExpenses(
          submittedOn = e.submittedOn.map(_.decrypted[String]),
          dateIgnored = e.dateIgnored.map(_.decrypted[String]),
          totalExpenses = e.totalExpenses.map(_.decrypted[BigDecimal]),
          expenses = e.expenses.map(decryptExpenses)
        )
    }
    val hmrcEmploymentData = employment.hmrcEmploymentData.map(decryptHmrcEmploymentSource)
    val customerEmploymentData = employment.customerEmploymentData.map(decryptEmploymentSource)

    val otherEmploymentIncome = employment.otherEmploymentIncome.map(_.decrypted())

    AllEmploymentData(
      hmrcEmploymentData = hmrcEmploymentData,
      hmrcExpenses = hmrcExpenses,
      customerEmploymentData = customerEmploymentData,
      customerExpenses = customerExpenses,
      otherEmploymentIncome = otherEmploymentIncome
    )
  }

  private def encryptPensions(pensions: Pensions)(implicit associatedText: String): EncryptedPensions = {
    val ePensionReliefs: Option[EncryptedPensionReliefs] = {
      pensions.pensionReliefs.map {
        r =>
          EncryptedPensionReliefs(
            submittedOn = r.submittedOn.encrypted,
            deletedOn = r.deletedOn.map(_.encrypted),
            pensionReliefs = encryptReliefs(r.pensionReliefs)
          )
      }
    }

    val ePensionCharges: Option[EncryptedPensionCharges] = {
      pensions.pensionCharges.map {
        c =>
          EncryptedPensionCharges(
            submittedOn = c.submittedOn.encrypted,
            pensionSavingsTaxCharges = c.pensionSavingsTaxCharges.map(encryptPensionSavingsTaxCharges),
            pensionSchemeOverseasTransfers = c.pensionSchemeOverseasTransfers.map(encryptPensionSchemeOverseasTransfers),
            pensionSchemeUnauthorisedPayments = c.pensionSchemeUnauthorisedPayments.map(encryptPensionSchemeUnauthorisedPayments),
            pensionContributions = c.pensionContributions.map(encryptPensionContributions),
            overseasPensionContributions = c.overseasPensionContributions.map(encryptOverseasPensionContributions)
          )
      }
    }

    val eStateBenefitsModel: Option[EncryptedAllStateBenefitsData] = {
      pensions.stateBenefits.map {
        s =>
          EncryptedAllStateBenefitsData(
            stateBenefitsData = s.stateBenefitsData.map(_.encrypted()),
            customerAddedStateBenefitsData = s.customerAddedStateBenefitsData.map(_.encrypted())
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
            submittedOn = pI.submittedOn.encrypted,
            deletedOn = pI.deletedOn.map(_.encrypted),
            foreignPension = pI.foreignPension.map(_.map(encryptForeignPension)),
            overseasPensionContribution = pI.overseasPensionContribution.map(_.map(encryptOverseasPensionContribution))
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

  private def encryptEmploymentPensionModel(e: EmploymentPensionModel)(implicit associatedText: String): EncryptedEmploymentPensionModel = {
    EncryptedEmploymentPensionModel(
      employmentId = e.employmentId.encrypted,
      pensionSchemeName = e.pensionSchemeName.encrypted,
      pensionSchemeRef = e.pensionSchemeRef.map(_.encrypted),
      pensionId = e.pensionId.map(_.encrypted),
      startDate = e.startDate.map(_.encrypted),
      endDate = e.endDate.map(_.encrypted),
      amount = e.amount.map(_.encrypted),
      taxPaid = e.taxPaid.map(_.encrypted),
      isCustomerEmploymentData = e.isCustomerEmploymentData.map(_.encrypted)
    )
  }

  private def decryptEmploymentPensionModel(e: EncryptedEmploymentPensionModel)(implicit associatedText: String): EmploymentPensionModel = {
    EmploymentPensionModel(
      employmentId = e.employmentId.decrypted[String],
      pensionSchemeName = e.pensionSchemeName.decrypted[String],
      pensionSchemeRef = e.pensionSchemeRef.map(_.decrypted[String]),
      pensionId = e.pensionId.map(_.decrypted[String]),
      startDate = e.startDate.map(_.decrypted[String]),
      endDate = e.endDate.map(_.decrypted[String]),
      amount = e.amount.map(_.decrypted[BigDecimal]),
      taxPaid = e.taxPaid.map(_.decrypted[BigDecimal]),
      isCustomerEmploymentData = e.isCustomerEmploymentData.map(_.decrypted[Boolean])
    )
  }

  private def encryptForeignPension(f: ForeignPension)(implicit associatedText: String): EncryptedForeignPension = {
    EncryptedForeignPension(
      countryCode = f.countryCode.encrypted,
      taxableAmount = f.taxableAmount.encrypted,
      amountBeforeTax = f.amountBeforeTax.map(_.encrypted),
      taxTakenOff = f.taxTakenOff.map(_.encrypted),
      specialWithholdingTax = f.specialWithholdingTax.map(_.encrypted),
      foreignTaxCreditRelief = f.foreignTaxCreditRelief.map(_.encrypted)
    )
  }

  private def encryptOverseasPensionContribution(o: OverseasPensionContribution)(implicit associatedText: String): EncryptedOverseasPensionContribution = {
    EncryptedOverseasPensionContribution(
      customerReference = o.customerReference.map(_.encrypted),
      exemptEmployersPensionContribs = o.exemptEmployersPensionContribs.encrypted,
      migrantMemReliefQopsRefNo = o.migrantMemReliefQopsRefNo.map(_.encrypted),
      dblTaxationRelief = o.dblTaxationRelief.map(_.encrypted),
      dblTaxationCountry = o.dblTaxationCountry.map(_.encrypted),
      dblTaxationArticle = o.dblTaxationArticle.map(_.encrypted),
      dblTaxationTreaty = o.dblTaxationTreaty.map(_.encrypted),
      sf74Reference = o.sf74Reference.map(_.encrypted)
    )
  }


  private def encryptPensionContributions(p: PensionContributions)(implicit associatedText: String): EncryptedPensionContributions = {
    EncryptedPensionContributions(
      pensionSchemeTaxReference = p.pensionSchemeTaxReference.map(_.encrypted),
      inExcessOfTheAnnualAllowance = p.inExcessOfTheAnnualAllowance.encrypted,
      annualAllowanceTaxPaid = p.annualAllowanceTaxPaid.encrypted,
      isAnnualAllowanceReduced = p.isAnnualAllowanceReduced.map(_.encrypted),
      taperedAnnualAllowance = p.taperedAnnualAllowance.map(_.encrypted),
      moneyPurchasedAllowance = p.moneyPurchasedAllowance.map(_.encrypted)
    )
  }

  private def encryptPensionSchemeUnauthorisedPayments(p: PensionSchemeUnauthorisedPayments)
                                                      (implicit associatedText: String): EncryptedPensionSchemeUnauthorisedPayments = {
    EncryptedPensionSchemeUnauthorisedPayments(
      pensionSchemeTaxReference = p.pensionSchemeTaxReference.map(_.map(_.encrypted)),
      surcharge = p.surcharge.map(encryptCharge),
      noSurcharge = p.noSurcharge.map(encryptCharge)
    )
  }

  private def encryptPensionSchemeOverseasTransfers(p: PensionSchemeOverseasTransfers)
                                                   (implicit associatedText: String): EncryptedPensionSchemeOverseasTransfers = {
    EncryptedPensionSchemeOverseasTransfers(
      overseasSchemeProvider = p.overseasSchemeProvider.map(encryptOverseasSchemeProvider),
      transferCharge = p.transferCharge.encrypted,
      transferChargeTaxPaid = p.transferChargeTaxPaid.encrypted
    )
  }

  private def encryptPensionSavingsTaxCharges(p: PensionSavingsTaxCharges)(implicit associatedText: String): EncryptedPensionSavingsTaxCharges = {
    EncryptedPensionSavingsTaxCharges(
      pensionSchemeTaxReference = p.pensionSchemeTaxReference.map(_.map(_.encrypted)),
      lumpSumBenefitTakenInExcessOfLifetimeAllowance = p.lumpSumBenefitTakenInExcessOfLifetimeAllowance.map(encryptLifeTimeAllowance),
      benefitInExcessOfLifetimeAllowance = p.benefitInExcessOfLifetimeAllowance.map(encryptLifeTimeAllowance)
    )
  }

  private def encryptOverseasPensionContributions(o: OverseasPensionContributions)(implicit associatedText: String): EncryptedOverseasPensionContributions = {
    EncryptedOverseasPensionContributions(
      overseasSchemeProvider = o.overseasSchemeProvider.map(encryptOverseasSchemeProvider),
      shortServiceRefund = o.shortServiceRefund.encrypted,
      shortServiceRefundTaxPaid = o.shortServiceRefundTaxPaid.encrypted
    )
  }

  private def encryptCharge(charge: Charge)(implicit associatedText: String): EncryptedCharge = {
    EncryptedCharge(
      amount = charge.amount.encrypted,
      foreignTaxPaid = charge.foreignTaxPaid.encrypted
    )
  }

  private def encryptOverseasSchemeProvider(overseasSchemeProvider: OverseasSchemeProvider)
                                           (implicit associatedText: String): EncryptedOverseasSchemeProvider = {
    EncryptedOverseasSchemeProvider(
      providerName = overseasSchemeProvider.providerName.encrypted,
      providerAddress = overseasSchemeProvider.providerAddress.encrypted,
      providerCountryCode = overseasSchemeProvider.providerCountryCode.encrypted,
      qualifyingRecognisedOverseasPensionScheme = overseasSchemeProvider.qualifyingRecognisedOverseasPensionScheme.map(_.map(_.encrypted)),
      pensionSchemeTaxReference = overseasSchemeProvider.pensionSchemeTaxReference.map(_.map(_.encrypted))
    )
  }

  private def encryptLifeTimeAllowance(lifeTimeAllowance: LifetimeAllowance)(implicit associatedText: String): EncryptedLifetimeAllowance = {
    EncryptedLifetimeAllowance(
      amount = lifeTimeAllowance.amount.encrypted,
      taxPaid = lifeTimeAllowance.taxPaid.encrypted
    )
  }

  private def encryptReliefs(reliefs: Reliefs)(implicit associatedText: String): EncryptedReliefs = {
    EncryptedReliefs(
      regularPensionContributions = reliefs.regularPensionContributions.map(_.encrypted),
      oneOffPensionContributionsPaid = reliefs.oneOffPensionContributionsPaid.map(_.encrypted),
      retirementAnnuityPayments = reliefs.retirementAnnuityPayments.map(_.encrypted),
      paymentToEmployersSchemeNoTaxRelief = reliefs.paymentToEmployersSchemeNoTaxRelief.map(_.encrypted),
      overseasPensionSchemeContributions = reliefs.overseasPensionSchemeContributions.map(_.encrypted)
    )
  }

  private def decryptReliefs(reliefs: EncryptedReliefs)(implicit associatedText: String): Reliefs = {
    Reliefs(
      regularPensionContributions = reliefs.regularPensionContributions.map(_.decrypted[BigDecimal]),
      oneOffPensionContributionsPaid = reliefs.oneOffPensionContributionsPaid.map(_.decrypted[BigDecimal]),
      retirementAnnuityPayments = reliefs.retirementAnnuityPayments.map(_.decrypted[BigDecimal]),
      paymentToEmployersSchemeNoTaxRelief = reliefs.paymentToEmployersSchemeNoTaxRelief.map(_.decrypted[BigDecimal]),
      overseasPensionSchemeContributions = reliefs.overseasPensionSchemeContributions.map(_.decrypted[BigDecimal])
    )
  }

  private def decryptPensionSavingsTaxCharges(p: EncryptedPensionSavingsTaxCharges)(implicit associatedText: String): PensionSavingsTaxCharges = {
    PensionSavingsTaxCharges(
      pensionSchemeTaxReference = p.pensionSchemeTaxReference.map(_.map(_.decrypted[String])),
      lumpSumBenefitTakenInExcessOfLifetimeAllowance = p.lumpSumBenefitTakenInExcessOfLifetimeAllowance.map(decryptLifeTimeAllowance),
      benefitInExcessOfLifetimeAllowance = p.benefitInExcessOfLifetimeAllowance.map(decryptLifeTimeAllowance)
    )
  }

  private def decryptLifeTimeAllowance(lifeTimeAllowance: EncryptedLifetimeAllowance)(implicit associatedText: String): LifetimeAllowance = {
    LifetimeAllowance(
      amount = lifeTimeAllowance.amount.decrypted[BigDecimal],
      taxPaid = lifeTimeAllowance.taxPaid.decrypted[BigDecimal]
    )
  }

  private def decryptPensionSchemeOverseasTransfers(p: EncryptedPensionSchemeOverseasTransfers)
                                                   (implicit associatedText: String): PensionSchemeOverseasTransfers = {
    PensionSchemeOverseasTransfers(
      overseasSchemeProvider = p.overseasSchemeProvider.map(decryptOverseasSchemeProvider),
      transferCharge = p.transferCharge.decrypted[BigDecimal],
      transferChargeTaxPaid = p.transferChargeTaxPaid.decrypted[BigDecimal]
    )
  }

  private def decryptOverseasSchemeProvider(overseasSchemeProvider: EncryptedOverseasSchemeProvider)
                                           (implicit associatedText: String): OverseasSchemeProvider = {
    OverseasSchemeProvider(
      providerName = overseasSchemeProvider.providerName.decrypted[String],
      providerAddress = overseasSchemeProvider.providerAddress.decrypted[String],
      providerCountryCode = overseasSchemeProvider.providerCountryCode.decrypted[String],
      qualifyingRecognisedOverseasPensionScheme = overseasSchemeProvider.qualifyingRecognisedOverseasPensionScheme.map(_.map(
        _.decrypted[String])),
      pensionSchemeTaxReference = overseasSchemeProvider.pensionSchemeTaxReference.map(_.map(_.decrypted[String]))
    )
  }

  private def decryptPensionSchemeUnauthorisedPayments(p: EncryptedPensionSchemeUnauthorisedPayments)
                                                      (implicit associatedText: String): PensionSchemeUnauthorisedPayments = {
    PensionSchemeUnauthorisedPayments(
      pensionSchemeTaxReference = p.pensionSchemeTaxReference.map(_.map(_.decrypted[String])),
      surcharge = p.surcharge.map(decryptCharge),
      noSurcharge = p.noSurcharge.map(decryptCharge)
    )
  }

  private def decryptCharge(charge: EncryptedCharge)(implicit associatedText: String): Charge = {
    Charge(
      amount = charge.amount.decrypted[BigDecimal],
      foreignTaxPaid = charge.foreignTaxPaid.decrypted[BigDecimal]
    )
  }

  private def decryptPensionContributions(p: EncryptedPensionContributions)(implicit associatedText: String): PensionContributions = {
    PensionContributions(
      pensionSchemeTaxReference = p.pensionSchemeTaxReference.map(_.decrypted[String]),
      inExcessOfTheAnnualAllowance = p.inExcessOfTheAnnualAllowance.decrypted[BigDecimal],
      annualAllowanceTaxPaid = p.annualAllowanceTaxPaid.decrypted[BigDecimal],
      isAnnualAllowanceReduced = p.isAnnualAllowanceReduced.map(_.decrypted[Boolean]),
      taperedAnnualAllowance = p.taperedAnnualAllowance.map(_.decrypted[Boolean]),
      moneyPurchasedAllowance = p.moneyPurchasedAllowance.map(_.decrypted[Boolean])
    )
  }

  private def decryptOverseasPensionContributions(o: EncryptedOverseasPensionContributions)(implicit associatedText: String): OverseasPensionContributions = {
    OverseasPensionContributions(
      overseasSchemeProvider = o.overseasSchemeProvider.map(decryptOverseasSchemeProvider),
      shortServiceRefund = o.shortServiceRefund.decrypted[BigDecimal],
      shortServiceRefundTaxPaid = o.shortServiceRefundTaxPaid.decrypted[BigDecimal]
    )
  }


  private def decryptForeignPension(f: EncryptedForeignPension)(implicit associatedText: String): ForeignPension = {
    ForeignPension(
      countryCode = f.countryCode.decrypted[String],
      taxableAmount = f.taxableAmount.decrypted[BigDecimal],
      amountBeforeTax = f.amountBeforeTax.map(_.decrypted[BigDecimal]),
      taxTakenOff = f.taxTakenOff.map(_.decrypted[BigDecimal]),
      specialWithholdingTax = f.specialWithholdingTax.map(_.decrypted[BigDecimal]),
      foreignTaxCreditRelief = f.foreignTaxCreditRelief.map(_.decrypted[Boolean])
    )
  }

  private def decryptOverseasPensionContribution(o: EncryptedOverseasPensionContribution)(implicit associatedText: String): OverseasPensionContribution = {
    OverseasPensionContribution(
      customerReference = o.customerReference.map(_.decrypted[String]),
      exemptEmployersPensionContribs = o.exemptEmployersPensionContribs.decrypted[BigDecimal],
      migrantMemReliefQopsRefNo = o.migrantMemReliefQopsRefNo.map(_.decrypted[String]),
      dblTaxationRelief = o.dblTaxationRelief.map(_.decrypted[BigDecimal]),
      dblTaxationCountry = o.dblTaxationCountry.map(_.decrypted[String]),
      dblTaxationArticle = o.dblTaxationArticle.map(_.decrypted[String]),
      dblTaxationTreaty = o.dblTaxationTreaty.map(_.decrypted[String]),
      sf74Reference = o.sf74Reference.map(_.decrypted[String])
    )
  }

  private def decryptPensions(pensions: EncryptedPensions)(implicit associatedText: String): Pensions = {
    val pensionReliefs: Option[PensionReliefs] = pensions.pensionReliefs.map {
      r =>
        PensionReliefs(
          submittedOn = r.submittedOn.decrypted[String],
          deletedOn = r.deletedOn.map(_.decrypted[String]),
          pensionReliefs = decryptReliefs(r.pensionReliefs)
        )
    }

    val pensionCharges: Option[PensionCharges] = pensions.pensionCharges.map {
      c =>
        PensionCharges(
          submittedOn = c.submittedOn.decrypted[String],
          pensionSavingsTaxCharges = c.pensionSavingsTaxCharges.map(decryptPensionSavingsTaxCharges),
          pensionSchemeOverseasTransfers = c.pensionSchemeOverseasTransfers.map(decryptPensionSchemeOverseasTransfers),
          pensionSchemeUnauthorisedPayments = c.pensionSchemeUnauthorisedPayments.map(decryptPensionSchemeUnauthorisedPayments),
          pensionContributions = c.pensionContributions.map(decryptPensionContributions),
          overseasPensionContributions = c.overseasPensionContributions.map(decryptOverseasPensionContributions)
        )
    }

    val stateBenefitsModel: Option[AllStateBenefitsData] = pensions.stateBenefits.map {
      s: EncryptedAllStateBenefitsData =>
        AllStateBenefitsData(
          stateBenefitsData = s.stateBenefitsData.map(_.decrypted()),
          customerAddedStateBenefitsData = s.customerAddedStateBenefitsData.map(_.decrypted())
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
          submittedOn = s.submittedOn.decrypted[String],
          deletedOn = s.deletedOn.map(_.decrypted[String]),
          foreignPension = s.foreignPension.map(_.map(decryptForeignPension)),
          overseasPensionContribution = s.overseasPensionContribution.map(_.map(decryptOverseasPensionContribution))
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

  private def encryptGains(gains: InsurancePoliciesModel)(implicit associatedText: String): EncryptedInsurancePoliciesModel = {

    val eForeign: Option[Seq[EncryptedForeignModel]] = {
      gains.foreign.map {
        g =>
          g.map(f =>
            EncryptedForeignModel(
              customerReference = f.customerReference.map(_.encrypted),
              gainAmount = f.gainAmount.encrypted,
              taxPaidAmount = f.taxPaidAmount.map(_.encrypted),
              yearsHeld = f.yearsHeld.map(_.encrypted)
            )
          )
      }
    }
    val eCapitalRedemption: Option[Seq[EncryptedCapitalRedemptionModel]] = {
      gains.capitalRedemption.map {
        g =>
          g.map(c =>
            EncryptedCapitalRedemptionModel(
              customerReference = c.customerReference.map(_.encrypted),
              event = c.event.map(_.encrypted),
              gainAmount = c.gainAmount.encrypted,
              taxPaid = c.taxPaid.map(_.encrypted),
              yearsHeld = c.yearsHeld.map(_.encrypted),
              yearsHeldSinceLastGain = c.yearsHeldSinceLastGain.map(_.encrypted),
              deficiencyRelief = c.deficiencyRelief.map(_.encrypted)
            )
          )
      }
    }

    val eLifeAnnuity: Option[Seq[EncryptedLifeAnnuityModel]] = {
      gains.lifeAnnuity.map {
        g =>
          g.map(l =>
            EncryptedLifeAnnuityModel(
              customerReference = l.customerReference.map(_.encrypted),
              event = l.event.map(_.encrypted),
              gainAmount = l.gainAmount.encrypted,
              taxPaid = l.taxPaid.map(_.encrypted),
              yearsHeld = l.yearsHeld.map(_.encrypted),
              yearsHeldSinceLastGain = l.yearsHeldSinceLastGain.map(_.encrypted),
              deficiencyRelief = l.deficiencyRelief.map(_.encrypted)
            )
          )
      }
    }

    val eLifeInsurance: Option[Seq[EncryptedLifeInsuranceModel]] = {
      gains.lifeInsurance.map {
        g =>
          g.map(l =>
            EncryptedLifeInsuranceModel(
              customerReference = l.customerReference.map(_.encrypted),
              event = l.event.map(_.encrypted),
              gainAmount = l.gainAmount.encrypted,
              taxPaid = l.taxPaid.map(_.encrypted),
              yearsHeld = l.yearsHeld.map(_.encrypted),
              yearsHeldSinceLastGain = l.yearsHeldSinceLastGain.map(_.encrypted),
              deficiencyRelief = l.deficiencyRelief.map(_.encrypted)
            )
          )
      }
    }

    val eVoidedIsa: Option[Seq[EncryptedVoidedIsaModel]] = {
      gains.voidedIsa.map {
        g =>
          g.map(v =>
            EncryptedVoidedIsaModel(
              customerReference = v.customerReference.map(_.encrypted),
              event = v.event.map(_.encrypted),
              gainAmount = v.gainAmount.encrypted,
              taxPaidAmount = v.taxPaidAmount.map(_.encrypted),
              yearsHeld = v.yearsHeld.map(_.encrypted),
              yearsHeldSinceLastGain = v.yearsHeldSinceLastGain.map(_.encrypted)
            )
          )
      }
    }


    EncryptedInsurancePoliciesModel(
      submittedOn = gains.submittedOn.map(_.encrypted),
      lifeInsurance = eLifeInsurance,
      capitalRedemption = eCapitalRedemption,
      lifeAnnuity = eLifeAnnuity,
      voidedIsa = eVoidedIsa,
      foreign = eForeign
    )
  }

  private def decryptGains(gains: EncryptedInsurancePoliciesModel)(implicit associatedText: String): InsurancePoliciesModel = {

    val eForeign: Option[Seq[ForeignModel]] = {
      gains.foreign.map {
        g =>
          g.map(f =>
            ForeignModel(
              customerReference = f.customerReference.map(_.decrypted[String]),
              gainAmount = f.gainAmount.decrypted[BigDecimal],
              taxPaidAmount = f.taxPaidAmount.map(_.decrypted[BigDecimal]),
              yearsHeld = f.yearsHeld.map(_.decrypted[Int])
            )
          )
      }
    }

    val eCapitalRedemption: Option[Seq[CapitalRedemptionModel]] = {
      gains.capitalRedemption.map {
        g =>
          g.map(c =>
            CapitalRedemptionModel(
              customerReference = c.customerReference.map(_.decrypted[String]),
              event = c.event.map(_.decrypted[String]),
              gainAmount = c.gainAmount.decrypted[BigDecimal],
              taxPaid = c.taxPaid.map(_.decrypted[Boolean]),
              yearsHeld = c.yearsHeld.map(_.decrypted[Int]),
              yearsHeldSinceLastGain = c.yearsHeldSinceLastGain.map(_.decrypted[Int]),
              deficiencyRelief = c.deficiencyRelief.map(_.decrypted[BigDecimal])
            )
          )
      }
    }

    val eLifeAnnuity: Option[Seq[LifeAnnuityModel]] = {
      gains.lifeAnnuity.map {
        g =>
          g.map(l =>
            LifeAnnuityModel(
              customerReference = l.customerReference.map(_.decrypted[String]),
              event = l.event.map(_.decrypted[String]),
              gainAmount = l.gainAmount.decrypted[BigDecimal],
              taxPaid = l.taxPaid.map(_.decrypted[Boolean]),
              yearsHeld = l.yearsHeld.map(_.decrypted[Int]),
              yearsHeldSinceLastGain = l.yearsHeldSinceLastGain.map(_.decrypted[Int]),
              deficiencyRelief = l.deficiencyRelief.map(_.decrypted[BigDecimal])
            )
          )
      }
    }

    val eLifeInsurance: Option[Seq[LifeInsuranceModel]] = {
      gains.lifeInsurance.map {
        g =>
          g.map(l =>
          LifeInsuranceModel(
            customerReference = l.customerReference.map(_.decrypted[String]),
            event = l.event.map(_.decrypted[String]),
            gainAmount = l.gainAmount.decrypted[BigDecimal],
            taxPaid = l.taxPaid.map(_.decrypted[Boolean]),
            yearsHeld = l.yearsHeld.map(_.decrypted[Int]),
            yearsHeldSinceLastGain = l.yearsHeldSinceLastGain.map(_.decrypted[Int]),
            deficiencyRelief = l.deficiencyRelief.map(_.decrypted[BigDecimal])
          )
          )
      }
    }

    val eVoidedIsa: Option[Seq[VoidedIsaModel]] = {
      gains.voidedIsa.map {
        g =>
          g.map(v =>
            VoidedIsaModel(
              customerReference = v.customerReference.map(_.decrypted[String]),
              event = v.event.map(_.decrypted[String]),
              gainAmount = v.gainAmount.decrypted[BigDecimal],
              taxPaidAmount = v.taxPaidAmount.map(_.decrypted[BigDecimal]),
              yearsHeld = v.yearsHeld.map(_.decrypted[Int]),
              yearsHeldSinceLastGain = v.yearsHeldSinceLastGain.map(_.decrypted[Int])
            )
          )
      }
    }


    InsurancePoliciesModel(
      submittedOn = gains.submittedOn.map(_.decrypted[String]),
      lifeInsurance = eLifeInsurance,
      capitalRedemption = eCapitalRedemption,
      lifeAnnuity = eLifeAnnuity,
      voidedIsa = eVoidedIsa,
      foreign = eForeign
    )
  }
}
