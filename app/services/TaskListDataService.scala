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
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import connectors.parsers.TaskListTailoringDataParser.TaskListResponseModel
import models.{APIErrorBodyModel, APIErrorModel}
import models.tasklist.SectionTitle.PensionsTitle
import models.tasklist.{SectionTitle, TaskListModel, TaskListSection, TaskListSectionItem, TaskStatus}
import play.api.http.Status.INTERNAL_SERVER_ERROR



class TaskListDataService @Inject()(
                                     connector: TaskListDataConnector,
                                     pensionTaskListDataConnector: PensionTaskListDataConnector,
                                     implicit val ec: ExecutionContext) {

  private def getSectionItemMap(taskListModel: TaskListModel): Map[String, Option[Seq[TaskListSectionItem]]] =
    taskListModel.taskList.map(v => (v.sectionTitle, v.taskItems)).toMap

  private def convertMapToSeq(dataMap: Map[String, Option[Seq[TaskListSectionItem]]]): Seq[TaskListSection] = {
    dataMap.map { case (sectionName, itemsOption) =>
      TaskListSection(sectionName, Some(itemsOption.getOrElse(Seq.empty)))
    }.toSeq
  }

  def checkAndMergeResponse(sectionTitleToMerge: SectionTitle, tailoringTaskListModel: TaskListModel,
                            otherServiceTaskList: Future[TaskListResponseModel]): Future[Either[APIErrorModel, Option[TaskListModel]]] = {

    val tailoringSectionItemsMap: Map[String, Option[Seq[TaskListSectionItem]]] = getSectionItemMap(tailoringTaskListModel)

    val tailoringSectionItems: Seq[TaskListSectionItem] =
      tailoringSectionItemsMap.get(sectionTitleToMerge.toString).flatMap(identity).getOrElse(Seq.empty)

    otherServiceTaskList.map {
      case Right(remoteModel) =>
        remoteModel match {
          case Some(x) =>
            val remoteTaskListSectionItems: Seq[TaskListSectionItem] =
              getSectionItemMap(x).get(sectionTitleToMerge.toString).flatMap(identity).getOrElse(Seq.empty)

            val commonItems = tailoringSectionItems.intersect(remoteTaskListSectionItems)
            val tailoringOnlyItems = tailoringSectionItems.diff(remoteTaskListSectionItems).map(_.copy(status = TaskStatus("Not Started")))
            //TODO change status on review, revisit this
            val remoteOnlyItems = remoteTaskListSectionItems.diff(tailoringSectionItems).map(_.copy(status = TaskStatus("Check now")))

            val mergedSectionItems: Option[Seq[TaskListSectionItem]] = Some(remoteOnlyItems ++ tailoringOnlyItems ++ commonItems)

            val updatedSectionsList: Seq[TaskListSection] =
              convertMapToSeq(tailoringSectionItemsMap.updated(sectionTitleToMerge.toString, mergedSectionItems))
val result = TaskListModel(updatedSectionsList)
            println(s"1111 - check and merge result $result")
            Right(Some(result))
          case None => Right(Some(tailoringTaskListModel))
        }
      case Left(_) =>
        // In case of error, respective section should be displayed as Under Maintenance
        val sectionWithUnderMaintenance: Option[Seq[TaskListSectionItem]] = Some(tailoringSectionItems.map(_.copy(status = TaskStatus("Under Maintenance"))))
        val updatedSectionsList: Seq[TaskListSection] =
          convertMapToSeq(tailoringSectionItemsMap.updated(sectionTitleToMerge.toString, sectionWithUnderMaintenance))
        val result = TaskListModel(updatedSectionsList)
        println(s"1111 - Left result $result")
        Right(Some(result))
    }.recover {
      case ex: Throwable =>
        println(s"1111 - error $ex")
        Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INVALID STATE", "Failed to merge task list data")))
    }
  }


  def get(taxYear: Int, nino: String, mtdItId: String)(implicit hc: HeaderCarrier): Future[Either[APIErrorModel, Option[TaskListModel]]] = {
    val tailoringTaskListResponse: Future[TaskListResponseModel] = connector.get(taxYear)(hc.withExtraHeaders(("mtditid", mtdItId)))
    val pensionTaskListResponse: Future[TaskListResponseModel] = pensionTaskListDataConnector.get(taxYear, nino)(hc.withExtraHeaders(("mtditid", mtdItId)))
println(s"1111- pension data $pensionTaskListResponse")
    tailoringTaskListResponse.flatMap {
      case Right(Some(tailoringData)) =>
        checkAndMergeResponse(PensionsTitle, tailoringData, pensionTaskListResponse)
      case Right(None) =>
        Future.successful(Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INVALID STATE", "Tailoring task list data cannot be empty"))))
      case Left(error) =>
        Future.successful(Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INVALID STATE", "Failed to retrieve tailoring task list data"))))
    }
  }

}
