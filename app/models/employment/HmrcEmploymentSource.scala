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

package models.employment

import java.time.Instant
import play.api.libs.json.{Format, Json, OFormat}
import play.api.Logging
import uk.gov.hmrc.crypto.EncryptedValue

case class HmrcEmploymentSource(employmentId: String,
                                employerName: String,
                                employerRef: Option[String],
                                payrollId: Option[String],
                                startDate: Option[String],
                                cessationDate: Option[String],
                                dateIgnored: Option[String],
                                submittedOn: Option[String],
                                hmrcEmploymentFinancialData: Option[EmploymentFinancialData],
                                customerEmploymentFinancialData: Option[EmploymentFinancialData],
                                occupationalPension: Option[Boolean]) extends Logging {

  //scalastyle:off
  lazy val hasOccupationalPension: Boolean = occupationalPension.contains(true) || hmrcEmploymentFinancialData.exists(_.hasOccPen)
  //scalastyle:on


  private def parseDate(submittedOn: String): Option[Instant] = {
    try {
      Some(Instant.parse(submittedOn))
    } catch {
      case e: Exception =>
        logger.error(s"[HmrcEmploymentSource][parseDate] Couldn't parse submitted on to DateTime - ${e.getMessage}")
        None
    }
  }

  def getLatestEmploymentFinancialData: Option[EmploymentFinancialData] = {

    (hmrcEmploymentFinancialData, customerEmploymentFinancialData) match {
      case (None, None) => None
      case (Some(hmrc), None) => Some(hmrc)
      case (None, Some(customer)) => Some(customer)
      case (Some(hmrc), Some(customer)) =>

        val hmrcSubmittedOn: Option[Instant] = hmrc.employmentData.map(_.submittedOn).flatMap(parseDate)
        val customerSubmittedOn: Option[Instant] = customer.employmentData.map(_.submittedOn).flatMap(parseDate)

        Some((hmrcSubmittedOn,customerSubmittedOn) match {
          case (Some(hmrcSubmittedOn), Some(customerSubmittedOn)) => if(hmrcSubmittedOn.isAfter(customerSubmittedOn)) hmrc else customer
          case (Some(_), None) => hmrc
          case (None, Some(_)) => customer
          case (None, None) => customer
        })
    }
  }
}

object HmrcEmploymentSource {
  implicit val format: OFormat[HmrcEmploymentSource] = Json.format[HmrcEmploymentSource]
}

case class EncryptedHmrcEmploymentSource(employmentId: EncryptedValue,
                                         employerName: EncryptedValue,
                                         employerRef: Option[EncryptedValue],
                                         payrollId: Option[EncryptedValue],
                                         startDate: Option[EncryptedValue],
                                         cessationDate: Option[EncryptedValue],
                                         dateIgnored: Option[EncryptedValue],
                                         submittedOn: Option[EncryptedValue],
                                         hmrcEmploymentFinancialData: Option[EncryptedEmploymentFinancialData],
                                         customerEmploymentFinancialData: Option[EncryptedEmploymentFinancialData],
                                         occupationalPension: Option[EncryptedValue])

object EncryptedHmrcEmploymentSource {
  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]

  implicit val format: Format[EncryptedHmrcEmploymentSource] = Json.format[EncryptedHmrcEmploymentSource]
}


