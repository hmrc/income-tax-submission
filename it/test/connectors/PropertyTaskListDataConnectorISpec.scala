/*
 * Copyright 2025 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.http.HttpHeader
import models.tasklist.TaskTitle.{ForeignProperty, UkForeignProperty, UkProperty}
import models.tasklist.{SectionTitle, TaskListSection, TaskListSectionItem, TaskStatus}
import models.{APIErrorBodyModel, APIErrorModel, APIErrorsBodyModel}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, SessionId}
import utils.{ConnectorIntegrationTest, MockAppConfig}

import java.time.{LocalDate, ZoneId}
import scala.concurrent.ExecutionContext.Implicits.global

class PropertyTaskListDataConnectorISpec extends ConnectorIntegrationTest {
  private val taxYear = LocalDate.now(ZoneId.systemDefault()).getYear
  private val mtdItIdHeader = ("mtditid", "1234567890")
  private val requestHeaders = Seq(new HttpHeader("mtditid", "1234567890"))
  val nino :String = "123456789"

  private val connector: PropertyTaskListDataConnector = new PropertyTaskListDataConnector(httpClientV2, new MockAppConfig())
  def propertyTaskListDataUrl: String = s"/income-tax-property/$taxYear/tasks/$nino"

  "PropertyTaskListDataConnector" should {

    val expectedResult: Seq[TaskListSection] =
      Seq(
        TaskListSection(
          sectionTitle = SectionTitle.UkPropertyTitle,
          taskItems = Some(List[TaskListSectionItem](
            TaskListSectionItem(UkProperty, status = TaskStatus.NotStarted, Some("url"))
          ))
        ),
        TaskListSection(
          sectionTitle = SectionTitle.ForeignPropertyTitle,
          taskItems = Some(List[TaskListSectionItem](
            TaskListSectionItem(ForeignProperty, status = TaskStatus.NotStarted, Some("url"))
          ))
        ),
        TaskListSection(
          sectionTitle = SectionTitle.UkForeignPropertyTitle,
          taskItems = Some(List[TaskListSectionItem](
            TaskListSectionItem(UkForeignProperty, status = TaskStatus.Completed, Some("url"))
          ))
        )
      )

    val responseBody = Json.toJson(expectedResult).toString()
    implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue"))).withExtraHeaders(mtdItIdHeader)

    "include internal headers" when {
      val headersSentToPropertyTaskList = Seq(new HttpHeader(HeaderNames.xSessionId, "sessionIdValue"))

      "the host is 'Internal'" in {
        stubGetWithResponseBody(propertyTaskListDataUrl, OK, responseBody, headersSentToPropertyTaskList)

        connector.get(taxYear,nino).futureValue shouldBe Right(Some(expectedResult))
      }

      "the host is 'External'" in {
        stubGetWithResponseBody(propertyTaskListDataUrl, OK, responseBody, headersSentToPropertyTaskList)

        connector.get(taxYear,nino).futureValue shouldBe Right(Some(expectedResult))
      }
    }

    "return a None for not found" in {
      stubGetWithResponseBody(propertyTaskListDataUrl, NOT_FOUND, "{}", requestHeaders)
      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtdItIdHeader)

      connector.get(taxYear,nino).futureValue shouldBe Right(None)
    }

    "API Returns multiple errors" in {
      val expectedErrorResult = APIErrorModel(BAD_REQUEST, APIErrorsBodyModel(Seq(
        APIErrorBodyModel("INVALID_IDTYPE", "ID is invalid"),
        APIErrorBodyModel("INVALID_IDTYPE_2", "ID 2 is invalid")
      )))

      val responseBody = Json.obj("failures" -> Json.arr(
        Json.obj("code" -> "INVALID_IDTYPE", "reason" -> "ID is invalid"),
        Json.obj("code" -> "INVALID_IDTYPE_2", "reason" -> "ID 2 is invalid")
      ))
      stubGetWithResponseBody(propertyTaskListDataUrl, BAD_REQUEST, responseBody.toString(), requestHeaders)

      connector.get(taxYear,nino).futureValue shouldBe Left(expectedErrorResult)
    }

    "return a BadRequest" in {
      val errorBody: APIErrorBodyModel = APIErrorBodyModel("BAD_REQUEST", "That request was bad")
      val expectedResult = APIErrorModel(BAD_REQUEST, errorBody)

      stubGetWithResponseBody(propertyTaskListDataUrl,
        BAD_REQUEST, Json.toJson(errorBody).toString(), requestHeaders)

      connector.get(taxYear,nino).futureValue shouldBe Left(expectedResult)
    }

    "return an InternalServerError " in {
      val errorBody: APIErrorBodyModel = APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong")
      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, errorBody)

      stubGetWithResponseBody(propertyTaskListDataUrl,
        INTERNAL_SERVER_ERROR, Json.toJson(errorBody).toString(), requestHeaders)

      connector.get(taxYear,nino).futureValue shouldBe Left(expectedResult)
    }

    "return an InternalServerError with parsing error when we can't parse the error body" in {
      val errorResponseBody = Json.toJson("INTERNAL_SERVER_ERROR")
      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)

      stubGetWithResponseBody(propertyTaskListDataUrl,
        INTERNAL_SERVER_ERROR, errorResponseBody.toString(), requestHeaders)

      connector.get(taxYear,nino).futureValue shouldBe Left(expectedResult)
    }

    "return an InternalServerError when an unexpected status is thrown" in {
      val errorResponseBody = Json.toJson(APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong"))
      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Something went wrong"))

      stubGetWithResponseBody(propertyTaskListDataUrl, IM_A_TEAPOT, errorResponseBody.toString(), requestHeaders)
      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtdItIdHeader)

      connector.get(taxYear,nino).futureValue shouldBe Left(expectedResult)
    }

    "return an InternalServerError when an unexpected status is thrown and there is no body" in {
      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)

      stubGetWithoutResponseBody(propertyTaskListDataUrl, IM_A_TEAPOT)
      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtdItIdHeader)

      connector.get(taxYear,nino).futureValue shouldBe Left(expectedResult)
    }

    "return a ServiceUnavailableError" in {
      val errorRequestBody = Json.toJson(APIErrorBodyModel("SERVICE_UNAVAILABLE", "Service went down")).toString()
      val expectedResult = APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel("SERVICE_UNAVAILABLE", "Service went down"))

      stubGetWithResponseBody(propertyTaskListDataUrl, SERVICE_UNAVAILABLE, errorRequestBody, requestHeaders)
      implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders(mtdItIdHeader)

      connector.get(taxYear,nino).futureValue shouldBe Left(expectedResult)
    }
  }
}

