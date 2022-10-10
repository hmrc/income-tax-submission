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

package utils

import models.mongo.TextAndKey

import java.time.{Instant, LocalDate}
import java.util.UUID

trait Encryptable[A] {
  def encrypt(value: A)(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedValue

  def decrypt(value: EncryptedValue)(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): A
}

object Encryptable {
  def encrypt[A](value: A)(implicit e: Encryptable[A], secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedValue = e.encrypt(value)

  def decrypt[A](value: EncryptedValue)(implicit e: Encryptable[A], secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): A = e.decrypt(value)
}


object EncryptableInstances {

  implicit val stringEncryptable: Encryptable[String] = new Encryptable[String] {
    def encrypt(value: String)(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedValue = secureGCMCipher.encrypt(value)

    def decrypt(value: EncryptedValue)(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): String =
      secureGCMCipher.decrypt[String](value.value, value.nonce)
  }

  implicit val bigDecimalEncryptable: Encryptable[BigDecimal] = new Encryptable[BigDecimal] {
    def encrypt(value: BigDecimal)(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedValue = secureGCMCipher.encrypt(value)

    def decrypt(value: EncryptedValue)(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): BigDecimal =
      secureGCMCipher.decrypt[BigDecimal](value.value, value.nonce)
  }

  implicit val uuidEncryptable: Encryptable[UUID] = new Encryptable[UUID] {
    override def encrypt(value: UUID)(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedValue = secureGCMCipher.encrypt(value)

    override def decrypt(value: EncryptedValue)(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): UUID =
      secureGCMCipher.decrypt[UUID](value.value, value.nonce)
  }

  implicit val localDateEncryptable: Encryptable[LocalDate] = new Encryptable[LocalDate] {
    override def encrypt(value: LocalDate)(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedValue = secureGCMCipher.encrypt(value)

    override def decrypt(value: EncryptedValue)(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): LocalDate =
      secureGCMCipher.decrypt[LocalDate](value.value, value.nonce)
  }

  implicit val instantEncryptable: Encryptable[Instant] = new Encryptable[Instant] {
    override def encrypt(value: Instant)(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedValue = secureGCMCipher.encrypt(value)

    override def decrypt(value: EncryptedValue)(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): Instant =
      secureGCMCipher.decrypt[Instant](value.value, value.nonce)
  }
}

object EncryptableSyntax {

  implicit class EncryptableOps[A](value: A) {
    def encrypted(implicit e: Encryptable[A], secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedValue = e.encrypt(value)
  }

  implicit class DecryptableOps[A](encryptedValue: EncryptedValue) {
    def decrypted[A](implicit e: Encryptable[A], secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): A = e.decrypt(encryptedValue)
  }
}