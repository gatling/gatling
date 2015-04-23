import sbt._
import sbt.Keys._

import com.typesafe.sbt.SbtSite.site
import com.typesafe.sbt.site.SphinxSupport.Sphinx

import io.gatling.build.license._
import io.gatling.build.LicenseKeys._
import io.gatling.build.MavenPublishKeys._
import sbtunidoc.Plugin.{ ScalaUnidoc, unidocSettings }

object BuildSettings {

  lazy val basicSettings = Seq(
    license := ApacheV2,
    githubPath := "gatling/gatling",
    projectDevelopers := developers
  )

  lazy val gatlingModuleSettings =
    basicSettings ++ scaladocSettings

  lazy val noCodeToPublish = Seq(
    publishArtifact in Compile := false
  )

  val developers = Seq(
    GatlingDeveloper("slandelle@excilys.com", "Stephane Landelle", true),
    GatlingDeveloper("nremond@gmail.com", "Nicolas Rémond", false),
    GatlingDeveloper("pdalpra@excilys.com", "Pierre Dal-Pra", false),
    GatlingDeveloper("aduffy@gilt.com", "Andrew Duffy", false),
    GatlingDeveloper("jasonk@bluedevel.com", "Jason Koch", false),
    GatlingDeveloper("ivan.mushketik@gmail.com", "Ivan Mushketyk", false),
    GatlingDeveloper("gcorre@excilys.com", "Guillaume Corré", true)
  )

  /****************************/
  /** Documentation settings **/
  /****************************/

  lazy val scaladocSettings = Seq(
    autoAPIMappings := true
  )

  lazy val docSettings = unidocSettings ++ site.settings ++ site.sphinxSupport() ++ Seq(
    site.addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), "latest/api"),
    unmanagedSourceDirectories in Test := ((sourceDirectory in Sphinx).value ** "code").get
  ) ++ scaladocSettings

  /**************************************/
  /** gatling-charts specific settings **/
  /**************************************/

  lazy val chartTestsSettings = Seq(
    fork := true,
    javaOptions in Test := Seq("-DGATLING_HOME=gatling-charts") // Allows FileDataReaderSpec to run
  )

  lazy val excludeDummyComponentLibrary =  Seq(
    mappings in (Compile, packageBin) := {
      val compiledClassesMappings = (mappings in (Compile, packageBin)).value 
      compiledClassesMappings.filter { case (file, path) => !path.contains("io/gatling/charts/component/impl") }
    }
  )
}
