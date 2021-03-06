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

package models.mongo

import models.employment.frontend.AllEmploymentData
import models.giftAid.GiftAidModel
import models.{DividendsModel, IncomeSourcesResponseModel, InterestModel}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.{OFormat, OWrites, Reads, __}
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats

case class UserData(sessionId: String,
                    mtdItId: String,
                    nino: String,
                    taxYear: Int,
                    dividends: Option[DividendsModel] = None,
                    interest: Option[Seq[InterestModel]] = None,
                    giftAid: Option[GiftAidModel] = None,
                    employment: Option[AllEmploymentData] = None,
                    lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC)){

  def toIncomeSourcesResponseModel: IncomeSourcesResponseModel ={
    IncomeSourcesResponseModel(dividends,interest,giftAid,employment)
  }
}

object UserData {

  implicit lazy val formats: OFormat[UserData]  = OFormat(reads, writes)

  implicit lazy val reads: Reads[UserData] = {

    import play.api.libs.functional.syntax._
    (
      (__ \ "sessionId").read[String] and
        (__ \ "mtdItId").read[String] and
        (__ \ "nino").read[String] and
        (__ \ "taxYear").read[Int] and
        (__ \ "dividends").readNullable[DividendsModel] and
        (__ \ "interest").readNullable[Seq[InterestModel]] and
        (__ \ "giftAid").readNullable[GiftAidModel] and
        (__ \ "employment").readNullable[AllEmploymentData] and
          (__ \ "lastUpdated").read(MongoJodaFormats.dateTimeReads)
      ) (UserData.apply _)
  }

  implicit lazy val writes: OWrites[UserData] = {

    import play.api.libs.functional.syntax._
    (
      (__ \ "sessionId").write[String] and
        (__ \ "mtdItId").write[String] and
        (__ \ "nino").write[String] and
        (__ \ "taxYear").write[Int] and
        (__ \ "dividends").writeNullable[DividendsModel] and
        (__ \ "interest").writeNullable[Seq[InterestModel]] and
        (__ \ "giftAid").writeNullable[GiftAidModel] and
        (__ \ "employment").writeNullable[AllEmploymentData] and
        (__ \ "lastUpdated").write(MongoJodaFormats.dateTimeWrites)
      ) (unlift(UserData.unapply))
  }
}
