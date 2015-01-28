import sbt._
import sbt.Keys._

import BuildSettings._
import Bundle._
import Dependencies._
import ConfigFiles._
import CopyLogback._
import VersionFile._

object GatlingBuild extends Build {

  override lazy val settings = super.settings ++ {
    shellPrompt := { state => Project.extract(state).currentProject.id + " > " }
  }

  /******************/
  /** Root project **/
  /******************/

  lazy val root = Project("gatling-parent", file("."))
    .dependsOn(Seq(core, http, jms, jdbc, redis).map(_ % "compile->compile;test->test"): _*)
    .aggregate(core, jdbc, redis, http, jms, charts, metrics, app, recorder, testFramework, bundle, compiler)
    .settings(basicSettings: _*)
    .settings(noCodeToPublish: _*)
    .settings(docSettings: _*)
    .settings(libraryDependencies ++= docDependencies)

  /*************/
  /** Modules **/
  /*************/

  def gatlingModule(id: String) = Project(id, file(id)).settings(gatlingModuleSettings: _*)

  lazy val core = gatlingModule("gatling-core")
    .settings(libraryDependencies ++= coreDependencies(scalaVersion.value))
    .settings(generateVersionFileSettings: _*)
    .settings(generateConfigFileSettings(bundle): _*)
    .settings(copyLogbackXmlSettings(bundle): _*)
    .settings(copyGatlingDefaults(compiler): _*)

  lazy val jdbc = gatlingModule("gatling-jdbc")
    .dependsOn(core)
    .settings(libraryDependencies ++= jdbcDependencies)

  lazy val redis = gatlingModule("gatling-redis")
    .dependsOn(core % "compile->compile;test->test")
    .settings(libraryDependencies ++= redisDependencies)

  lazy val http = gatlingModule("gatling-http")
    .dependsOn(core % "compile->compile;test->test")
    .settings(libraryDependencies ++= httpDependencies)

  lazy val jms = gatlingModule("gatling-jms")
    .dependsOn(core % "compile->compile;test->test")
    .settings(libraryDependencies ++= jmsDependencies)

  lazy val charts = gatlingModule("gatling-charts")
    .dependsOn(core)
    .settings(libraryDependencies ++= chartsDependencies)
    .settings(excludeDummyComponentLibrary: _*)
    .settings(chartTestsSettings: _*)

  lazy val metrics = gatlingModule("gatling-metrics")
    .dependsOn(core % "compile->compile;test->test")
    .settings(libraryDependencies ++= metricsDependencies)

  lazy val compiler = gatlingModule("gatling-compiler")
    .settings(scalaVersion := "2.10.4")
    .settings(libraryDependencies ++= compilerDependencies(scalaVersion.value))

  lazy val app = gatlingModule("gatling-app")
    .dependsOn(core, http, jms, jdbc, redis, metrics, charts)

  lazy val recorder = gatlingModule("gatling-recorder")
    .dependsOn(core, http)
    .settings(libraryDependencies ++= recorderDependencies)
    .settings(generateConfigFileSettings(bundle): _*)

  lazy val testFramework = gatlingModule("gatling-test-framework")
    .dependsOn(app)
    .settings(libraryDependencies ++= testFrameworkDependencies)

  lazy val bundle = gatlingModule("gatling-bundle")
    .settings(bundleSettings: _*)
    .settings(noCodeToPublish: _*)
}
