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

import models._
import utils.PagerDutyHelper.PagerDutyKeys._
import utils.PagerDutyHelper.pagerDutyLog
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND, OK, SERVICE_UNAVAILABLE}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}


object SubmittedDividendsParser {
  type IncomeSourcesResponseModel = Either[ErrorResponseModel, Option[SubmittedDividendsModel]]

  implicit object SubmittedDividendsHttpReads extends HttpReads[IncomeSourcesResponseModel] {
    override def read(method: String, url: String, response: HttpResponse): IncomeSourcesResponseModel = {
      response.status match {
        case OK =>
          response.json.validate[SubmittedDividendsModel].fold[IncomeSourcesResponseModel](
          _ =>  {
            pagerDutyLog(BAD_SUCCESS_JSON_FROM_API, Some(s"[SubmittedDividendsParser][read] Invalid Json from API."))
            Left(ErrorResponseModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError))
          },
          {
            case SubmittedDividendsModel(None, None) => Right(None)
            case parsedModel => Right(Some(parsedModel))
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
          pagerDutyLog(UNEXPECTED_RESPONSE_FROM_API, logMessage(response))
          handleError(response, Some(INTERNAL_SERVER_ERROR))
      }
    }
  }

  private def handleError(response: HttpResponse, statusOverride: Option[Int] = None): IncomeSourcesResponseModel = {

    val status = statusOverride.getOrElse(response.status)

    try {
      response.json.validate[ErrorBodyModel].fold[IncomeSourcesResponseModel](

        jsonErrors => {
          pagerDutyLog(UNEXPECTED_RESPONSE_FROM_API, Some(s"[SubmittedDividendsParser][read] Unexpected Json from API."))
          Left(ErrorResponseModel(status, ErrorBodyModel.parsingError))
        },
        parsedModel => Left(ErrorResponseModel(status, parsedModel)))
    } catch {
      case _: Exception => Left(ErrorResponseModel(status, ErrorBodyModel.parsingError))
    }
  }

  private def logMessage(response:HttpResponse): Option[String] ={
    Some(s"[SubmittedDividendsParser][read] Received ${response.status} from income-tax-dividends. Body:${response.body}")
  }
}
