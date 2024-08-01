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
import connectors.parsers.TaskListPensionDataParser.{TaskListPensionResponseModel, TaskListSectionResponseModel}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import models.{APIErrorBodyModel, APIErrorModel}
import models.tasklist.SectionTitle.{DividendsTitle, InsuranceGainsTitle, PensionsTitle}
import models.tasklist.TaskStatus.{CheckNow, NotStarted, UnderMaintenance}
import models.tasklist.{SectionTitle, TaskListModel, TaskListSection, TaskListSectionItem}
import play.api.http.Status.INTERNAL_SERVER_ERROR

import scala.collection.immutable.ListMap


class TaskListDataService @Inject()(
                                     connector: TaskListDataConnector,
                                     pensionTaskListDataConnector: PensionTaskListDataConnector,
                                     dividendsTaskListDataConnector: DividendsTaskListDataConnector,
                                     additionalInfoTaskListDataConnector: AdditionalInfoTaskListDataConnector,
                                     implicit val ec: ExecutionContext){

  private def getSectionItemMap(taskListModel: TaskListModel): ListMap[SectionTitle, Option[Seq[TaskListSectionItem]]] =
    ListMap.from(taskListModel.taskList.map(v => (v.sectionTitle, v.taskItems)))

  private def convertMapToSeq(dataMap: ListMap[SectionTitle, Option[Seq[TaskListSectionItem]]]): Seq[TaskListSection] =
    dataMap.map {
      case (sectionName, itemsOption) => TaskListSection(sectionName, Some(itemsOption.getOrElse(Seq.empty)))
    }.toSeq


  private def mergeSectionItemsWithCheckNow(tailoringItems: Seq[TaskListSectionItem], remoteItems: Seq[TaskListSectionItem]): Seq[TaskListSectionItem] = {
    val remoteItemsMap = remoteItems.map(item => item.title -> item).toMap

    val combinedItems = tailoringItems.map { item =>
      remoteItemsMap.get(item.title) match {
        case Some(remoteItem) => remoteItem // If remote item exists, take the remote item
        case None => item.copy(status = NotStarted) // If not, take the tailoring item and set status to NotStarted
      }
    }

    val remoteOnlyItems = remoteItems.filterNot(item => tailoringItems.exists(_.title == item.title)).map(_.copy(status = CheckNow))

    combinedItems ++ remoteOnlyItems
  }

  /**
   * Below merge is performed on assumption that `check now` status in handled in downstream
   */
//    private def mergeSectionItems(tailoringItems: Seq[TaskListSectionItem], remoteItems: Seq[TaskListSectionItem]): Seq[TaskListSectionItem] = {
//      val tailoringOnlyItems = tailoringItems.filterNot(item => remoteItems.exists(_.title == item.title)).map(_.copy(status = NotStarted))
//      //This should get all the status including check now
//      val remoteOnlyItems = remoteItems.filterNot(item => tailoringItems.exists(_.title == item.title))
//
//      remoteOnlyItems ++ tailoringOnlyItems
//    }
  private def handleTaskListSectionResponse(
                                             sectionTitleToMerge: SectionTitle,
                                             tailoringTaskListModel: TaskListModel,
                                             otherServiceTaskList: Future[TaskListSectionResponseModel]
                                           ): Future[TaskListModel] = {

    val tailoringSectionItemsMap: ListMap[SectionTitle, Option[Seq[TaskListSectionItem]]] = getSectionItemMap(tailoringTaskListModel)
    val tailoringSectionItems: Seq[TaskListSectionItem] = tailoringSectionItemsMap.getOrElse(sectionTitleToMerge, Some(Seq.empty)).getOrElse(Seq.empty)

    otherServiceTaskList.map {
      case Right(Some(remoteSection)) => {
        val remoteSectionItems: Seq[TaskListSectionItem] = remoteSection.taskItems.getOrElse(Seq.empty)
        val mergedSectionItems = Some(mergeSectionItemsWithCheckNow(tailoringSectionItems, remoteSectionItems))
        val updatedSectionsList = convertMapToSeq(tailoringSectionItemsMap.updated(sectionTitleToMerge, mergedSectionItems))
        TaskListModel(updatedSectionsList)
      }
      case Right(None) => tailoringTaskListModel
      case Left(error) =>
        val underMaintenanceItems = tailoringSectionItems.map(_.copy(status = UnderMaintenance,href = None))
        val updatedSectionsList = convertMapToSeq(tailoringSectionItemsMap.updated(sectionTitleToMerge, Some(underMaintenanceItems)))
        TaskListModel(updatedSectionsList)
    }.recover {
      case ex: Throwable =>
        val underMaintenanceItems = tailoringSectionItems.map(_.copy(status = UnderMaintenance,href = None))
        val updatedSectionsList = convertMapToSeq(tailoringSectionItemsMap.updated(sectionTitleToMerge, Some(underMaintenanceItems)))
        TaskListModel(updatedSectionsList)
    }
  }

  /**
   * TODO Once pension service is brought inline to return only sections and sectionitems, we can remove this function.
   * This is created to support different model being returned by pension
   */

  private def handleTaskListModelResponse(
                                           sectionTitleToMerge: SectionTitle,
                                           tailoringTaskListModel: TaskListModel,
                                           otherServiceTaskList: Future[TaskListPensionResponseModel]
                                         ): Future[TaskListModel] = {

    val tailoringSectionItemsMap = getSectionItemMap(tailoringTaskListModel)
    val tailoringSectionItems = tailoringSectionItemsMap.getOrElse(sectionTitleToMerge, Some(Seq.empty)).getOrElse(Seq.empty)

    otherServiceTaskList.flatMap{
      case Right(Some(remoteModel)) =>

        val remoteSectionItems: Seq[TaskListSectionItem] =
          remoteModel.taskList.find(_.sectionTitle == sectionTitleToMerge).flatMap(_.taskItems).getOrElse(Seq.empty)
        val mergedSectionItems = Some(mergeSectionItemsWithCheckNow(tailoringSectionItems, remoteSectionItems))
        val updatedSectionsList = convertMapToSeq(tailoringSectionItemsMap.updated(sectionTitleToMerge, mergedSectionItems))
        Future.successful(TaskListModel(updatedSectionsList))

      case Right(None) => Future.successful(tailoringTaskListModel)
      case Left(error) =>
        val underMaintenanceItems: Seq[TaskListSectionItem] = tailoringSectionItems.map(_.copy(status = UnderMaintenance,href = None))
        val updatedSectionsList = convertMapToSeq(tailoringSectionItemsMap.updated(sectionTitleToMerge, Some(underMaintenanceItems)))
        Future.successful(TaskListModel(updatedSectionsList))

    }.recover {
      case ex: Throwable =>
        val underMaintenanceItems = tailoringSectionItems.map(_.copy(status = UnderMaintenance,href = None))
        val updatedSectionsList = convertMapToSeq(tailoringSectionItemsMap.updated(sectionTitleToMerge, Some(underMaintenanceItems)))
        TaskListModel(updatedSectionsList)
    }
  }


  def get(taxYear: Int, nino: String, mtdItId: String)(implicit hc: HeaderCarrier): Future[Either[APIErrorModel, Option[TaskListModel]]] = {
    //TODO move "mtditid" as a constant across the repo in separate PR
    val enhancedHC = hc.withExtraHeaders(("mtditid", mtdItId))
    val tailoringTaskListResponse = connector.get(taxYear)(enhancedHC)
    val pensionTaskListResponse = pensionTaskListDataConnector.get(taxYear, nino)(enhancedHC)
    val dividendsTaskListResponse = dividendsTaskListDataConnector.get(taxYear, nino)(enhancedHC)
    val additionalInfoTaskListResponse = additionalInfoTaskListDataConnector.get(taxYear, nino)(enhancedHC)

    tailoringTaskListResponse.flatMap {
      case Right(Some(tailoringData)) =>
        for {
          mergedWithPensions <- handleTaskListModelResponse(PensionsTitle, tailoringData, pensionTaskListResponse)
          mergedWithDividends <- handleTaskListSectionResponse(DividendsTitle, mergedWithPensions, dividendsTaskListResponse)
          finalMerged <- handleTaskListSectionResponse(InsuranceGainsTitle, mergedWithDividends, additionalInfoTaskListResponse)
        } yield Right(Some(finalMerged))
      case Right(None) =>
        Future.successful(Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INVALID STATE", "Tailoring task list data cannot be empty"))))
      case Left(error) =>
        Future.successful(Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INVALID STATE", "Failed to retrieve tailoring task list data"))))
    }
  }
}
