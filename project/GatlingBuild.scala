import sbt._
import sbt.Keys._

import BuildSettings._
import Dependencies._
import Bundle._

object GatlingBuild extends Build {

  override lazy val settings = super.settings ++ {
    shellPrompt := { state => Project.extract(state).currentProject.id + " > " }
  }

  /******************/
  /** Root project **/
  /******************/

  lazy val root = Project("gatling-parent", file("."))
    .aggregate(core, jdbc, redis, http, jms, charts, metrics, app, recorder, bundle)
    .settings(basicSettings: _*)
    .settings(noCodeToPublish: _*)
    .settings(docSettings: _*)

  /*************/
  /** Modules **/
  /*************/

  def gatlingModule(id: String) = Project(id, file(id)).settings(gatlingModuleSettings: _*)

  lazy val core = gatlingModule("gatling-core")
    .settings(libraryDependencies ++= coreDependencies(scalaVersion.value))

  lazy val jdbc = gatlingModule("gatling-jdbc")
    .dependsOn(core)

  lazy val redis = gatlingModule("gatling-redis")
    .dependsOn(core % "compile->compile;test->test")
    .settings(libraryDependencies ++= redisDependencies)

  lazy val http = gatlingModule("gatling-http")
    .dependsOn(core)
    .settings(libraryDependencies ++= httpDependencies)

  lazy val jms = gatlingModule("gatling-jms")
    .dependsOn(core % "test->test;compile->compile")
    .settings(libraryDependencies ++= jmsDependencies)

  lazy val charts = gatlingModule("gatling-charts")
    .dependsOn(core)
    .settings(libraryDependencies ++= chartsDependencies)
    .settings(excludeDummyComponentLibrary: _*)
    .settings(chartTestsSettings: _*)

  lazy val metrics = gatlingModule("gatling-metrics")
    .dependsOn(core)
    .settings(libraryDependencies ++= metricsDependencies)

  lazy val app = gatlingModule("gatling-app")
    .dependsOn(core, http, jms, jdbc, redis, metrics, charts)
    .settings(libraryDependencies ++= appDependencies)

  lazy val recorder = gatlingModule("gatling-recorder")
    .dependsOn(core, http)
    .settings(libraryDependencies ++= recorderDependencies(scalaVersion.value))

  lazy val bundle = gatlingModule("gatling-bundle")
    .settings(bundleSettings: _*)
    .settings(noCodeToPublish: _*)
}
