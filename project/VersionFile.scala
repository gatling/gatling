import sbt._
import sbt.Keys._

object VersionFile {

  val generateVersionFileSettings = Seq(
    Compile / resourceGenerators += Def.task {
      Seq(generateVersionFile((Compile / resourceDirectory).value, (ThisBuild / version).value))
    }.taskValue
  )

  private def generateVersionFile(resourcesDir: File, version: String): File = {
    val versionFile = resourcesDir / "gatling-version.properties"
    IO.write(versionFile, s"version=$version")
    versionFile
  }
}
