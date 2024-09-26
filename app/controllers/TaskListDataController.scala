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

package controllers

import controllers.predicates.AuthorisedAction
import common.IncomeSources
import models.User
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result, Results}
import services.{RefreshCacheService, TaskListDataService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

case class TaskListDataController @Inject()(service: TaskListDataService,
                                            cc: ControllerComponents,
                                            cacheService: RefreshCacheService,
                                            authorisedAction: AuthorisedAction)
                                           (implicit ec: ExecutionContext) extends BackendController(cc) with Logging {

  def get(nino: String, taxYear: Int): Action[AnyContent] = authorisedAction.async { implicit user =>
    refreshIncomeSources(taxYear).flatMap { _ =>
      service.get(taxYear, nino)(hc.withExtraHeaders("mtditid" -> user.mtditid)).map {
        case Left(error) =>
          logger.info(s"[TaskListDataController][get] Error with status: ${error.status} and body: ${error.body}")
          Status(error.status)(error.toJson)
        case Right(data) => data match {
          case Some(value) => Ok(Json.toJson(value))
          case None => NotFound
        }
      }
    }
  }

  private def refreshIncomeSources(taxYear: Int)(implicit user: User[_]): Future[Unit] = {
    // TODO: Async updates with Future.sequence were not updating all sources which is why we chained them, could update
    //  individual CYA pages to do the cache call as done in the income sources we switched to mini journey
    cacheService.getLatestDataAndRefreshCache(taxYear, IncomeSources.GIFT_AID).map(_ =>
      cacheService.getLatestDataAndRefreshCache(taxYear, IncomeSources.CIS).map(_ =>
        cacheService.getLatestDataAndRefreshCache(taxYear, IncomeSources.EMPLOYMENT).map(_ =>
          cacheService.getLatestDataAndRefreshCache(taxYear, IncomeSources.INTEREST).map(_ =>
            cacheService.getLatestDataAndRefreshCache(taxYear, IncomeSources.STATE_BENEFITS)
          )
        )
      )
    )
  }
}
