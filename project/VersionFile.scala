import sbt._
import sbt.Keys._

object VersionFile {

  val generateVersionFileSettings = Seq(
    resourceGenerators in Compile += Def.task {
      Seq(generateVersionFile((resourceDirectory in Compile).value, version.value))
    }.taskValue
  )

  private def generateVersionFile(resourcesDir: File, version: String): File = {
    val versionFile = resourcesDir / "gatling-version.properties"
    IO.write(versionFile, s"version=$version")
    versionFile
  }
}
