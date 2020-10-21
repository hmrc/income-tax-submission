
/*
 * Copyright 2018 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlMatching}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status._
import play.api.libs.json.{JsObject, Json}

trait AuthStub extends {

  private val authoriseUri: String = "/auth/authorise"
  private val AGENT_ENROLMENT_KEY = "HMRC-AS-AGENT"

  val successITSAEnrolment: JsObject = Json.obj(
    "key" -> "HMRC-MTD-IT",
    "identifiers" -> Json.arr(
      Json.obj(
        "key" -> "MTDITID",
        "value" -> "555555555"
      )
    )
  )

  val otherEnrolment: JsObject = Json.obj(
    "key" -> "HMRC-OTHER-ENROLMENT",
    "identifiers" -> Json.arr(
      Json.obj(
        "key" -> "OTHERID",
        "value" -> "555555555"
      )
    )
  )
  private val agentEnrolment = Json.obj(
    "key" -> AGENT_ENROLMENT_KEY,
    "identifiers" -> Json.arr(
      Json.obj(
        "key" -> "AgentReferenceNumber",
        "value" -> "1234567890"
      )
    )
  )

  def authorised(response: JsObject = successfulAuthResponse("Individual", successITSAEnrolment)): StubMapping = {
    stubFor(post(urlMatching(authoriseUri))
      .willReturn(
        aResponse()
          .withStatus(OK)
          .withBody(response.toString())
          .withHeader("Content-Type", "application/json; charset=utf-8")))
  }

  def agentAuthorised(): StubMapping = {
    stubFor(post(urlMatching(authoriseUri))
      .willReturn(
        aResponse()
          .withStatus(OK)
          .withBody(successfulAuthResponse("Agent", agentEnrolment).toString())
          .withHeader("Content-Type", "application/json; charset=utf-8")))
  }

  def unauthorisedOtherEnrolment(): StubMapping = {
    stubFor(post(urlMatching(authoriseUri))
      .willReturn(
        aResponse()
          .withStatus(OK)
          .withBody(successfulAuthResponse("Individual", otherEnrolment).toString())
          .withHeader("Content-Type", "application/json; charset=utf-8")))
  }

  def insufficientEnrolments(): StubMapping = {
    stubFor(post(urlMatching(authoriseUri))
      .willReturn(
        aResponse()
          .withStatus(UNAUTHORIZED)
          .withBody(successfulAuthResponse("Agent", agentEnrolment).toString())
          .withHeader("WWW-Authenticate", """MDTP detail="InsufficientEnrolments""")
      ))
  }

  private def successfulAuthResponse(affinityGroup: String, enrolments: JsObject*): JsObject = {
    Json.obj("allEnrolments" -> enrolments, "affinityGroup" -> affinityGroup)
  }

  def partialsAuthResponse(enrolments: JsObject*): JsObject = {
    Json.obj("authorisedEnrolments" -> enrolments)
  }
}