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

import config.AppConfig
import connectors._
import connectors.parsers.TaskListCISDataParser.SeqOfTaskListSection
import connectors.parsers.TaskListPensionDataParser.TaskListSectionResponseModel
import models.tasklist.SectionTitle._
import models.tasklist.TaskStatus.{NotStarted, UnderMaintenance}
import models.tasklist.{SectionTitle, TaskListModel, TaskListSection, TaskListSectionItem}
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.collection.immutable.ListMap
import scala.concurrent.{ExecutionContext, Future}


class TaskListDataService @Inject()(connector: TaskListDataConnector,
                                    pensionTaskListDataConnector: PensionTaskListDataConnector,
                                    dividendsTaskListDataConnector: DividendsTaskListDataConnector,
                                    additionalInfoTaskListDataConnector: AdditionalInfoTaskListDataConnector,
                                    charitableDonationsTaskListDataConnector: CharitableDonationsTaskListDataConnector,
                                    interestTaskListDataConnector: InterestTaskListDataConnector,
                                    cisTaskListDataConnector: CISTaskListDataConnector,
                                    seTaskListDataConnector: SelfEmploymentTaskListDataConnector,
                                    stateBenefitsConnector: StateBenefitsTaskListDataConnector,
                                    employmentTaskListDataConnector: EmploymentTaskListDataConnector,
                                    appConfig: AppConfig
                                   )
                                   (implicit val ec: ExecutionContext) {

  private def getSectionItemMap(taskListModel: TaskListModel): ListMap[SectionTitle, Option[Seq[TaskListSectionItem]]] =
    ListMap.from(taskListModel.taskList.map(v => (v.sectionTitle, v.taskItems)))

  private def convertMapToSeq(dataMap: ListMap[SectionTitle, Option[Seq[TaskListSectionItem]]]): Seq[TaskListSection] =
    dataMap.map {
      case (sectionName, itemsOption) => TaskListSection(sectionName, itemsOption)
    }.toSeq

  private def mergeSectionItemsWithCheckNow(tailoringItems: Seq[TaskListSectionItem], remoteItems: Seq[TaskListSectionItem]): Seq[TaskListSectionItem] = {
    val remoteItemsMap = remoteItems.map(item => item.title -> item).toMap

    val combinedItems = tailoringItems.map { item =>
      remoteItemsMap.get(item.title) match {
        case Some(remoteItem) => remoteItem // If remote item exists, take the remote item
        case None => item.copy(status = NotStarted) // If not, take the tailoring item and set status to NotStarted
      }
    }

    val remoteOnlyItems = remoteItems.filterNot(item => tailoringItems.exists(_.title == item.title)).map(_.copy(status = NotStarted))

    combinedItems ++ remoteOnlyItems
  }

  private def mergeSections(sectionTitleToMerge: SectionTitle,
                            tailoringTaskListModel: TaskListModel,
                            otherServiceTaskList: Future[TaskListSectionResponseModel]): Future[TaskListModel] = {

    val tailoringSectionItemsMap: ListMap[SectionTitle, Option[Seq[TaskListSectionItem]]] = getSectionItemMap(tailoringTaskListModel)
    val tailoringSectionItems: Seq[TaskListSectionItem] = tailoringSectionItemsMap.getOrElse(sectionTitleToMerge, Some(Seq.empty)).getOrElse(Seq.empty)

    otherServiceTaskList.map {
      case Right(Some(remoteSection)) =>
        val remoteSectionItems: Seq[TaskListSectionItem] = remoteSection.taskItems.getOrElse(Seq.empty)
        val mergedSectionItems = Some(mergeSectionItemsWithCheckNow(tailoringSectionItems, remoteSectionItems))
        val updatedSectionsList = convertMapToSeq(tailoringSectionItemsMap.updated(sectionTitleToMerge, mergedSectionItems))
        TaskListModel(updatedSectionsList.filter(_.taskItems.getOrElse(Seq.empty).nonEmpty))
      case Right(None) => tailoringTaskListModel
      case Left(_) =>
        val underMaintenanceItems = tailoringSectionItems.map(_.copy(status = UnderMaintenance, href = None))
        val updatedSectionsList = convertMapToSeq(tailoringSectionItemsMap.updated(sectionTitleToMerge, Some(underMaintenanceItems)))
        TaskListModel(updatedSectionsList)
    }.recover {
      case _: Throwable =>
        val underMaintenanceItems = tailoringSectionItems.map(_.copy(status = UnderMaintenance, href = None))
        val updatedSectionsList = convertMapToSeq(tailoringSectionItemsMap.updated(sectionTitleToMerge, Some(underMaintenanceItems)))
        TaskListModel(updatedSectionsList)
    }
  }

  private def extractSectionByTitle(
                                     allSections: Future[SeqOfTaskListSection],
                                     sectionTitle: SectionTitle
                                   ): Future[TaskListSectionResponseModel] =
    allSections.map {
      case Right(value) => Right(value.map((t: Seq[TaskListSection]) => t.filter(_.sectionTitle == sectionTitle).head))
      case Left(_) => Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INVALID STATE", "Failed to retrieve tailoring task list data")))
    }

  def safeFutureCall[T](future: () => Future[Either[APIErrorModel, T]], context: String): Future[Either[APIErrorModel, T]] = {
    future().recover {
      case _ =>
        Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("ERROR", s"Downstream service call $context failed")))
    }
  }

  def get(taxYear: Int, nino: String)(implicit hc: HeaderCarrier): Future[Either[APIErrorModel, Option[TaskListModel]]] = {
    val allPensionTaskList: Future[SeqOfTaskListSection] = safeFutureCall(() => pensionTaskListDataConnector.get(taxYear, nino), "Pension")
    val pensionsTaskList = safeFutureCall(() => extractSectionByTitle(allPensionTaskList, PensionsTitle), "PensionTitle")
    val paymentIntoPensionTaskList = safeFutureCall(() => extractSectionByTitle(allPensionTaskList, PaymentsIntoPensionsTitle), "PaymentIntoPensionTaskList")


    val tailoringTaskList: Future[Either[APIErrorModel, Option[TaskListModel]]] = safeFutureCall(() => connector.get(taxYear), "Tailoring")
    val dividendsTaskList = safeFutureCall(() => dividendsTaskListDataConnector.get(taxYear, nino), "Dividend")
    val additionalInfoTaskList = safeFutureCall(() => additionalInfoTaskListDataConnector.get(taxYear, nino), "AdditionalInfo")
    val charitableDonationsTaskList = safeFutureCall(() => charitableDonationsTaskListDataConnector.get(taxYear, nino), "Gift aid")
    val interestTaskList: Future[Either[APIErrorModel, Option[TaskListSection]]] = safeFutureCall(() => interestTaskListDataConnector.get(taxYear, nino), "Interest")

    val selfEmploymentTaskList = if(appConfig.selfEmploymentTaskListEnabled) {
      getSETaskList(taxYear, nino)
    } else {
      safeFutureCall(() => cisTaskListDataConnector.get(taxYear, nino),"CIS")
    }

    val stateBenefitTaskList: Future[SeqOfTaskListSection] = safeFutureCall(() => stateBenefitsConnector.get(taxYear, nino), "StateBenefit")
    val esaTaskList = safeFutureCall(() => extractSectionByTitle(stateBenefitTaskList, EsaTitle), "Esa")
    val jsaTaskList = safeFutureCall(() => extractSectionByTitle(stateBenefitTaskList, JsaTitle), "Jsa")
    val employmentTaskList = safeFutureCall(() => employmentTaskListDataConnector.get(taxYear, nino), "Employment")

    tailoringTaskList.flatMap {
      case Right(Some(tailoringData)) =>
        for {
          mergedPensions <- mergeSections(PensionsTitle, tailoringData, pensionsTaskList)
          mergedPaymentIntoPensions <- mergeSections(PaymentsIntoPensionsTitle, mergedPensions, paymentIntoPensionTaskList)
          mergedDividends <- mergeSections(DividendsTitle, mergedPaymentIntoPensions, dividendsTaskList)
          mergedCharitableDonations <- mergeSections(CharitableDonationsTitle, mergedDividends, charitableDonationsTaskList)
          mergedInterest <- mergeSections(InterestTitle, mergedCharitableDonations, interestTaskList)
          mergedSEAndCIS <- mergeSections(SelfEmploymentTitle, mergedInterest, selfEmploymentTaskList)
          mergedESA <- mergeSections(EsaTitle, mergedSEAndCIS, esaTaskList)
          mergedJSA <- mergeSections(JsaTitle, mergedESA, jsaTaskList)
          mergedEmployment <- mergeSections(EmploymentTitle, mergedJSA, employmentTaskList)
          finalMerged <- mergeSections(InsuranceGainsTitle, mergedEmployment, additionalInfoTaskList)
        } yield Right(Some(finalMerged))
      case Right(None) =>
        Future.successful(Left(APIErrorModel(NOT_FOUND, APIErrorBodyModel("NOT_FOUND", "Tailoring task list data is not found"))))
      case Left(_) =>
        Future.successful(Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INVALID STATE", "Failed to retrieve tailoring task list data"))))
    }
  }

  private def getSETaskList(taxYear: Int, nino: String)(implicit hc: HeaderCarrier): Future[Either[APIErrorModel, Option[TaskListSection]]] = {
    val result = for {
      seResponse <- seTaskListDataConnector.get(taxYear, nino)
      cisResponse <- cisTaskListDataConnector.get(taxYear, nino)
    } yield {
      for {
        seTaskList <- seResponse
        cisTaskList <- cisResponse
      } yield (seTaskList, cisTaskList) match {
        case (Some(a), Some(b)) => Some(TaskListSection(SelfEmploymentTitle, getSeTaskLists(a, b)))
        case (Some(a), None) => Some(a)
        case (None, Some(b)) => Some(b)
        case (None, None) => None
      }
    }

    safeFutureCall(() => result, "Self employment or CIS")
  }

  private def getSeTaskLists(se: TaskListSection, cis: TaskListSection): Option[Seq[TaskListSectionItem]] = {
    for {
      seTaskList <- se.taskItems
      cisTaskList <- cis.taskItems
    } yield seTaskList ++ cisTaskList
  }
}
