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

package models.gains


import models._
import play.api.libs.json.{JsObject, Json}
import utils.TestUtils

class InsurancePoliciesModelSpec extends TestUtils {

  val validLifeInsuranceModel: LifeInsuranceModel = LifeInsuranceModel(
    customerReference = Some("RefNo13254687"),
    event = Some("Life"),
    gainAmount = 123.45,
    taxPaid = Some(true),
    yearsHeld = Some(4),
    yearsHeldSinceLastGain = Some(3),
    deficiencyRelief = Some(123.45)
  )

  val validCapitalRedemptionModel: CapitalRedemptionModel = CapitalRedemptionModel(
    customerReference = Some("RefNo13254687"),
    event = Some("Capital"),
    gainAmount = 123.45,
    taxPaid = Some(true),
    yearsHeld = Some(3),
    yearsHeldSinceLastGain = Some(2),
    deficiencyRelief = Some(0)
  )

  val validLifeAnnuityModel: LifeAnnuityModel = LifeAnnuityModel(
    customerReference = Some("RefNo13254687"),
    event = Some("Life"),
    gainAmount = 0,
    taxPaid = Some(true),
    yearsHeld = Some(2),
    yearsHeldSinceLastGain = Some(22),
    deficiencyRelief = Some(123.45)
  )

  val validVoidedIsaModel: VoidedIsaModel = VoidedIsaModel(
    customerReference = Some("RefNo13254687"),
    event = Some("isa"),
    gainAmount = 123.45,
    taxPaidAmount = Some(123.45),
    yearsHeld = Some(5),
    yearsHeldSinceLastGain = Some(6)
  )

  val validForeignModel: ForeignModel = ForeignModel(
    customerReference = Some("RefNo13254687"),
    gainAmount = 123.45,
    taxPaidAmount = Some(123.45),
    yearsHeld = Some(3)
  )

  val submittedOn = "2020-01-04T05:01:01Z"
  val validInsurancePoliciesModel: InsurancePoliciesModel = InsurancePoliciesModel(
    submittedOn,
    Seq(validLifeInsuranceModel),
    Some(Seq(validCapitalRedemptionModel)),
    Some(Seq(validLifeAnnuityModel)),
    Some(Seq(validVoidedIsaModel)),
    Some(Seq(validForeignModel))
  )

  val validJson: JsObject = Json.obj(
    "submittedOn" -> "2020-01-04T05:01:01Z",
    "lifeInsurance" -> Json.arr(
      Json.obj(
        "customerReference" -> "RefNo13254687",
        "event" -> "Life",
        "gainAmount" -> 123.45,
        "taxPaid" -> true,
        "yearsHeld" -> 4,
        "yearsHeldSinceLastGain" -> 3,
        "deficiencyRelief" -> 123.45
      )
    ),
    "capitalRedemption" -> Json.arr(
      Json.obj(
        "customerReference" -> "RefNo13254687",
        "event" -> "Capital",
        "gainAmount" -> 123.45,
        "taxPaid" -> true,
        "yearsHeld" -> 3,
        "yearsHeldSinceLastGain" -> 2,
        "deficiencyRelief" -> 0
      )
    ),
    "lifeAnnuity" -> Json.arr(
      Json.obj(
        "customerReference" -> "RefNo13254687",
        "event" -> "Life",
        "gainAmount" -> 0,
        "taxPaid" -> true,
        "yearsHeld" -> 2,
        "yearsHeldSinceLastGain" -> 22,
        "deficiencyRelief" -> 123.45
      )
    ),
    "voidedIsa" -> Json.arr(
      Json.obj(
        "customerReference" -> "RefNo13254687",
        "event" -> "isa",
        "gainAmount" -> 123.45,
        "taxPaidAmount" -> 123.45,
        "yearsHeld" -> 5,
        "yearsHeldSinceLastGain" -> 6
      )
    ),
    "foreign" -> Json.arr(
      Json.obj(
        "customerReference" -> "RefNo13254687",
        "gainAmount" -> 123.45,
        "taxPaidAmount" -> 123.45,
        "yearsHeld" -> 3
      )
    )
  )

  "InsurancePoliciesModel" should {

    "parse from json" in {
      validJson.as[InsurancePoliciesModel] mustBe validInsurancePoliciesModel
    }

    "parse to json" in {
      Json.toJson(validInsurancePoliciesModel) mustBe validJson
    }

  }

}
