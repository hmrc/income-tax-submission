
import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings

val appName = "income-tax-submission"

lazy val coverageSettings: Seq[Setting[_]] = {
  import scoverage.ScoverageKeys

  val excludedPackages = Seq(
    "<empty>",
    ".*Reverse.*",
    ".*standardError*.*",
    ".*govuk_wrapper*.*",
    ".*main_template*.*",
    "uk.gov.hmrc.BuildInfo",
    "app.*",
    "prod.*",
    "config.*",
    "testOnly.*",
    "testOnlyDoNotUseInAppConf.*",
    ".*feedback*.*",
    "partials.*",
    ".javascript"
  )

  Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(PlayKeys.playDefaultPort := 9304)
  .settings(
    majorVersion := 0,
    scalaVersion := "2.13.10",
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    // To resolve a bug with version 2.x.x of the scoverage plugin - https://github.com/sbt/sbt/issues/6997
    libraryDependencySchemes ++= Seq("org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always),
    scalacOptions += "-Wconf:src=routes/.*:s"
  )
  .configs(IntegrationTest extend Test)
  .settings(integrationTestSettings(): _*)
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(coverageSettings: _*)


