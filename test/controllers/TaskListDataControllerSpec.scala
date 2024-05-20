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

import connectors.parsers.TaskListDataParser.TaskListResponseModel
import models._
import models.tasklist.{TaskListModel, TaskListSection, TaskListSectionItem, TaskStatus, TaskTitle}
import org.scalamock.handlers.CallHandler3
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import services.TaskListDataService
import uk.gov.hmrc.http.HeaderCarrier
import utils.{TaxYearUtils, TestUtils}

import scala.concurrent.Future

class TaskListDataControllerSpec extends TestUtils {

  private val mockTaskListDataService: TaskListDataService = mock[TaskListDataService]

  def mockGetTaskListData(data: Either[APIErrorModel, Option[TaskListModel]]):
  CallHandler3[Int, String, HeaderCarrier, Future[TaskListResponseModel]] = {
    (mockTaskListDataService.get(_: Int, _: String)(_: HeaderCarrier))
      .expects(*, *, *)
      .returning(Future.successful(data))
  }

  private val taskListData: TaskListResponseModel = Right(Some(TaskListModel(List[TaskListSection](
    TaskListSection(
      sectionTitle = "AboutYou",
      taskItems = Some(List[TaskListSectionItem](
        TaskListSectionItem(TaskTitle(content = "UK Residence Status"), status = TaskStatus("Completed"), Some("url"))))
    )
  ))))

  private val controller: TaskListDataController = TaskListDataController(mockTaskListDataService, mockControllerComponents, authorisedAction)
  private val mtdItId: String = "1234567890"
  private val taxYear: Int = TaxYearUtils.taxYear
  private val fakeGetRequest = FakeRequest("GET",
    s"/income-tax-submission-service/income-tax/task-list/$taxYear").withHeaders("mtditid" -> mtdItId, "sessionId" -> "sessionId")


  "calling .get" should {

    "return an OK response with data" in {
      val result = {
        mockAuth()
        mockGetTaskListData(taskListData)
        controller.get(taxYear)(fakeGetRequest)
      }
      status(result) mustBe OK
      Json.parse(bodyOf(result)) mustBe Json.toJson(taskListData.toOption.get)
    }

    "return a NO_CONTENT response with no data" in {
      val result = {
        mockAuth()
        mockGetTaskListData(Right(None))
        controller.get(taxYear)(fakeGetRequest)
      }
      status(result) mustBe NOT_FOUND
    }

    "return an SERVICE_UNAVAILABLE response with error model" in {
      val result = {
        mockAuth()
        mockGetTaskListData(Left(APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel("NOT_GOOD", "something went wrong"))))
        controller.get(taxYear)(fakeGetRequest)
      }
      status(result) mustBe SERVICE_UNAVAILABLE
      Json.parse(bodyOf(result)) mustBe APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel("NOT_GOOD", "something went wrong")).toJson
    }
  }
}
