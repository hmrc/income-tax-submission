/*
 * Copyright 2022 HM Revenue & Customs
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

import models.APIErrorModel
import models.pensions.Pensions
import play.api.Logging
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.PagerDutyHelper.PagerDutyKeys.{FOURXX_RESPONSE_FROM_API, INTERNAL_SERVER_ERROR_FROM_API, SERVICE_UNAVAILABLE_FROM_API, UNEXPECTED_RESPONSE_FROM_API}
import utils.PagerDutyHelper.pagerDutyLog

object SubmittedPensionsParser extends APIParser with Logging {
  type IncomeSourcesResponseModel = Either[APIErrorModel, Option[Pensions]]

  override val parserName: String = "SubmittedPensionsParser"
  override val service: String = "income-tax-pensions"

  implicit object SubmittedPensionsHttpReads extends HttpReads[IncomeSourcesResponseModel] {
    override def read(method: String, url: String, response: HttpResponse): IncomeSourcesResponseModel = {
      response.status match {
        case OK =>
          response.json.validate[Pensions].fold[IncomeSourcesResponseModel](
            _ => badSuccessJsonFromAPI,
            {
              case Pensions(None, None, None, None, None) => Right(None)
              case parsedModel => Right(Some(parsedModel))
            }
          )
        case NO_CONTENT =>
          logger.info(logMessage(response))
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
