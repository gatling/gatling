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
    parallelExecution in Test := false,
    licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"))
    // [fl]
    //
    //
    //
    // [fl]
  )

  lazy val gatlingModuleSettings =
    basicSettings ++ scaladocSettings ++ utf8Encoding

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

  // UTF-8

  lazy val utf8Encoding = Seq(
    fork := true,
    javacOptions in Compile ++= Seq("-encoding", "utf8"),
    javacOptions in Test ++= Seq("-encoding", "utf8")
  )

  // Documentation settings

  lazy val scaladocSettings = Seq(
    autoAPIMappings := true
  )

  // gatling-charts specific settings

  lazy val chartTestsSettings = Seq(
    fork := true,
    javaOptions in Test := Seq("-DGATLING_HOME=gatling-charts") // Allows FileDataReaderSpec to run
  )

  lazy val excludeDummyComponentLibrary = Seq(
    mappings in (Compile, packageBin) := {
      val compiledClassesMappings = (mappings in (Compile, packageBin)).value
      compiledClassesMappings.filter { case (_, path) => !path.contains("io/gatling/charts/component/impl") }
    }
  )
}
