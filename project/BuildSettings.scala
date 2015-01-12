import sbt._
import sbt.Keys._

import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import com.typesafe.sbt.SbtSite.site
import com.typesafe.sbt.site.SphinxSupport.Sphinx
import net.virtualvoid.sbt.graph.Plugin.graphSettings
import sbtunidoc.Plugin.{ ScalaUnidoc, unidocSettings }
import scoverage.ScoverageSbtPlugin.instrumentSettings
import scoverage.ScoverageSbtPlugin.ScoverageKeys

object BuildSettings {

  lazy val basicSettings = Seq(
    homepage                  := Some(url("http://gatling.io")),
    organization              := "io.gatling",
    organizationHomepage      := Some(url("http://gatling.io")),
    startYear                 := Some(2011),
    licenses                  := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),
    scalaVersion              := "2.11.5",
    updateOptions             := updateOptions.value.withCachedResolution(true),
    resolvers                 := Seq(Resolver.mavenLocal, Opts.resolver.sonatypeSnapshots),
    javacOptions              := Seq("-Xlint:-options","-source", "1.7", "-target", "1.7"),
    parallelExecution in Test := false,
    scalacOptions             := Seq(
      "-encoding", "UTF-8",
      "-target:jvm-1.7",
      "-deprecation",
      "-feature",
      "-unchecked",
      "-language:implicitConversions",
      "-language:postfixOps",
      "-Xfuture"
    )
  ) ++ Publish.settings ++ Release.settings

  lazy val gatlingModuleSettings =
    basicSettings ++ formattingSettings ++ graphSettings ++ scaladocSettings ++ instrumentSettings ++ Seq(ScoverageKeys.highlighting := true)

  lazy val noCodeToPublish = Seq(
    publishArtifact in Compile := false
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

  /*************************/
  /** Formatting settings **/
  /*************************/

  lazy val formattingSettings = SbtScalariform.scalariformSettings ++ Seq(
    ScalariformKeys.preferences := formattingPreferences
  )

  import scalariform.formatter.preferences._

  def formattingPreferences = 
    FormattingPreferences()
      .setPreference(DoubleIndentClassDeclaration, true)
      .setPreference(AlignParameters, true)
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(IndentLocalDefs, true)

}
