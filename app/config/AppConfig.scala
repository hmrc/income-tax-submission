/*
 * Copyright 2021 HM Revenue & Customs
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

package config

import com.google.inject.ImplementedBy

import javax.inject.Inject
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.duration.Duration

class BackendAppConfig @Inject()(config: Configuration, servicesConfig: ServicesConfig) extends AppConfig {

  val authBaseUrl: String = servicesConfig.baseUrl("auth")

  val dividendsBaseUrl: String = servicesConfig.baseUrl("income-tax-dividends")
  val interestBaseUrl: String = servicesConfig.baseUrl("income-tax-interest")
  val employmentBaseUrl: String = servicesConfig.baseUrl("income-tax-employment")
  val giftAidBaseUrl: String = servicesConfig.baseUrl("income-tax-gift-aid")

  val auditingEnabled: Boolean = config.get[Boolean]("auditing.enabled")
  val graphiteHost: String = config.get[String]("microservice.metrics.graphite.host")

  //Mongo config
  lazy val encryptionKey: String = servicesConfig.getString("mongodb.encryption.key")
  lazy val mongoTTL: Int = Duration(servicesConfig.getString("mongodb.timeToLive")).toMinutes.toInt

  lazy val useEncryption: Boolean = config.get[Boolean]("useEncryption")

}

@ImplementedBy(classOf[BackendAppConfig])
trait AppConfig{
  val authBaseUrl: String

  val dividendsBaseUrl: String
  val interestBaseUrl: String
  val employmentBaseUrl: String
  val giftAidBaseUrl: String

  val auditingEnabled: Boolean
  val graphiteHost: String

  //Mongo config
  val encryptionKey: String
  val mongoTTL: Int

  val useEncryption: Boolean
}
