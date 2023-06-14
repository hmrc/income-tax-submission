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

import models.APIErrorModel
import models.employment.OtherEmploymentIncome
import play.api.Logging
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.PagerDutyHelper.PagerDutyKeys._
import utils.PagerDutyHelper.pagerDutyLog

object OtherEmploymentIncomeParser extends APIParser with Logging {
  type OtherEmploymentIncomeResponseModel = Either[APIErrorModel, Option[OtherEmploymentIncome]]

  override val parserName: String = "OtherEmploymentIncomeParser"
  override val service: String = "income-tax-employment"

  implicit object OtherEmploymentIncomeHttpReads extends HttpReads[OtherEmploymentIncomeResponseModel] {
    override def read(method: String, url: String, response: HttpResponse): OtherEmploymentIncomeResponseModel = {
      response.status match {
        case OK                                 =>
          response.json.validate[OtherEmploymentIncome].fold[OtherEmploymentIncomeResponseModel](
            _ => badSuccessJsonFromAPI,
            {
              case otherEmploymentsModel => Right(Some(otherEmploymentsModel))
              case _                     => Right(None)
            }
          )
        case NO_CONTENT                         =>
          logger.info(logMessage(response))
          Right(None)
        case BAD_REQUEST | UNPROCESSABLE_ENTITY =>
          pagerDutyLog(FOURXX_RESPONSE_FROM_API, logMessage(response))
          handleAPIError(response)
        case INTERNAL_SERVER_ERROR              =>
          pagerDutyLog(INTERNAL_SERVER_ERROR_FROM_API, logMessage(response))
          handleAPIError(response)
        case SERVICE_UNAVAILABLE                =>
          pagerDutyLog(SERVICE_UNAVAILABLE_FROM_API, logMessage(response))
          handleAPIError(response)
        case _                                  =>
          pagerDutyLog(UNEXPECTED_RESPONSE_FROM_API, logMessage(response))
          handleAPIError(response, Some(INTERNAL_SERVER_ERROR))
      }
    }
  }
}
