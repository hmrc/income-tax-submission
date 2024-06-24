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

package services

import connectors._
import models.tasklist.{SectionTitle, TaskListModel, TaskListSection, TaskListSectionItem, TaskStatus, TaskTitle}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{TaxYearUtils, TestUtils}

import scala.concurrent.Future

class TaskListDataServiceSpec extends TestUtils {

  private val taxYear = TaxYearUtils.taxYear

  private val taskListDataConnector: TaskListDataConnector = mock[TaskListDataConnector]
  private val mtdItId: String = "1234567890"
  private val mockHeaderCarrier: HeaderCarrier = emptyHeaderCarrier.withExtraHeaders(("mtditid", mtdItId))

  private val underTest: TaskListDataService = new TaskListDataService(
    taskListDataConnector,
    scala.concurrent.ExecutionContext.global
  )

  private val taskListResult = Some(TaskListModel(List[TaskListSection](
    TaskListSection(
      sectionTitle = SectionTitle.AboutYouTitle,
      taskItems = Some(List[TaskListSectionItem](
        TaskListSectionItem(TaskTitle.UkResidenceStatus, status = TaskStatus.Completed, Some("url"))))
    )
  )))

  ".get" when {
    "data exists" should {
      "return response with task list model" in {

        (taskListDataConnector.get(_: Int)(_: HeaderCarrier))
          .expects(taxYear, mockHeaderCarrier)
          .returning(Future.successful(Right(taskListResult)))

        val result = underTest.get(taxYear, mtdItId)

        await(result) mustBe Right(taskListResult)
      }
    }
  }
}
