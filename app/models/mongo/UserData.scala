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

package models.mongo

import models.employment.frontend.{AllEmploymentData, EncryptedAllEmploymentData}
import models.giftAid.{EncryptedGiftAidModel, GiftAidModel}
import models.{DividendsModel, EncryptedDividendsModel, EncryptedInterestModel, IncomeSourcesResponseModel, InterestModel}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats

case class UserData(sessionId: String,
                    mtdItId: String,
                    nino: String,
                    taxYear: Int,
                    dividends: Option[DividendsModel] = None,
                    interest: Option[Seq[InterestModel]] = None,
                    giftAid: Option[GiftAidModel] = None,
                    employment: Option[AllEmploymentData] = None,
                    lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC)) {

  def toIncomeSourcesResponseModel: IncomeSourcesResponseModel = {
    IncomeSourcesResponseModel(dividends, interest, giftAid, employment)
  }
}

object UserData extends MongoJodaFormats {

  implicit val mongoJodaDateTimeFormats: Format[DateTime] = dateTimeFormat

  implicit lazy val formats: OFormat[UserData] = Json.format[UserData]
}

case class EncryptedUserData(sessionId: String,
                             mtdItId: String,
                             nino: String,
                             taxYear: Int,
                             dividends: Option[EncryptedDividendsModel] = None,
                             interest: Option[Seq[EncryptedInterestModel]] = None,
                             giftAid: Option[EncryptedGiftAidModel] = None,
                             employment: Option[EncryptedAllEmploymentData] = None,
                             lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC))

object EncryptedUserData extends MongoJodaFormats {

  implicit val mongoJodaDateTimeFormats: Format[DateTime] = dateTimeFormat

  implicit lazy val formats: OFormat[EncryptedUserData] = Json.format[EncryptedUserData]
}
