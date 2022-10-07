import sbt._
import sbt.Keys._

import java.time.{ ZoneOffset, ZonedDateTime }
import java.time.format.DateTimeFormatter

object VersionFile {
  val generateVersionFileSettings = Seq(
    Compile / resourceGenerators += Def.task {
      Seq(generateVersionFile((Compile / resourceDirectory).value, (ThisBuild / version).value))
    }.taskValue
  )

  private def generateVersionFile(resourcesDir: File, version: String): File = {
    val versionFile = resourcesDir / "gatling-version.properties"
    IO.write(
      versionFile,
      s"""version=$version
         |release-date=${ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME)}""".stripMargin
    )
    versionFile
  }
}
