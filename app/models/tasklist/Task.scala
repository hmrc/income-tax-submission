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

package models.tasklist

case class Task(sectionName:SectionName, sectionItems:List[TaskSectionItem])
case class Link(name:String,url:String)
case class TaskSectionItem(link:Link,status: TaskListStatus)

sealed trait SectionName
case object AboutYou extends SectionName
case object CharitableDonations extends SectionName
case object Employment extends SectionName
case object SelfEmployment extends SectionName
case object EmploymentAndSupportAllowance extends SectionName
case object JobseekersAllowance extends SectionName

object SectionName {
  private val sectionMap: Map[String, SectionName] = Map(
    "AboutYou"            -> AboutYou,
    "CharitableDonations" -> CharitableDonations,
    "Employment"          -> Employment,
    "SelfEmployment"      -> SelfEmployment,
    "EmploymentAndSupportAllowance" -> EmploymentAndSupportAllowance,
    "JobseekersAllowance" -> JobseekersAllowance
  )

  def fromString(value: String): Option[SectionName] = sectionMap.get(value)

  def toString(section: SectionName, isAgent:Boolean): String = {
    def readFromConfig(key:String)=
    {
      val derivedKey = if(isAgent) s"agent$key" else key
      //config.getKey(derivedKey)
      "changeme"
    }

    section match {
      case AboutYou => readFromConfig("aboutYouSectionKey")
      case CharitableDonations => readFromConfig("charitableDonationsSectionKey")
      case Employment => readFromConfig("employmentSectionKey")
      case SelfEmployment => readFromConfig("selfEmploymentSectionKey")
      case EmploymentAndSupportAllowance => readFromConfig("esaSectionKey")
      case JobseekersAllowance => readFromConfig("jsaSectionKey")
    }
  }
}

sealed trait TaskListStatus
case object NotStarted extends TaskListStatus
case object InProgress extends TaskListStatus
case object Completed extends TaskListStatus
case object Check extends TaskListStatus
case object UnderMaintenance extends TaskListStatus

object TaskListStatus{
  private val taskListStatusnMap: Map[String, TaskListStatus] = Map(
    "NotStarted"        -> NotStarted,
    "InProgress"        -> InProgress,
    "Completed"         -> Completed,
    "Check"             -> Check,
    "UnderMaintenance"  -> UnderMaintenance
  )

  def fromString(value: String): Option[TaskListStatus] = taskListStatusnMap.get(value)

  def toString(taskStatus: TaskListStatus): String = {
    taskStatus match {
      case NotStarted       => "NotStarted"
      case InProgress       => "InProgress"
      case Completed        => "Completed"
      case Check            => "Check"
      case UnderMaintenance => "UnderMaintenance"
    }
  }
}
