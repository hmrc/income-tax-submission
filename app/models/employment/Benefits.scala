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

package models.employment

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{OFormat, __}
import utils.EncryptedValue

case class Benefits(accommodation: Option[BigDecimal] = None,
                    assets: Option[BigDecimal] = None,
                    assetTransfer: Option[BigDecimal] = None,
                    beneficialLoan: Option[BigDecimal] = None,
                    car: Option[BigDecimal] = None,
                    carFuel: Option[BigDecimal] = None,
                    educationalServices: Option[BigDecimal] = None,
                    entertaining: Option[BigDecimal] = None,
                    expenses: Option[BigDecimal] = None,
                    medicalInsurance: Option[BigDecimal] = None,
                    telephone: Option[BigDecimal] = None,
                    service: Option[BigDecimal] = None,
                    taxableExpenses: Option[BigDecimal] = None,
                    van: Option[BigDecimal] = None,
                    vanFuel: Option[BigDecimal] = None,
                    mileage: Option[BigDecimal] = None,
                    nonQualifyingRelocationExpenses: Option[BigDecimal] = None,
                    nurseryPlaces: Option[BigDecimal] = None,
                    otherItems: Option[BigDecimal] = None,
                    paymentsOnEmployeesBehalf: Option[BigDecimal] = None,
                    personalIncidentalExpenses: Option[BigDecimal] = None,
                    qualifyingRelocationExpenses: Option[BigDecimal] = None,
                    employerProvidedProfessionalSubscriptions: Option[BigDecimal] = None,
                    employerProvidedServices: Option[BigDecimal] = None,
                    incomeTaxPaidByDirector: Option[BigDecimal] = None,
                    travelAndSubsistence: Option[BigDecimal] = None,
                    vouchersAndCreditCards: Option[BigDecimal] = None,
                    nonCash: Option[BigDecimal] = None)

object Benefits {
  val firstSetOfFields: OFormat[(Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[BigDecimal],
    Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[BigDecimal],
    Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[BigDecimal],
    Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[BigDecimal],
    Option[BigDecimal], Option[BigDecimal], Option[BigDecimal])] = (
    (__ \ "accommodation").formatNullable[BigDecimal] and
      (__ \ "assets").formatNullable[BigDecimal] and
      (__ \ "assetTransfer").formatNullable[BigDecimal] and
      (__ \ "beneficialLoan").formatNullable[BigDecimal] and
      (__ \ "car").formatNullable[BigDecimal] and
      (__ \ "carFuel").formatNullable[BigDecimal] and
      (__ \ "educationalServices").formatNullable[BigDecimal] and
      (__ \ "entertaining").formatNullable[BigDecimal] and
      (__ \ "expenses").formatNullable[BigDecimal] and
      (__ \ "medicalInsurance").formatNullable[BigDecimal] and
      (__ \ "telephone").formatNullable[BigDecimal] and
      (__ \ "service").formatNullable[BigDecimal] and
      (__ \ "taxableExpenses").formatNullable[BigDecimal] and
      (__ \ "van").formatNullable[BigDecimal] and
      (__ \ "vanFuel").formatNullable[BigDecimal] and
      (__ \ "mileage").formatNullable[BigDecimal] and
      (__ \ "nonQualifyingRelocationExpenses").formatNullable[BigDecimal] and
      (__ \ "nurseryPlaces").formatNullable[BigDecimal] and
      (__ \ "otherItems").formatNullable[BigDecimal] and
      (__ \ "paymentsOnEmployeesBehalf").formatNullable[BigDecimal] and
      (__ \ "personalIncidentalExpenses").formatNullable[BigDecimal] and
      (__ \ "qualifyingRelocationExpenses").formatNullable[BigDecimal]
    ).tupled

  val secondSetOfFields: OFormat[(Option[BigDecimal], Option[BigDecimal], Option[BigDecimal], Option[BigDecimal],
    Option[BigDecimal], Option[BigDecimal])] = (
    (__ \ "employerProvidedProfessionalSubscriptions").formatNullable[BigDecimal] and
      (__ \ "employerProvidedServices").formatNullable[BigDecimal] and
      (__ \ "incomeTaxPaidByDirector").formatNullable[BigDecimal] and
      (__ \ "travelAndSubsistence").formatNullable[BigDecimal] and
      (__ \ "vouchersAndCreditCards").formatNullable[BigDecimal] and
      (__ \ "nonCash").formatNullable[BigDecimal]
    ).tupled

  implicit val format: OFormat[Benefits] = {
    (firstSetOfFields and secondSetOfFields).apply({
      case (
        (accommodation, assets, assetTransfer, beneficialLoan, car, carFuel, educationalServices, entertaining,
        expenses, medicalInsurance, telephone, service, taxableExpenses, van, vanFuel, mileage, nonQualifyingRelocationExpenses,
        nurseryPlaces, otherItems, paymentsOnEmployeesBehalf, personalIncidentalExpenses, qualifyingRelocationExpenses),
        (employerProvidedProfessionalSubscriptions, employerProvidedServices, incomeTaxPaidByDirector, travelAndSubsistence,
        vouchersAndCreditCards, nonCash)
        ) =>
        Benefits(
          accommodation, assets, assetTransfer, beneficialLoan, car, carFuel, educationalServices, entertaining, expenses,
          medicalInsurance, telephone, service, taxableExpenses, van, vanFuel, mileage, nonQualifyingRelocationExpenses,
          nurseryPlaces, otherItems, paymentsOnEmployeesBehalf, personalIncidentalExpenses, qualifyingRelocationExpenses,
          employerProvidedProfessionalSubscriptions, employerProvidedServices, incomeTaxPaidByDirector, travelAndSubsistence,
          vouchersAndCreditCards, nonCash
        )
    }, {
      benefits =>
        (
          (benefits.accommodation, benefits.assets, benefits.assetTransfer, benefits.beneficialLoan, benefits.car, benefits.carFuel,
            benefits.educationalServices, benefits.entertaining, benefits.expenses, benefits.medicalInsurance, benefits.telephone,
            benefits.service, benefits.taxableExpenses, benefits.van, benefits.vanFuel, benefits.mileage,
            benefits.nonQualifyingRelocationExpenses, benefits.nurseryPlaces, benefits.otherItems, benefits.paymentsOnEmployeesBehalf,
            benefits.personalIncidentalExpenses, benefits.qualifyingRelocationExpenses),
          (benefits.employerProvidedProfessionalSubscriptions, benefits.employerProvidedServices, benefits.incomeTaxPaidByDirector,
            benefits.travelAndSubsistence, benefits.vouchersAndCreditCards, benefits.nonCash)
        )
    })
  }
}

case class EncryptedBenefits(accommodation: Option[EncryptedValue] = None,
                             assets: Option[EncryptedValue] = None,
                             assetTransfer: Option[EncryptedValue] = None,
                             beneficialLoan: Option[EncryptedValue] = None,
                             car: Option[EncryptedValue] = None,
                             carFuel: Option[EncryptedValue] = None,
                             educationalServices: Option[EncryptedValue] = None,
                             entertaining: Option[EncryptedValue] = None,
                             expenses: Option[EncryptedValue] = None,
                             medicalInsurance: Option[EncryptedValue] = None,
                             telephone: Option[EncryptedValue] = None,
                             service: Option[EncryptedValue] = None,
                             taxableExpenses: Option[EncryptedValue] = None,
                             van: Option[EncryptedValue] = None,
                             vanFuel: Option[EncryptedValue] = None,
                             mileage: Option[EncryptedValue] = None,
                             nonQualifyingRelocationExpenses: Option[EncryptedValue] = None,
                             nurseryPlaces: Option[EncryptedValue] = None,
                             otherItems: Option[EncryptedValue] = None,
                             paymentsOnEmployeesBehalf: Option[EncryptedValue] = None,
                             personalIncidentalExpenses: Option[EncryptedValue] = None,
                             qualifyingRelocationExpenses: Option[EncryptedValue] = None,
                             employerProvidedProfessionalSubscriptions: Option[EncryptedValue] = None,
                             employerProvidedServices: Option[EncryptedValue] = None,
                             incomeTaxPaidByDirector: Option[EncryptedValue] = None,
                             travelAndSubsistence: Option[EncryptedValue] = None,
                             vouchersAndCreditCards: Option[EncryptedValue] = None,
                             nonCash: Option[EncryptedValue] = None)

object EncryptedBenefits {
  val firstSetOfFields: OFormat[(Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue],
    Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue],
    Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue],
    Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue],
    Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue])] = (
    (__ \ "accommodation").formatNullable[EncryptedValue] and
      (__ \ "assets").formatNullable[EncryptedValue] and
      (__ \ "assetTransfer").formatNullable[EncryptedValue] and
      (__ \ "beneficialLoan").formatNullable[EncryptedValue] and
      (__ \ "car").formatNullable[EncryptedValue] and
      (__ \ "carFuel").formatNullable[EncryptedValue] and
      (__ \ "educationalServices").formatNullable[EncryptedValue] and
      (__ \ "entertaining").formatNullable[EncryptedValue] and
      (__ \ "expenses").formatNullable[EncryptedValue] and
      (__ \ "medicalInsurance").formatNullable[EncryptedValue] and
      (__ \ "telephone").formatNullable[EncryptedValue] and
      (__ \ "service").formatNullable[EncryptedValue] and
      (__ \ "taxableExpenses").formatNullable[EncryptedValue] and
      (__ \ "van").formatNullable[EncryptedValue] and
      (__ \ "vanFuel").formatNullable[EncryptedValue] and
      (__ \ "mileage").formatNullable[EncryptedValue] and
      (__ \ "nonQualifyingRelocationExpenses").formatNullable[EncryptedValue] and
      (__ \ "nurseryPlaces").formatNullable[EncryptedValue] and
      (__ \ "otherItems").formatNullable[EncryptedValue] and
      (__ \ "paymentsOnEmployeesBehalf").formatNullable[EncryptedValue] and
      (__ \ "personalIncidentalExpenses").formatNullable[EncryptedValue] and
      (__ \ "qualifyingRelocationExpenses").formatNullable[EncryptedValue]
    ).tupled

  val secondSetOfFields: OFormat[(Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue], Option[EncryptedValue],
    Option[EncryptedValue], Option[EncryptedValue])] = (
    (__ \ "employerProvidedProfessionalSubscriptions").formatNullable[EncryptedValue] and
      (__ \ "employerProvidedServices").formatNullable[EncryptedValue] and
      (__ \ "incomeTaxPaidByDirector").formatNullable[EncryptedValue] and
      (__ \ "travelAndSubsistence").formatNullable[EncryptedValue] and
      (__ \ "vouchersAndCreditCards").formatNullable[EncryptedValue] and
      (__ \ "nonCash").formatNullable[EncryptedValue]
    ).tupled

  implicit val format: OFormat[EncryptedBenefits] = {
    (firstSetOfFields and secondSetOfFields).apply({
      case (
        (accommodation, assets, assetTransfer, beneficialLoan, car, carFuel, educationalServices, entertaining,
        expenses, medicalInsurance, telephone, service, taxableExpenses, van, vanFuel, mileage, nonQualifyingRelocationExpenses,
        nurseryPlaces, otherItems, paymentsOnEmployeesBehalf, personalIncidentalExpenses, qualifyingRelocationExpenses),
        (employerProvidedProfessionalSubscriptions, employerProvidedServices, incomeTaxPaidByDirector, travelAndSubsistence,
        vouchersAndCreditCards, nonCash)
        ) =>
        EncryptedBenefits(
          accommodation, assets, assetTransfer, beneficialLoan, car, carFuel, educationalServices, entertaining, expenses,
          medicalInsurance, telephone, service, taxableExpenses, van, vanFuel, mileage, nonQualifyingRelocationExpenses,
          nurseryPlaces, otherItems, paymentsOnEmployeesBehalf, personalIncidentalExpenses, qualifyingRelocationExpenses,
          employerProvidedProfessionalSubscriptions, employerProvidedServices, incomeTaxPaidByDirector, travelAndSubsistence,
          vouchersAndCreditCards, nonCash
        )
    }, {
      benefits =>
        (
          (benefits.accommodation, benefits.assets, benefits.assetTransfer, benefits.beneficialLoan, benefits.car, benefits.carFuel,
            benefits.educationalServices, benefits.entertaining, benefits.expenses, benefits.medicalInsurance, benefits.telephone,
            benefits.service, benefits.taxableExpenses, benefits.van, benefits.vanFuel, benefits.mileage,
            benefits.nonQualifyingRelocationExpenses, benefits.nurseryPlaces, benefits.otherItems, benefits.paymentsOnEmployeesBehalf,
            benefits.personalIncidentalExpenses, benefits.qualifyingRelocationExpenses),
          (benefits.employerProvidedProfessionalSubscriptions, benefits.employerProvidedServices, benefits.incomeTaxPaidByDirector,
            benefits.travelAndSubsistence, benefits.vouchersAndCreditCards, benefits.nonCash)
        )
    })
  }
}
