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

import config.AppConfig
import org.scalamock.scalatest.MockFactory

class MockAppConfig(isEncrypted: Boolean = true) extends AppConfig with MockFactory {

  private val host: String = "http://localhost:11111"

  override val authBaseUrl: String = "/auth"

  override val dividendsBaseUrl: String = host
  override val interestBaseUrl: String = host
  override val employmentBaseUrl: String = host
  override val giftAidBaseUrl: String = host
  override val pensionsBaseUrl: String = host
  override val cisBaseUrl: String = host
  override val seBaseUrl: String = host
  override val stateBenefitsBaseUrl: String = host
  override val additionalInfoBaseUrl: String = host
  override val tailoringPhaseIIBaseUrl: String = host
  override val propertyBaseUrl: String = host


  override val auditingEnabled: Boolean = true
  override val selfEmploymentTaskListEnabled: Boolean = true

  override lazy val encryptionKey: String = "encryptionKey12345"
  override lazy val mongoTTL: Int = 1550

  override lazy val useEncryption: Boolean = isEncrypted
}
