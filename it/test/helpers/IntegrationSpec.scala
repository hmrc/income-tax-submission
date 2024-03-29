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

package helpers

import com.codahale.metrics.SharedMetricRegistries
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.HeaderNames
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

import scala.concurrent.ExecutionContext

trait IntegrationSpec extends AnyWordSpec
  with Matchers
  with BeforeAndAfterEach
  with BeforeAndAfterAll
  with GuiceOneServerPerSuite
  with FutureAwaits
  with DefaultAwaitTimeout
  with WireMockHelper
  with WiremockStubHelpers
  with AuthStub {

  lazy val ws: WSClient = app.injector.instanceOf(classOf[WSClient])
  implicit val ec: ExecutionContext = ExecutionContext.global

  lazy val connectedServices: Seq[String] = Seq(
    "income-tax-dividends",
    "income-tax-employment",
    "income-tax-interest",
    "income-tax-gift-aid",
    "auth",
    "income-tax-pensions",
    "income-tax-cis",
    "income-tax-state-benefits",
    "income-tax-additional-information"
  )

  def servicesToUrlConfig: Seq[(String, String)] = connectedServices
    .flatMap(service => Seq(s"microservice.services.$service.host" -> s"localhost", s"microservice.services.$service.port" -> wiremockPort.toString))

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .configure(("useEncryption" -> true) +: ("auditing.consumer.baseUri.port" -> wiremockPort) +: servicesToUrlConfig: _*)
    .build()

  lazy val appWithInvalidEncryptionKey: Application = GuiceApplicationBuilder()
    .configure(("useEncryption" -> true) +: ("mongodb.encryption.key" -> "key") +: ("auditing.consumer.baseUri.port" -> wiremockPort) +: servicesToUrlConfig: _*)
    .build()

  override def beforeAll(): Unit = {
    super.beforeAll()
    wireMockServer.start()
    SharedMetricRegistries.clear()
    WireMock.configureFor("localhost", wiremockPort)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    wireMockServer.stop()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset()
  }

  def buildClient(urlandUri: String,
                  port: Int = port,
                  additionalCookies: Map[String, String] = Map.empty): WSRequest = {

    ws
      .url(s"http://localhost:$port$urlandUri")
      .withHttpHeaders(HeaderNames.COOKIE -> PlaySessionCookieBaker.bakeSessionCookie(additionalCookies), "Csrf-Token" -> "nocheck")
      .withFollowRedirects(false)
  }
}
