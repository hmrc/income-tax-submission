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

package services

import common.IncomeSources.{GIFT_AID, INTEREST}
import models.mongo.{DatabaseError, ExclusionUserDataModel}
import models.{APIErrorModel, ExcludeJourneyModel, User}
import repositories.ExclusionUserDataRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.ShaHashHelper

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ExcludeJourneyService @Inject()(
                                       exclusionUserDataRepository: ExclusionUserDataRepository,
                                       incomeSourcesService: GetIncomeSourcesService
                                     )(implicit ec: ExecutionContext) extends ShaHashHelper {

  def obtainHash(
                  taxYear: Int, journeyKey: String
                )(implicit user: User[_], hc: HeaderCarrier): Future[Either[APIErrorModel, Option[String]]] = {

    journeyKey match {
      case INTEREST =>
        incomeSourcesService.getInterest(user.nino, taxYear, user.mtditid).map {
          case Right(data) =>
            val accounts = data.getOrElse(Seq.empty).map(_.accountName).sorted
            Right(Some(sha256Hash(accounts.mkString(","))))
          case Left(error) =>
            Left(error)
        }
      case GIFT_AID =>
        incomeSourcesService.getGiftAid(user.nino, taxYear, user.mtditid).map {
          case Right(data) =>
            val nonUkInvestmentAccounts = data.flatMap(_.gifts.flatMap(_.investmentsNonUkCharitiesCharityNames)).getOrElse(Seq.empty)
            val nonUkAccounts = data.flatMap(_.giftAidPayments.flatMap(_.nonUkCharitiesCharityNames)).getOrElse(Seq.empty)
            val accounts = (nonUkInvestmentAccounts ++ nonUkAccounts).sorted

            Right(Some(sha256Hash(accounts.mkString(","))))
          case Left(error) => Left(error)
        }
      case _ => Future.successful(Right(None))
    }
  }

  def findExclusionData(taxYear: Int)(implicit user: User[_]): Future[Either[DatabaseError, Option[ExclusionUserDataModel]]] = {
    exclusionUserDataRepository.find(taxYear)
  }

  def createOrUpdate(
                      dataModel: ExcludeJourneyModel,
                      rootExclusionModel: ExclusionUserDataModel,
                      preExisting: Boolean
                    )(implicit user: User[_]): Future[Either[DatabaseError, Boolean]] = {

    val exclusionSeq = if (rootExclusionModel.exclusionModel.map(_.journey).contains(dataModel.journey)) {
      rootExclusionModel.exclusionModel
    } else {
      rootExclusionModel.exclusionModel :+ dataModel
    }
    val finalModel = rootExclusionModel.copy(exclusionModel = exclusionSeq)

    if (preExisting) {
      exclusionUserDataRepository.update(finalModel)
    } else {
      exclusionUserDataRepository.create(finalModel)
    }
  }

  def createOrUpdate(
                      exclusionModel: ExclusionUserDataModel,
                      preExisting: Boolean
                    )(implicit user: User[_]): Future[Either[DatabaseError, Boolean]] = {
    if (preExisting) {
      exclusionUserDataRepository.update(exclusionModel)
    } else {
      exclusionUserDataRepository.create(exclusionModel)
    }
  }

  def removeJourney(taxYear: Int, journeyKey: String)(implicit user: User[_]): Future[Either[DatabaseError, Boolean]] = {
    exclusionUserDataRepository.find(taxYear).flatMap {
      case Right(optionalData) =>
        optionalData match {
          case Some(data) =>
            val modifiedJourneys = data.exclusionModel.filterNot(_.journey == journeyKey)
            exclusionUserDataRepository.update(data.copy(exclusionModel = modifiedJourneys))
          case _ => Future.successful(Right(true))
        }
      case Left(error) => Future.successful(Left(error))
    }
  }

  def journeyKeyToModel(taxYear: Int, journeyKey: String)
                       (implicit user: User[_], hc: HeaderCarrier): Future[Either[APIErrorModel, ExcludeJourneyModel]] = {

    def createModel(hash: Either[APIErrorModel, Option[String]]): Either[APIErrorModel, ExcludeJourneyModel] = {
      hash match {
        case Right(potentialHash) => Right(ExcludeJourneyModel(journeyKey, potentialHash))
        case Left(error) => Left(error)
      }
    }

    obtainHash(taxYear, journeyKey).map(createModel)
  }

}
