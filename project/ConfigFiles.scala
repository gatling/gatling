import sbt._
import sbt.Keys._

object ConfigFiles {

  def copyGatlingDefaults(destProject: Project) =
    destProject / Compile / resourceGenerators += Def.task {
      copyGatlingDefaultConfigFile((destProject / Compile / resourceDirectory).value, (Compile / resourceDirectory).value)
    }.taskValue

  private def copyGatlingDefaultConfigFile(destDirectory: File, resourceDirectory: File): Seq[File] = {
    val configFile = (resourceDirectory ** new ExactFilter("gatling-defaults.conf")).get.head
    val targetFile = destDirectory / configFile.getName
    IO.copyFile(configFile, targetFile)
    Seq(targetFile.getCanonicalFile)
  }
}
