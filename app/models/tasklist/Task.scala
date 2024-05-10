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

  def toString(enum: SectionName,isAgent:Boolean): String = {
    def readFromConfig(key:String:String =
    {
      val derivedKey = if(isAgent) s"agent$key" else key
      //config.getKey(derivedKey)
      "changeme"
    }

    enum match {
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

  def toString(enum: TaskListStatus): String = {
    enum match {
      case NotStarted       => "NotStarted"
      case InProgress       => "InProgress"
      case Completed        => "Completed"
      case Check            => "Check"
      case UnderMaintenance => "UnderMaintenance"
    }
  }
}
