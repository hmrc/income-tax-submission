/*
 * Copyright 2020 HM Revenue & Customs
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

package models
import play.api.libs.json.{JsPath, Writes}
import play.api.libs.json._
import play.api.libs.functional.syntax._

sealed class ErrorResponse(val httpStatusCode: Int, val errorCode: String, val errorMsg: String)

object ErrorResponse {

  implicit val writes: Writes[ErrorResponse] =  (
    (JsPath \ "code").write[String] and
    (JsPath \ "message").write[String]
  ) (error => (error.errorCode, error.errorMsg))
}

case object InternalServerError extends ErrorResponse(
  500, "INTERNAL_SERVER_ERROR", "A downstream service didn't response as expected"
)

case object ServiceUnavailableError extends ErrorResponse(
  503, "SERVICE_UNAVAILABLE", "The service is temporarily unavailable"
)

case object NotFoundError extends ErrorResponse(
  404, "NOT_FOUND", "The data requested could not be found"
)
