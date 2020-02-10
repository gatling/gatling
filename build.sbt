import sbt._

import BuildSettings._
import Bundle._
import ConfigFiles._
import CopyLogback._
import Dependencies._
import VersionFile._

// Root project

ThisBuild / Keys.useCoursier := false

lazy val root = Project("gatling-parent", file("."))
  .enablePlugins(AutomateHeaderPlugin, SonatypeReleasePlugin, SphinxPlugin)
  .dependsOn(Seq(commons, jsonpath, core, http, jms, decoupledResponse, mqtt, jdbc, redis).map(_ % "compile->compile;test->test"): _*)
  .aggregate(
    nettyUtil,
    commons,
    jsonpath,
    core,
    jdbc,
    redis,
    httpClient,
    http,
    jms,
    decoupledResponse,
    mqtt,
    graphite,
    app,
    recorder,
    testFramework,
    bundle,
    compiler
  )
  .settings(basicSettings)
  .settings(skipPublishing)
  .settings(libraryDependencies ++= docDependencies)
  .settings(unmanagedSourceDirectories in Test := ((sourceDirectory in Sphinx).value ** "code").get)

// Modules

def gatlingModule(id: String) =
  Project(id, file(id))
    .enablePlugins(AutomateHeaderPlugin, SonatypeReleasePlugin)
    .settings(gatlingModuleSettings ++ CodeAnalysis.settings)

lazy val nettyUtil = gatlingModule("gatling-netty-util")
  .settings(libraryDependencies ++= nettyUtilDependencies)

lazy val commons = gatlingModule("gatling-commons")
  .dependsOn(nettyUtil % "compile->compile;test->test")
  .settings(libraryDependencies ++= commonsDependencies(scalaVersion.value))
  .settings(generateVersionFileSettings)

lazy val jsonpath = gatlingModule("gatling-jsonpath")
  .settings(libraryDependencies ++= jsonpathDependencies)

lazy val core = gatlingModule("gatling-core")
  .dependsOn(commons % "compile->compile;test->test")
  .dependsOn(jsonpath % "compile->compile;test->test")
  .settings(libraryDependencies ++= coreDependencies)
  .settings(copyGatlingDefaults(compiler))

lazy val jdbc = gatlingModule("gatling-jdbc")
  .dependsOn(core % "compile->compile;test->test")
  .settings(libraryDependencies ++= jdbcDependencies)

lazy val mqtt = gatlingModule("gatling-mqtt")
  .dependsOn(nettyUtil, core)
  .settings(libraryDependencies ++= mqttDependencies)

lazy val redis = gatlingModule("gatling-redis")
  .dependsOn(core % "compile->compile;test->test")
  .settings(libraryDependencies ++= redisDependencies)

lazy val httpClient = gatlingModule("gatling-http-client")
  .dependsOn(nettyUtil % "compile->compile;test->test")
  .settings(libraryDependencies ++= httpClientDependencies)

lazy val http = gatlingModule("gatling-http")
  .dependsOn(core % "compile->compile;test->test", httpClient % "compile->compile;test->test")
  .settings(libraryDependencies ++= httpDependencies)

lazy val jms = gatlingModule("gatling-jms")
  .dependsOn(core % "compile->compile;test->test")
  .settings(libraryDependencies ++= jmsDependencies)
  .settings(parallelExecution in Test := false)

lazy val decoupledResponse = gatlingModule("gatling-decoupled-response")
  .dependsOn(core % "compile->compile;test->test", http % "compile->compile;test->test")
  .settings(libraryDependencies ++= httpDependencies)

lazy val charts = gatlingModule("gatling-charts")
  .dependsOn(core % "compile->compile;test->test")
  .settings(libraryDependencies ++= chartsDependencies)
  .settings(excludeDummyComponentLibrary)
  .settings(chartTestsSettings)

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
  .dependsOn(core, http, jms, decoupledResponse, jdbc, redis, graphite, charts)

lazy val recorder = gatlingModule("gatling-recorder")
  .dependsOn(core % "compile->compile;test->test", http)
  .settings(libraryDependencies ++= recorderDependencies)

lazy val testFramework = gatlingModule("gatling-test-framework")
  .dependsOn(app)
  .settings(libraryDependencies ++= testFrameworkDependencies)

lazy val bundle = gatlingModule("gatling-bundle")
  .dependsOn(core, http)
  .enablePlugins(UniversalPlugin)
  .settings(generateConfigFiles(core))
  .settings(generateConfigFiles(recorder))
  .settings(copyLogbackXml(core))
  .settings(bundleSettings)
  .settings(exportJars := false, noArtifactToPublish)
  .settings(CodeAnalysis.disable)
