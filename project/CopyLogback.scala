import sbt._
import sbt.Keys._

object CopyLogback {

  def copyLogbackXmlSettings(destProject: Project) = Seq(
    resourceGenerators in Compile in destProject += Def.task {
      copyLogbackXml(destProject.base, (resourceDirectory in Compile).value)
    }.taskValue
  )

  private def copyLogbackXml(projectPath: File, resourceDirectory: File): Seq[File] = {
    val configFile = (resourceDirectory ** new ExactFilter("logback.dummy")).get.head
    val outputPath = projectPath / "src" / "universal" / "conf"
    val targetFile = outputPath / (configFile.base + ".xml")
    IO.copyFile(configFile, targetFile)
    Seq(targetFile.getAbsoluteFile)
  }
}
