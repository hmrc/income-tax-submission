/*
 * Copyright 2024 HM Revenue & Customs
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

package models.logging

import play.api.mvc.{Request, RequestHeader}

import java.util.UUID

object CorrelationId {
  val CorrelationIdHeaderKey = "CorrelationId" // This header is used in IFS/DES. We keep the same naming in our services too for simplicity

  private def getOrGenerateCorrelationId(requestHeader: RequestHeader): String = requestHeader.headers
    .get(CorrelationIdHeaderKey)
    .getOrElse(CorrelationId.generate())

  implicit class RequestHeaderOps(val value: RequestHeader) extends AnyVal {
    def withCorrelationId(): (RequestHeader, String) = {
      val correlationId = getOrGenerateCorrelationId(value)
      val updatedRequest = value.withHeaders(value.headers.replace(CorrelationIdHeaderKey -> correlationId))

      (updatedRequest, correlationId)
    }

  }

  object RequestOps {
    def withCorrelationId[A](value: Request[A]): (Request[A], String) = {
      val correlationId = getOrGenerateCorrelationId(value)
      val updatedRequest = value.withHeaders(value.headers.replace(CorrelationIdHeaderKey -> correlationId))

      (updatedRequest, correlationId)
    }
  }

  private def generate(): String = UUID.randomUUID().toString
}
