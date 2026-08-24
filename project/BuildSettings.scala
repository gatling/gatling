import com.github.sbt.JavaFormatterPlugin.autoImport._
import sbt.Keys._
import sbt._

object BuildSettings {
  lazy val basicSettings = Seq(
    Test / parallelExecution := false,
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html"))
    // not set in private build
    // [e]
    //
    //
    // [e]
  )

  lazy val javafmtSettings = Seq(
    javafmtOnCompile := !sys.env.getOrElse("CI", "false").toBoolean
  )

  lazy val gatlingModuleSettings =
    basicSettings ++ scaladocSettings ++ utf8Encoding ++ javafmtSettings

  lazy val skipPublishing =
    publish / skip := true

  lazy val noSrcToPublish =
    Compile / packageSrc / publishArtifact := false

  lazy val noDocToPublish =
    Compile / packageDoc / publishArtifact := false

  // UTF-8

  lazy val utf8Encoding = Seq(
    fork := true,
    Compile / javacOptions ++= Seq("-encoding", "utf8", "-Xlint:unchecked"),
    Test / javacOptions ++= Seq("-encoding", "utf8", "-Xlint:unchecked")
  )

  // Documentation settings

  lazy val scaladocSettings = Seq(
    autoAPIMappings := true
  )

  // gatling-charts specific settings

  lazy val chartTestsSettings = Seq(
    fork := true,
    Test / javaOptions += "--add-opens=java.base/java.lang=ALL-UNNAMED" // Allows LogFileReaderSpec to run
  )
}
