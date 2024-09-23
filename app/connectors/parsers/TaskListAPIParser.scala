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

package connectors.parsers

import connectors.parsers.TaskListTailoringDataParser.TaskListResponseModel
import models.APIErrorModel
import models.tasklist.{TaskListModel, TaskListSection}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.PagerDutyHelper.PagerDutyKeys.UNEXPECTED_RESPONSE_FROM_API
import utils.PagerDutyHelper.pagerDutyLog
import play.api.http.Status._
import utils.PagerDutyHelper.PagerDutyKeys._

trait TaskListAPIParser extends APIParser {

  //This is for tailoring [TaskListResponseModel]
  implicit object TaskListHttpReads extends HttpReads[TaskListResponseModel] {
    override def read(method: String, url: String, response: HttpResponse): TaskListResponseModel = {
      response.status match {
        case OK =>
          response.json.validate[TaskListModel].fold(
            _ => badSuccessJsonFromAPI,
            model => Right(Some(model))
          )
        case NOT_FOUND =>
          Right(None)
        case BAD_REQUEST | UNPROCESSABLE_ENTITY | FORBIDDEN =>
          pagerDutyLog(FOURXX_RESPONSE_FROM_API, logMessage(response))
          handleAPIError(response)
        case INTERNAL_SERVER_ERROR =>
          pagerDutyLog(INTERNAL_SERVER_ERROR_FROM_API, logMessage(response))
          handleAPIError(response)
        case SERVICE_UNAVAILABLE =>
          pagerDutyLog(SERVICE_UNAVAILABLE_FROM_API, logMessage(response))
          handleAPIError(response)
        case _ =>
          pagerDutyLog(UNEXPECTED_RESPONSE_FROM_API, logMessage(response))
          handleAPIError(response, Some(INTERNAL_SERVER_ERROR))
      }
    }
  }

  //This is for Pension, StateBenefits(ESA , JSA) Seq[TaskListSection]
  type SeqOfTaskListSection = Either[APIErrorModel, Option[Seq[TaskListSection]]]
  implicit object SeqOfTaskListSectionHttpReads extends HttpReads[SeqOfTaskListSection] {

    override def read(method: String, url: String, response: HttpResponse): SeqOfTaskListSection = {
      response.status match {
        case OK =>
          response.json.validate[Seq[TaskListSection]].fold(
            _ => badSuccessJsonFromAPI,
            model => Right(Some(model))
          )
        case NOT_FOUND =>
          Right(None)
        case BAD_REQUEST | UNPROCESSABLE_ENTITY | FORBIDDEN =>
          pagerDutyLog(FOURXX_RESPONSE_FROM_API, logMessage(response))
          handleAPIError(response)
        case INTERNAL_SERVER_ERROR =>
          pagerDutyLog(INTERNAL_SERVER_ERROR_FROM_API, logMessage(response))
          handleAPIError(response)
        case SERVICE_UNAVAILABLE =>
          pagerDutyLog(SERVICE_UNAVAILABLE_FROM_API, logMessage(response))
          handleAPIError(response)
        case _ =>
          pagerDutyLog(UNEXPECTED_RESPONSE_FROM_API, logMessage(response))
          handleAPIError(response, Some(INTERNAL_SERVER_ERROR))
      }
    }
  }

  //This is for most of services Option[TaskListSection]
  type TaskListSectionResponseModel = Either[APIErrorModel, Option[TaskListSection]]
  implicit object TaskListSectionHttpReads extends HttpReads[TaskListSectionResponseModel] {

    override def read(method: String, url: String, response: HttpResponse): TaskListSectionResponseModel = {
      println(response.body)
      response.status match {
        case OK =>
          response.json.validate[TaskListSection].fold(
            _ => badSuccessJsonFromAPI,
            model => Right(Some(model))
          )
        case NOT_FOUND =>
          Right(None)
        case BAD_REQUEST | UNPROCESSABLE_ENTITY | FORBIDDEN =>
          pagerDutyLog(FOURXX_RESPONSE_FROM_API, logMessage(response))
          handleAPIError(response)
        case INTERNAL_SERVER_ERROR =>
          pagerDutyLog(INTERNAL_SERVER_ERROR_FROM_API, logMessage(response))
          handleAPIError(response)
        case SERVICE_UNAVAILABLE =>
          pagerDutyLog(SERVICE_UNAVAILABLE_FROM_API, logMessage(response))
          handleAPIError(response)
        case _ =>
          pagerDutyLog(UNEXPECTED_RESPONSE_FROM_API, logMessage(response))
          handleAPIError(response, Some(INTERNAL_SERVER_ERROR))
      }
    }
  }

}
