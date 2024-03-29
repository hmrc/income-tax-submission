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

package models.otheremployment

import play.api.libs.json._

sealed abstract class ShareAwardedShareSchemePlanType

object ShareAwardedShareSchemePlanType {

  case object SIP extends ShareAwardedShareSchemePlanType
  case object Other extends ShareAwardedShareSchemePlanType

  def fromString(str: String): Option[ShareAwardedShareSchemePlanType] = str match {
    case "SIP" => Some(SIP)
    case "Other" => Some(Other)
    case _ => None
  }

  implicit val format: Format[ShareAwardedShareSchemePlanType] = new Format[ShareAwardedShareSchemePlanType] {
    def writes(schemePlanType: ShareAwardedShareSchemePlanType): JsValue = schemePlanType match {
      case SIP => JsString("SIP")
      case Other => JsString("Other")
    }

    def reads(json: JsValue): JsResult[ShareAwardedShareSchemePlanType] = json match {
      case JsString("SIP") => JsSuccess(SIP)
      case JsString("Other") => JsSuccess(Other)
      case other => JsError(s"Invalid ShareAwardedShareSchemePlanType: $other")
    }
  }
}
