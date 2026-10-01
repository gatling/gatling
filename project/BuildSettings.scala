import com.github.sbt.JavaFormatterPlugin.autoImport.*
import sbt.Keys.*
import sbt.{ *, given }

object BuildSettings {
  lazy val basicSettings: Seq[Def.Setting[?]] = Seq(
    Test / parallelExecution := false,
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    licenses := Seq("Apache-2.0" -> uri("https://www.apache.org/licenses/LICENSE-2.0.html"))
    // not set in private build
    // [e]
    //
    //
    // [e]
  )

  lazy val javafmtSettings: Seq[Def.Setting[?]] = Seq(
    javafmtOnCompile := !sys.env.getOrElse("CI", "false").toBoolean
  )

  lazy val gatlingModuleSettings: Seq[Def.Setting[?]] =
    basicSettings ++ scaladocSettings ++ utf8Encoding ++ javafmtSettings

  lazy val skipPublishing: Def.Setting[?] =
    publish / skip := true

  lazy val noSrcToPublish: Def.Setting[?] =
    Compile / packageSrc / publishArtifact := false

  lazy val noDocToPublish: Def.Setting[?] =
    Compile / packageDoc / publishArtifact := false

  // UTF-8

  lazy val utf8Encoding: Seq[Def.Setting[?]] = Seq(
    fork := true,
    Compile / javacOptions ++= Seq("-encoding", "utf8", "-Xlint:unchecked"),
    Test / javacOptions ++= Seq("-encoding", "utf8", "-Xlint:unchecked")
  )

  // Documentation settings

  lazy val scaladocSettings: Seq[Def.Setting[?]] = Seq(
    autoAPIMappings := true
  )

  // gatling-charts specific settings

  lazy val chartTestsSettings: Seq[Def.Setting[?]] = Seq(
    fork := true,
    Test / javaOptions += "--add-opens=java.base/java.lang=ALL-UNNAMED" // Allows LogFileReaderSpec to run
  )
}
