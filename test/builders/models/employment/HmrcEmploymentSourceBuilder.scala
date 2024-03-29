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

package builders.models.employment

import builders.models.employment.EmploymentFinancialDataBuilder.{anEmploymentFinancialData, anEmploymentFinancialDataOccPen}
import models.employment.HmrcEmploymentSource

object HmrcEmploymentSourceBuilder {

  val aHmrcEmploymentSource: HmrcEmploymentSource = HmrcEmploymentSource(
    employmentId = "00000000-0000-0000-1111-000000000000",
    employerName = "default-employer",
    employerRef = Some("666/66666"),
    payrollId = Some("1234567890"),
    startDate = Some("2020-01-01"),
    cessationDate = Some("2020-01-01"),
    dateIgnored = None,
    submittedOn = Some("2020-01-04T05:01:01Z"),
    hmrcEmploymentFinancialData = Some(anEmploymentFinancialData),
    customerEmploymentFinancialData = Some(anEmploymentFinancialData),
    occupationalPension = None
  )

  val aHmrcEmploymentSourceOccPen: HmrcEmploymentSource = HmrcEmploymentSource(
    employmentId = "00000000-0000-0000-1111-000000000000",
    employerName = "default-employer",
    employerRef = Some("666/66666"),
    payrollId = Some("1234567890"),
    startDate = Some("2020-01-01"),
    cessationDate = Some("2020-01-01"),
    dateIgnored = None,
    submittedOn = Some("2020-01-04T05:01:01Z"),
    hmrcEmploymentFinancialData = Some(anEmploymentFinancialDataOccPen),
    customerEmploymentFinancialData = Some(anEmploymentFinancialData),
    occupationalPension = None
  )
}