import BuildSettings._
import Bundle._
import ConfigFiles._
import CopyLogback._
import Dependencies._
import VersionFile._

  lazy val `gatling-parent` = project.in(file("."))
    .enablePlugins(SonatypeReleasePlugin)
    .dependsOn(Seq(`gatling-commons`, `gatling-core`, `gatling-http`, `gatling-jms`, `gatling-jdbc`, `gatling-redis`).map(_ % "compile->compile;test->test"): _*)
    .aggregate(
      `gatling-commons`, `gatling-core`, `gatling-jdbc`, `gatling-redis`, `gatling-http`, 
      `gatling-jms`, `gatling-charts`, `gatling-metrics`, `gatling-app`, `gatling-recorder`, 
      `gatling-test-framework`, `gatling-bundle`, `gatling-compiler`)
    .settings(basicSettings: _*)
    .settings(noArtifactToPublish)
    .settings(docSettings(`gatling-benchmarks`, `gatling-bundle`): _*)
    .settings(libraryDependencies ++= docDependencies)

  lazy val `gatling-commons` = project.in(file("gatling-commons"))
    .enablePlugins(SonatypeReleasePlugin)
    .settings(gatlingModuleSettings: _*)
    .settings(libraryDependencies ++= commonsDependencies(scalaVersion.value))

  lazy val `gatling-core` = project.in(file("gatling-core"))
    .enablePlugins(SonatypeReleasePlugin)
    .settings(gatlingModuleSettings: _*)
    .dependsOn(`gatling-commons` % "compile->compile;test->test")
    .settings(libraryDependencies ++= coreDependencies)
    .settings(generateVersionFileSettings: _*)
    .settings(copyGatlingDefaults(`gatling-compiler`): _*)

  lazy val `gatling-jdbc` = project.in(file("gatling-jdbc"))
    .enablePlugins(SonatypeReleasePlugin)
    .settings(gatlingModuleSettings: _*)
    .dependsOn(`gatling-core` % "compile->compile;test->test")
    .settings(libraryDependencies ++= jdbcDependencies)

  lazy val `gatling-redis` = project.in(file("gatling-redis"))
    .enablePlugins(SonatypeReleasePlugin)
    .settings(gatlingModuleSettings: _*)
    .dependsOn(`gatling-core` % "compile->compile;test->test")
    .settings(libraryDependencies ++= redisDependencies)

  lazy val `gatling-http` = project.in(file("gatling-http"))
    .enablePlugins(SonatypeReleasePlugin)
    .settings(gatlingModuleSettings: _*)
    .dependsOn(`gatling-core` % "compile->compile;test->test")
    .settings(libraryDependencies ++= httpDependencies)

  lazy val `gatling-jms` = project.in(file("gatling-jms"))
    .enablePlugins(SonatypeReleasePlugin)
    .settings(gatlingModuleSettings: _*)
    .dependsOn(`gatling-core` % "compile->compile;test->test")
    .settings(libraryDependencies ++= jmsDependencies)
    .settings(parallelExecution in Test := false)

  lazy val `gatling-charts` = project.in(file("gatling-charts"))
    .enablePlugins(SonatypeReleasePlugin)
    .settings(gatlingModuleSettings: _*)
    .dependsOn(`gatling-core` % "compile->compile;test->test")
    .settings(libraryDependencies ++= chartsDependencies)
    .settings(excludeDummyComponentLibrary: _*)
    .settings(chartTestsSettings: _*)

  lazy val `gatling-metrics` = project.in(file("gatling-metrics"))
    .enablePlugins(SonatypeReleasePlugin)
    .settings(gatlingModuleSettings: _*)
    .dependsOn(`gatling-core` % "compile->compile;test->test")
    .settings(libraryDependencies ++= metricsDependencies)

  lazy val `gatling-compiler` = project.in(file("gatling-compiler"))
    .enablePlugins(SonatypeReleasePlugin)
    .settings(gatlingModuleSettings: _*)
    .settings(scalaVersion := "2.10.6")
    .settings(libraryDependencies ++= compilerDependencies(scalaVersion.value))

  lazy val `gatling-benchmarks` = project.in(file("gatling-benchmarks"))
    .enablePlugins(SonatypeReleasePlugin)
    .settings(gatlingModuleSettings: _*)
    .dependsOn(`gatling-core`, `gatling-http`)
    .enablePlugins(JmhPlugin)
    .settings(libraryDependencies ++= benchmarkDependencies)

  lazy val `gatling-app` = project.in(file("gatling-app"))
    .enablePlugins(SonatypeReleasePlugin)
    .settings(gatlingModuleSettings: _*)
    .dependsOn(`gatling-core`, `gatling-http`, `gatling-jms`, `gatling-jdbc`, `gatling-redis`, `gatling-metrics`, `gatling-charts`)

  lazy val `gatling-recorder` = project.in(file("gatling-recorder"))
    .enablePlugins(SonatypeReleasePlugin)
    .settings(gatlingModuleSettings: _*)
    .dependsOn(`gatling-core` % "compile->compile;test->test", `gatling-http`)
    .settings(libraryDependencies ++= recorderDependencies)

  lazy val `gatling-test-framework` = project.in(file("gatling-test-framework"))
    .enablePlugins(SonatypeReleasePlugin)
    .settings(gatlingModuleSettings: _*)
    .dependsOn(`gatling-app`)
    .settings(libraryDependencies ++= testFrameworkDependencies)

  lazy val `gatling-bundle` = project.in(file("gatling-bundle"))
    .enablePlugins(SonatypeReleasePlugin)
    .settings(gatlingModuleSettings: _*)
    .dependsOn(`gatling-core`, `gatling-http`)
    .settings(generateConfigFiles(`gatling-core`): _*)
    .settings(generateConfigFiles(`gatling-recorder`): _*)
    .settings(copyLogbackXml(`gatling-core`): _*)
    .settings(bundleSettings: _*)
    .settings(noArtifactToPublish)
