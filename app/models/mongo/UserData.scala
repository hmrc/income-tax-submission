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

import models._
import models.cis.{AllCISDeductions, EncryptedAllCISDeductions}
import models.employment.{AllEmploymentData, EncryptedAllEmploymentData}
import models.gifts.{EncryptedGiftAid, GiftAid}
import models.pensions.{EncryptedPensions, Pensions}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats

case class UserData(sessionId: String,
                    mtdItId: String,
                    nino: String,
                    taxYear: Int,
                    dividends: Option[Dividends] = None,
                    interest: Option[Seq[Interest]] = None,
                    giftAid: Option[GiftAid] = None,
                    employment: Option[AllEmploymentData] = None,
                    pensions: Option[Pensions] = None,
                    cis: Option[AllCISDeductions] = None,
                    lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC)) {

  def toIncomeSourcesResponseModel: IncomeSources = {
    IncomeSources(dividends, interest, giftAid, employment, pensions, cis)
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
                             dividends: Option[EncryptedDividends] = None,
                             interest: Option[Seq[EncryptedInterest]] = None,
                             giftAid: Option[EncryptedGiftAid] = None,
                             employment: Option[EncryptedAllEmploymentData] = None,
                             pensions: Option[EncryptedPensions] = None,
                             cis: Option[EncryptedAllCISDeductions] = None,
                             lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC))

object EncryptedUserData extends MongoJodaFormats {

  implicit val mongoJodaDateTimeFormats: Format[DateTime] = dateTimeFormat

  implicit lazy val formats: OFormat[EncryptedUserData] = Json.format[EncryptedUserData]
}
