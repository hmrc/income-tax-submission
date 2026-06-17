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

package utils

import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.crypto.EncryptedValue
import utils.Cypher.{bigDecimalCypher, booleanCypher, stringCypher}

class CypherSpec extends AnyWordSpec with MockitoSugar with Matchers with BeforeAndAfterEach {

  private val encryptedBoolean = EncryptedValue("encrypted-boolean", "nonce-1")
  private val encryptedString = EncryptedValue("encrypted-string", "nonce-2")
  private val encryptedBigDecimal = EncryptedValue("encrypted-bigdecimal", "nonce-3")
  private val encryptedValue = EncryptedValue("some-value", "some-nonce")

  private implicit val aesGcmAdCrypto: AesGcmAdCrypto = mock[AesGcmAdCrypto]
  private implicit val associatedText: String = "some-associated-text"

  "stringCypher" should {
    val stringValue = "some-string-value"
    "encrypt string values" in {
      when(aesGcmAdCrypto.encrypt(stringValue)(associatedText)).thenReturn(encryptedString)

      stringCypher.encrypt(stringValue) shouldBe encryptedString
    }

    "decrypt to string values" in {
      when(aesGcmAdCrypto.decrypt(encryptedValue)(associatedText)).thenReturn(stringValue)

      stringCypher.decrypt(encryptedValue) shouldBe stringValue
    }
  }

  "booleanCypher" should {
    val someBoolean = true
    "encrypt boolean values" in {
      when(aesGcmAdCrypto.encrypt(someBoolean.toString)(associatedText)).thenReturn(encryptedBoolean)

      booleanCypher.encrypt(someBoolean) shouldBe encryptedBoolean
    }

    "decrypt to boolean values" in {
      when(aesGcmAdCrypto.decrypt(encryptedValue)(associatedText)).thenReturn(someBoolean.toString)

      booleanCypher.decrypt(encryptedValue) shouldBe someBoolean
    }
  }

  "bigDecimalCypher" should {
    val bigDecimalValue: BigDecimal = 500.0
    "encrypt BigDecimal values" in {
      when(aesGcmAdCrypto.encrypt(bigDecimalValue.toString)(associatedText)).thenReturn(encryptedBigDecimal)

      bigDecimalCypher.encrypt(bigDecimalValue) shouldBe encryptedBigDecimal
    }

    "decrypt to BigDecimal values" in {
      when(aesGcmAdCrypto.decrypt(encryptedValue)(associatedText)).thenReturn(bigDecimalValue.toString)

      bigDecimalCypher.decrypt(encryptedValue) shouldBe bigDecimalValue
    }
  }
}
