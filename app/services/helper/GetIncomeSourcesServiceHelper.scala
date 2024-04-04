/*
 * Copyright 2024 HM Revenue & Customs
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

package services.helper

import cats.implicits.{catsSyntaxOptionId, none, toBifunctorOps}
import models.APIErrorModel
import models.employment.AllEmploymentData
import models.pensions.Pensions
import models.pensions.employmentPensions.EmploymentPensions

object GetIncomeSourcesServiceHelper {

  def handlePensions(pensionsOutcome: Either[APIErrorModel, Option[Pensions]],
                     employmentOutcome: Either[APIErrorModel, Option[AllEmploymentData]]): Option[Pensions] = {

    def handleEmployment: Option[EmploymentPensions] =
      employmentOutcome match {
        case Right(Some(employment)) => employment.buildEmploymentPensions().some
        case _                       => none[EmploymentPensions]
      }

    pensionsOutcome
      .map { maybePensions =>
        maybePensions
          .map(_.copy(employmentPensions = handleEmployment))
          .orElse(
            handleEmployment.fold(none[Pensions]) { employment =>
              Pensions.empty
                .copy(employmentPensions = employment.some)
                .some
            }
          )
      }
      .leftMap(_ => Pensions.empty.some)
      .merge
  }

}
