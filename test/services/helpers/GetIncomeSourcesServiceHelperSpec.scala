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

package services.helpers

import builders.models.employment.AllEmploymentDataBuilder.anAllEmploymentDataWithOccPen
import builders.models.pensions.PensionsBuilder.aPensions
import cats.implicits.{catsSyntaxEitherId, catsSyntaxOptionId, none}
import models.employment.AllEmploymentData
import models.pensions.Pensions
import models.{APIErrorBodyModel, APIErrorModel}
import org.scalatest.EitherValues.convertEitherToValuable
import org.scalatest.OptionValues.convertOptionToValuable
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.BAD_REQUEST
import services.helpers.GetIncomeSourcesServiceHelper.handlePensions

class GetIncomeSourcesServiceHelperSpec extends AnyWordSpec with Matchers {

  val downstreamError = APIErrorModel(BAD_REQUEST, APIErrorBodyModel("SOME_ERROR", "SOME_REASON")).asLeft

  val emptyPensionsCall    = none[Pensions].asRight
  val nonEmptyPensionsCall = aPensions.some.asRight

  val nonEmptyEmploymentCall = anAllEmploymentDataWithOccPen.some.asRight
  val emptyEmploymentCall    = none[AllEmploymentData].asRight

  "handling pensions call" when {
    "the pensions downstream returns an error" should {
      "return an empty pensions model" in {
        handlePensions(downstreamError, nonEmptyEmploymentCall) shouldBe Pensions.empty.some
      }
    }
    "downstream calls are successful" when {
      "no pensions data was retrieved" when {
        "no employment was retrieved" should {
          "return None" in {
            handlePensions(emptyPensionsCall, emptyEmploymentCall) shouldBe none[Pensions]
          }
        }
        "employment was retrieved" should {
          "add employment to the empty pensions model" in {
            val expectedEmplPens = nonEmptyEmploymentCall.value.value.buildEmploymentPensions()
            val expectedResult   = Pensions.empty.copy(employmentPensions = expectedEmplPens.some)

            handlePensions(emptyPensionsCall, nonEmptyEmploymentCall) shouldBe expectedResult.some
          }
        }
      }
      "pensions data was retrieved" when {
        "employment data was retrieved" should {
          "add employment to pensions" in {
            val expectedEmplPens = nonEmptyEmploymentCall.value.value.buildEmploymentPensions()
            val expectedResult   = nonEmptyPensionsCall.value.value.copy(employmentPensions = expectedEmplPens.some)

            handlePensions(nonEmptyPensionsCall, nonEmptyEmploymentCall) shouldBe expectedResult.some
          }
        }
        "no employment was retrieved (or the calls were unsuccessful)" should {
          "not add employment to pensions" in {
            val expectedResult = nonEmptyPensionsCall.value.value

            handlePensions(nonEmptyPensionsCall, emptyEmploymentCall) shouldBe expectedResult.some
          }
        }
      }
    }
  }
}
