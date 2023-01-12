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

package connectors

import builders.models.statebenefits.AllStateBenefitsDataBuilder.anAllStateBenefitsData
import com.github.tomakehurst.wiremock.http.HttpHeader
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status.{BAD_REQUEST, NO_CONTENT, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import utils.{ConnectorIntegrationTest, MockAppConfig}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class IncomeTaxStateBenefitsConnectorISpec extends ConnectorIntegrationTest {

  private val mtditid = "some-mtditid"
  private val sessionId = "some-sessionId"
  private val nino = "some-nino"
  private val taxYear = 2022

  implicit private val headerCarrier: HeaderCarrier = HeaderCarrier().withExtraHeaders("mtditid" -> mtditid, "X-Session-ID" -> sessionId)

  private val underTest = new IncomeTaxStateBenefitsConnector(httpClient, new MockAppConfig())

  "IncomeTaxStateBenefitsConnector" should {
    val headers = Seq(new HttpHeader("X-Session-ID", sessionId), new HttpHeader("mtditid", mtditid))

    "return AllStateBenefitsData when successful result" in {
      val responseBody = Json.toJson(anAllStateBenefitsData).toString()

      stubGetWithResponseBody(s"/income-tax-state-benefits/income-tax/nino/$nino/tax-year/$taxYear", OK, responseBody, headers)

      Await.result(underTest.getSubmittedStateBenefits(nino, taxYear), Duration.Inf) shouldBe Right(Some(anAllStateBenefitsData))
    }

    "return None when no content" in {
      stubGetWithResponseBody(s"/income-tax-state-benefits/income-tax/nino/$nino/tax-year/$taxYear", NO_CONTENT, "{}", headers)

      Await.result(underTest.getSubmittedStateBenefits(nino, taxYear), Duration.Inf) shouldBe Right(None)
    }

    "return error when BAD_REQUEST" in {
      val errorBody: APIErrorBodyModel = APIErrorBodyModel("BAD_REQUEST", "That request was bad")
      val expectedResult = APIErrorModel(BAD_REQUEST, errorBody)

      stubGetWithResponseBody(s"/income-tax-state-benefits/income-tax/nino/$nino/tax-year/$taxYear", BAD_REQUEST, Json.toJson(errorBody).toString(), headers)

      Await.result(underTest.getSubmittedStateBenefits(nino, taxYear), Duration.Inf) shouldBe Left(expectedResult)
    }
  }
}
