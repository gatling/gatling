import sbt.Keys._
import sbt._

object BuildSettings {

  lazy val basicSettings = Seq(
    parallelExecution in Test := false,
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"))
    // [fl]
    //
    //
    // [fl]
  )

  lazy val gatlingModuleSettings =
    basicSettings ++ scaladocSettings ++ utf8Encoding

  lazy val skipPublishing =
    skip in publish := true

  lazy val noSrcToPublish =
    publishArtifact in packageSrc in Compile := false

  lazy val noDocToPublish =
    publishArtifact in packageDoc in Compile := false

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
