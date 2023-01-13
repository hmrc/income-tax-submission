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

package models

import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import utils.TestUtils

class UserSpec extends TestUtils {

  ".asAgent" should {

    "return true" when {

      "the user has an ARN" in {

        User[AnyContent]("12234567890", Some("1234567890"), "AA123456A", "sessionId")(FakeRequest()).isAgent mustBe true

      }
    }

    "return false" when {

      "the user does not have an ARN" in {

        User[AnyContent]("12234567890", None, "AA123456A", "sessionId")(FakeRequest()).isAgent mustBe false

      }
    }

  }

}
