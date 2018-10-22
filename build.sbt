import BuildSettings._
import Bundle._
import ConfigFiles._
import CopyLogback._
import Dependencies._
import VersionFile._
import sbt._

// Root project

lazy val root = Project("gatling-parent", file("."))
  .enablePlugins(AutomateHeaderPlugin, SonatypeReleasePlugin, SphinxPlugin)
  .dependsOn(Seq(commons, core, http, jms, jdbc, redis).map(_ % "compile->compile;test->test"): _*)
  .aggregate(nettyUtil, commons, core, jdbc, redis, httpClient, http, jms, charts, graphite, app, recorder, testFramework, bundle, compiler)
  .settings(basicSettings: _*)
  .settings(noArtifactToPublish)
  .settings(libraryDependencies ++= docDependencies)
  .settings(updateOptions := updateOptions.value.withGigahorse(false))
  .settings(unmanagedSourceDirectories in Test := ((sourceDirectory in Sphinx).value ** "code").get)

// Modules

def gatlingModule(id: String) = Project(id, file(id))
  .enablePlugins(AutomateHeaderPlugin, SonatypeReleasePlugin)
  .settings(gatlingModuleSettings: _*)
  .settings(updateOptions := updateOptions.value.withGigahorse(false))

lazy val nettyUtil = gatlingModule("gatling-netty-util")
  .settings(libraryDependencies ++= nettyUtilDependencies)

lazy val commons = gatlingModule("gatling-commons")
  .dependsOn(nettyUtil % "compile->compile;test->test")
  .settings(libraryDependencies ++= commonsDependencies(scalaVersion.value))
  .settings(generateVersionFileSettings: _*)

lazy val core = gatlingModule("gatling-core")
  .dependsOn(commons % "compile->compile;test->test")
  .settings(libraryDependencies ++= coreDependencies)
  .settings(copyGatlingDefaults(compiler): _*)

lazy val jdbc = gatlingModule("gatling-jdbc")
  .dependsOn(core % "compile->compile;test->test")
  .settings(libraryDependencies ++= jdbcDependencies)

lazy val redis = gatlingModule("gatling-redis")
  .dependsOn(core % "compile->compile;test->test")
  .settings(libraryDependencies ++= redisDependencies)

lazy val httpClient = gatlingModule("gatling-http-client")
  .dependsOn(nettyUtil % "compile->compile;test->test")
  .settings(libraryDependencies ++= httpClientDependencies)

lazy val http = gatlingModule("gatling-http")
  .dependsOn(
    core % "compile->compile;test->test",
    httpClient % "compile->compile;test->test")
  .settings(libraryDependencies ++= httpDependencies)

lazy val jms = gatlingModule("gatling-jms")
  .dependsOn(core % "compile->compile;test->test")
  .settings(libraryDependencies ++= jmsDependencies)
  .settings(parallelExecution in Test := false)

lazy val charts = gatlingModule("gatling-charts")
  .dependsOn(core % "compile->compile;test->test")
  .settings(libraryDependencies ++= chartsDependencies)
  .settings(excludeDummyComponentLibrary: _*)
  .settings(chartTestsSettings: _*)

lazy val graphite = gatlingModule("gatling-graphite")
  .dependsOn(core % "compile->compile;test->test")
  .settings(libraryDependencies ++= graphiteDependencies)

lazy val compiler = gatlingModule("gatling-compiler")
  .settings(libraryDependencies ++= compilerDependencies(scalaVersion.value))

lazy val benchmarks = gatlingModule("gatling-benchmarks")
  .dependsOn(core, http)
  .enablePlugins(JmhPlugin)
  .settings(libraryDependencies ++= benchmarkDependencies)

lazy val app = gatlingModule("gatling-app")
  .dependsOn(core % "compile->compile;test->test", http, jms, jdbc, redis, graphite, charts)

lazy val recorder = gatlingModule("gatling-recorder")
  .dependsOn(core % "compile->compile;test->test", http)
  .settings(libraryDependencies ++= recorderDependencies)

lazy val testFramework = gatlingModule("gatling-test-framework")
  .dependsOn(app)
  .settings(libraryDependencies ++= testFrameworkDependencies)

lazy val bundle = gatlingModule("gatling-bundle")
  .dependsOn(core, http)
  .enablePlugins(UniversalPlugin)
  .settings(generateConfigFiles(core): _*)
  .settings(generateConfigFiles(recorder): _*)
  .settings(copyLogbackXml(core): _*)
  .settings(bundleSettings: _*)
  .settings(noArtifactToPublish)
