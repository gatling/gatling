import sbt._

import BuildSettings._
import Bundle._
import ConfigFiles._
import Dependencies._
import VersionFile._

Global / githubPath := "gatling/gatling"
Global / gatlingDevelopers := Seq(
  GatlingDeveloper("slandelle@gatling.io", "Stephane Landelle", isGatlingCorp = true),
  GatlingDeveloper("gcorre@gatling.io", "Guillaume Corré", isGatlingCorp = true),
  GatlingDeveloper("ccousseran@gatling.io", "Cédric Cousseran", isGatlingCorp = true),
  GatlingDeveloper("tpetillot@gatling.io  ", "Thomas Petillot", isGatlingCorp = true)
)
// [e]
//
// [e]

// Root project

ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
Global / scalaVersion := "2.13.10"

lazy val root = Project("gatling-parent", file("."))
  .enablePlugins(GatlingOssPlugin)
  .disablePlugins(SbtSpotless)
  .aggregate(
    jdkUtil,
    nettyUtil,
    commonsShared,
    commonsSharedUnstable,
    commons,
    docSamples,
    jsonpath,
    core,
    coreJava,
    jdbc,
    jdbcJava,
    redis,
    redisJava,
    httpClient,
    http,
    httpJava,
    jms,
    jmsJava,
    mqtt,
    mqttJava,
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

// Modules
lazy val docSamples = (project in file("src/docs"))
  .disablePlugins(SbtSpotless)
  .settings(
    basicSettings,
    skipPublishing,
    Test / unmanagedSourceDirectories ++= (baseDirectory.value ** "code").get,
    libraryDependencies ++= docDependencies,
    // Avoid formatting but avoid errors when calling this tasks with "all"
    scalafmtSbtCheck := Def.task(true).value,
    scalafmtCheckAll := Def.task(()).value,
    scalafixAll := Def.task(()).value,
    spotlessCheck := Def.task(()).value,
    kotlinVersion := "1.8.10"
  )
  .dependsOn(
    Seq(commons, jsonpath, core, coreJava, http, httpJava, jms, jmsJava, mqtt, mqttJava, jdbc, jdbcJava, redis, redisJava).map(
      _ % "compile->compile;test->test"
    ): _*
  )
  .settings(libraryDependencies ++= docSamplesDependencies)

def gatlingModule(id: String) =
  Project(id, file(id))
    .enablePlugins(GatlingOssPlugin)
    .disablePlugins(KotlinPlugin)
    .settings(gatlingModuleSettings ++ CodeAnalysis.settings)

lazy val jdkUtil = gatlingModule("gatling-jdk-util")

lazy val nettyUtil = gatlingModule("gatling-netty-util")
  .dependsOn(jdkUtil)
  .settings(libraryDependencies ++= nettyUtilDependencies)

lazy val commonsShared = gatlingModule("gatling-commons-shared")
  .disablePlugins(SbtSpotless)
  .dependsOn(jdkUtil)
  .settings(libraryDependencies ++= commonsSharedDependencies(scalaVersion.value))

lazy val commonsSharedUnstable = gatlingModule("gatling-commons-shared-unstable")
  .disablePlugins(SbtSpotless)
  .dependsOn(commonsShared)
  .settings(libraryDependencies ++= commonsSharedUnstableDependencies)

lazy val commons = gatlingModule("gatling-commons")
  .disablePlugins(SbtSpotless)
  .dependsOn(commonsShared % "compile->compile;test->test")
  .dependsOn(commonsSharedUnstable)
  .settings(libraryDependencies ++= commonsDependencies)
  .settings(generateVersionFileSettings)

lazy val jsonpath = gatlingModule("gatling-jsonpath")
  .dependsOn(jdkUtil)
  .disablePlugins(SbtSpotless)
  .settings(libraryDependencies ++= jsonpathDependencies)

lazy val core = gatlingModule("gatling-core")
  .dependsOn(nettyUtil)
  .dependsOn(commons % "compile->compile;test->test")
  .dependsOn(jsonpath % "compile->compile;test->test")
  .settings(libraryDependencies ++= coreDependencies)
  .settings(copyGatlingDefaults(compiler))

lazy val coreJava = gatlingModule("gatling-core-java")
  .dependsOn(core % "compile->compile;test->test")
  .settings(libraryDependencies ++= coreJavaDependencies)

lazy val jdbc = gatlingModule("gatling-jdbc")
  .dependsOn(core % "compile->compile;test->test")
  .settings(libraryDependencies ++= jdbcDependencies)

lazy val jdbcJava = gatlingModule("gatling-jdbc-java")
  .dependsOn(coreJava, jdbc % "compile->compile;test->test")
  .settings(libraryDependencies ++= defaultJavaDependencies)

lazy val mqtt = gatlingModule("gatling-mqtt")
  .disablePlugins(SbtSpotless)
  .dependsOn(nettyUtil, core)
  .settings(libraryDependencies ++= mqttDependencies)

lazy val mqttJava = gatlingModule("gatling-mqtt-java")
  .dependsOn(coreJava, mqtt % "compile->compile;test->test")
  .settings(libraryDependencies ++= defaultJavaDependencies)

lazy val redis = gatlingModule("gatling-redis")
  .disablePlugins(SbtSpotless)
  .dependsOn(core % "compile->compile;test->test")
  .settings(libraryDependencies ++= redisDependencies)

lazy val redisJava = gatlingModule("gatling-redis-java")
  .dependsOn(coreJava, redis % "compile->compile;test->test")
  .settings(libraryDependencies ++= defaultJavaDependencies)

lazy val httpClient = gatlingModule("gatling-http-client")
  .dependsOn(nettyUtil % "compile->compile;test->test")
  .settings(libraryDependencies ++= httpClientDependencies)

lazy val http = gatlingModule("gatling-http")
  .dependsOn(core % "compile->compile;test->test", httpClient % "compile->compile;test->test")
  .settings(libraryDependencies ++= httpDependencies)

lazy val httpJava = gatlingModule("gatling-http-java")
  .dependsOn(coreJava, http % "compile->compile;test->test")
  .settings(libraryDependencies ++= defaultJavaDependencies)

lazy val jms = gatlingModule("gatling-jms")
  .dependsOn(core % "compile->compile;test->test")
  .settings(libraryDependencies ++= jmsDependencies)
  .settings(Test / parallelExecution := false)

lazy val jmsJava = gatlingModule("gatling-jms-java")
  .dependsOn(coreJava, jms % "compile->compile;test->test")
  .settings(libraryDependencies ++= defaultJavaDependencies)

lazy val charts = gatlingModule("gatling-charts")
  .disablePlugins(SbtSpotless)
  .dependsOn(core % "compile->compile;test->test")
  .settings(libraryDependencies ++= chartsDependencies)
  .settings(chartTestsSettings)

lazy val graphite = gatlingModule("gatling-graphite")
  .disablePlugins(SbtSpotless)
  .dependsOn(core % "compile->compile;test->test")
  .settings(libraryDependencies ++= graphiteDependencies)

lazy val compiler = gatlingModule("gatling-compiler")
  .disablePlugins(SbtSpotless)
  .settings(libraryDependencies ++= compilerDependencies(scalaVersion.value))

lazy val benchmarks = gatlingModule("gatling-benchmarks")
  .disablePlugins(SbtSpotless)
  .dependsOn(core, http)
  .enablePlugins(JmhPlugin)
  .settings(libraryDependencies ++= benchmarkDependencies)

lazy val app = gatlingModule("gatling-app")
  .disablePlugins(SbtSpotless)
  .dependsOn(core, coreJava, http, httpJava, jms, jmsJava, mqtt, mqttJava, jdbc, jdbcJava, redis, redisJava, graphite, charts)

lazy val recorder = gatlingModule("gatling-recorder")
  .dependsOn(core % "compile->compile;test->test", http)
  .settings(libraryDependencies ++= recorderDependencies)

lazy val testFramework = gatlingModule("gatling-test-framework")
  .disablePlugins(SbtSpotless)
  .dependsOn(app)
  .settings(libraryDependencies ++= testFrameworkDependencies)

lazy val bundle = gatlingModule("gatling-bundle")
  .dependsOn(app, recorder)
  .enablePlugins(UniversalPlugin)
  .settings(bundleSettings(core, bundleSamples, recorder))
  .settings(libraryDependencies ++= bundleDependencies)

lazy val bundleSamples = gatlingModule("gatling-bundle-samples")
  .dependsOn(app)
  .settings(
    skipPublishing
  )

lazy val publicSamples = Project("gatling-samples", file("gatling-samples"))
  .dependsOn(app)
  .enablePlugins(GatlingOssPlugin)
  .settings(gatlingModuleSettings)
  .settings(
    skipPublishing
  )
