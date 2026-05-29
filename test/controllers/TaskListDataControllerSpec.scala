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

package controllers

import connectors.parsers.TaskListTailoringDataParser.TaskListResponseModel
import models._
import models.tasklist._
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.{Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, status}
import services.{RefreshCacheService, TaskListDataService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{TaxYearUtils, TestUtils}

import scala.concurrent.{ExecutionContext, Future}

class TaskListDataControllerSpec extends TestUtils {

  private val mockTaskListDataService: TaskListDataService = mock[TaskListDataService]
  private val mockRefreshCacheService: RefreshCacheService = mock[RefreshCacheService]

  def mockGetTaskListData(data: Either[APIErrorModel, Option[TaskListModel]]): Unit = {
    when(mockTaskListDataService.get(eqTo(taxYear), eqTo(nino))(any[HeaderCarrier]))
      .thenReturn(Future.successful(data))
  }

  def mockRefreshCache(): Unit = {
    when(mockRefreshCacheService.getLatestDataAndRefreshCache(eqTo(taxYear), any[String])(anyUser, any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.successful(Results.Ok))
  }

  private val taskListData: TaskListResponseModel = Right(Some(TaskListModel(List[TaskListSection](
    TaskListSection(
      sectionTitle = SectionTitle.AboutYouTitle,
      taskItems = Some(List[TaskListSectionItem](
        TaskListSectionItem(TaskTitle.UkResidenceStatus, status = TaskStatus.Completed, Some("url"))))
    )
  ))))

  private val controller: TaskListDataController =
    TaskListDataController(mockTaskListDataService, mockControllerComponents, mockRefreshCacheService, authorisedAction)
  private val mtdItId: String = "1234567890"
  private val taxYear: Int = TaxYearUtils.taxYear
  private val nino: String = "AA123456A"
  private val fakeGetRequest = FakeRequest("GET",
    s"/income-tax-submission-service/income-tax/task-list/$taxYear").withHeaders("mtditid" -> mtdItId, "sessionId" -> "sessionId")


  "calling .get" should {

    "return an OK response with data" in {
      val result = {
        mockAuth()
        mockRefreshCache()
        mockGetTaskListData(taskListData)
        controller.get(nino, taxYear)(fakeGetRequest)
      }
      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(taskListData.toOption.get)
    }

    "return a NO_CONTENT response with no data" in {
      val result = {
        mockAuth()
        mockRefreshCache()
        mockGetTaskListData(Right(None))
        controller.get(nino, taxYear)(fakeGetRequest)
      }
      status(result) mustBe NOT_FOUND
    }

    "return an SERVICE_UNAVAILABLE response with error model" in {
      val result = {
        mockAuth()
        mockRefreshCache()
        mockGetTaskListData(Left(APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel("NOT_GOOD", "something went wrong"))))
        controller.get(nino, taxYear)(fakeGetRequest)
      }
      status(result) mustBe SERVICE_UNAVAILABLE
      contentAsJson(result) mustBe APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel("NOT_GOOD", "something went wrong")).toJson
    }
  }
}
