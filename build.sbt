import sbt._

import BuildSettings._
import Bundle._
import ConfigFiles._
import CopyLogback._
import Dependencies._
import VersionFile._

Global / githubPath := "gatling/gatling"
Global / gatlingDevelopers := Seq(
  GatlingDeveloper("slandelle@gatling.io", "Stephane Landelle", isGatlingCorp = true),
  GatlingDeveloper("gcorre@gatling.io", "Guillaume Corré", isGatlingCorp = true),
  GatlingDeveloper("ccousseran@gatling.io", "Cédric Cousseran", isGatlingCorp = true),
  GatlingDeveloper("tpetillot@gatling.io  ", "Thomas Petillot", isGatlingCorp = true)
)
// [fl]
//
// [fl]

// Root project

Global / scalaVersion := "2.13.5"

lazy val root = Project("gatling-parent", file("."))
  .enablePlugins(GatlingOssPlugin, SphinxPlugin)
  .dependsOn(Seq(commons, jsonpath, core, http, jms, mqtt, jdbc, redis).map(_ % "compile->compile;test->test"): _*)
  .aggregate(
    nettyUtil,
    commonsShared,
    commonsSharedUnstable,
    commons,
    jsonpath,
    core,
    jdbc,
    redis,
    httpClient,
    http,
    jms,
    mqtt,
    charts,
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
  .settings(Test / unmanagedSourceDirectories := ((Sphinx / sourceDirectory).value ** "code").get)
  .settings(scalafmtConfig := Def.task {
    val file = scalafmtConfig.value
    IO.append(
      file,
      """
        |project.excludeFilters = ["src/sphinx"]
        |""".stripMargin
    )
    file
  }.value)

// Modules

def gatlingModule(id: String) =
  Project(id, file(id))
    .enablePlugins(GatlingOssPlugin)
    .settings(gatlingModuleSettings ++ CodeAnalysis.settings)

lazy val nettyUtil = gatlingModule("gatling-netty-util")
  .settings(libraryDependencies ++= nettyUtilDependencies)

lazy val commonsShared = gatlingModule("gatling-commons-shared")
  .dependsOn(nettyUtil % "compile->compile;test->test")
  .settings(libraryDependencies ++= commonsSharedDependencies(scalaVersion.value))

lazy val commonsSharedUnstable = gatlingModule("gatling-commons-shared-unstable")
  .dependsOn(commonsShared)
  .settings(libraryDependencies ++= commonsSharedUnstableDependencies)

lazy val commons = gatlingModule("gatling-commons")
  .dependsOn(commonsShared % "compile->compile;test->test")
  .dependsOn(commonsSharedUnstable)
  .settings(libraryDependencies ++= commonsDependencies)
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
  .settings(Test / parallelExecution := false)

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
  .dependsOn(core, http, jms, jdbc, redis, graphite, charts)

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
  .settings(exportJars := false, publishArtifact in Compile := false)
  .settings(CodeAnalysis.disable)
