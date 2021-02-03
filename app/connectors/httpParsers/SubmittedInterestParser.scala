/*
 * Copyright 2021 HM Revenue & Customs
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

package connectors.httpParsers

import utils.PagerDutyHelper.PagerDutyKeys._
import utils.PagerDutyHelper.pagerDutyLog
import models.{ErrorResponseModel, _}
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object SubmittedInterestParser {
  type IncomeSourcesResponseModel = Either[ErrorResponseModel, Option[List[SubmittedInterestModel]]]

  implicit object SubmittedInterestHttpReads extends HttpReads[IncomeSourcesResponseModel] {
    override def read(method: String, url: String, response: HttpResponse): IncomeSourcesResponseModel = {
      response.status match {
        case OK =>
          response.json.validate[List[SubmittedInterestModel]].fold[IncomeSourcesResponseModel](
          _ => Left(ErrorResponseModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError)),
          {
            case parsedModel if parsedModel.nonEmpty => Right(Some(parsedModel))
            case _ => Right(None)
          }
        )
        case NOT_FOUND => Right(None)
        case BAD_REQUEST =>
          pagerDutyLog(BAD_REQUEST_FROM_API, logMessage(response))
          handleError(response)
        case INTERNAL_SERVER_ERROR =>
          pagerDutyLog(INTERNAL_SERVER_ERROR_FROM_API, logMessage(response))
          handleError(response)
        case SERVICE_UNAVAILABLE =>
          pagerDutyLog(SERVICE_UNAVAILABLE_FROM_API, logMessage(response))
          handleError(response)
        case _ =>
          pagerDutyLog(INTERNAL_SERVER_ERROR_FROM_API, logMessage(response))
          handleError(response, Some(INTERNAL_SERVER_ERROR))
      }
    }
  }

  private def handleError(response: HttpResponse, statusOverride: Option[Int] = None): IncomeSourcesResponseModel = {

    val status = statusOverride.getOrElse(response.status)

    try {
      response.json.validate[ErrorBodyModel].fold[IncomeSourcesResponseModel](

        jsonErrors => {
          Left(ErrorResponseModel(status, ErrorBodyModel.parsingError))
        },
        parsedModel => Left(ErrorResponseModel(status, parsedModel)))
    } catch {
      case _: Exception => Left(ErrorResponseModel(status, ErrorBodyModel.parsingError))
    }
  }

  private def logMessage(response:HttpResponse): Option[String] ={
    Some(s"[SubmittedInterestParser][read] Received ${response.status} from income-tax-interest. Body:${response.body}")
  }
}
