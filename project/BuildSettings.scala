import io.gatling.build.MavenPublishKeys._
import io.gatling.build.license._

import sbt.Keys._
import sbt._
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport._

object BuildSettings {

  lazy val basicSettings = Seq(
    headerLicense := ApacheV2License,
    githubPath := "gatling/gatling",
    projectDevelopers := developers,
    parallelExecution in Test := false
  // [fl]
  //
  //
  //
  // [fl]
  )

  lazy val gatlingModuleSettings =
    basicSettings ++ scaladocSettings

  lazy val noArtifactToPublish =
    publishArtifact in Compile := false

  // [fl]
  //
  //
  //
  //
  //
  // [fl]

  val developers = Seq(
    GatlingDeveloper("slandelle@gatling.io", "Stephane Landelle", isGatlingCorp = true),
    GatlingDeveloper("gcorre@gatling.io", "Guillaume Corré", isGatlingCorp = true),
    GatlingDeveloper("tgrenier@gatling.io", "Thomas Grenier", isGatlingCorp = true),
    GatlingDeveloper("ccousseran@gatling.io", "Cédric Cousseran", isGatlingCorp = true),
    GatlingDeveloper("achaouat@gatling.io", "Alexandre Chaouat", isGatlingCorp = true)
  )

/****************************/
  /** Documentation settings **/
/****************************/

  lazy val scaladocSettings = Seq(
    autoAPIMappings := true
  )

/**************************************/
  /** gatling-charts specific settings **/
/**************************************/

  lazy val chartTestsSettings = Seq(
    fork := true,
    javaOptions in Test := Seq("-DGATLING_HOME=gatling-charts") // Allows FileDataReaderSpec to run
  )

  lazy val excludeDummyComponentLibrary = Seq(
    mappings in (Compile, packageBin) := {
      val compiledClassesMappings = (mappings in (Compile, packageBin)).value
      compiledClassesMappings.filter { case (file, path) => !path.contains("io/gatling/charts/component/impl") }
    }
  )
}
