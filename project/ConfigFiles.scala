import com.typesafe.sbt.SbtNativePackager.Universal
import sbt._
import sbt.Keys._

object ConfigFiles {

  private val LeadingSpacesRegex = """^(\s+)"""

  def generateConfigFiles(fromProject: Project) = Seq(
    Compile / resourceGenerators += Def.task {
      generateCommentedConfigFile((fromProject / Compile / resources).value, (Universal / sourceDirectory).value)
    }.taskValue
  )

  def copyGatlingDefaults(destProject: Project) = Seq(
    destProject / Compile / resourceGenerators += Def.task {
      copyGatlingDefaultConfigFile((destProject / Compile / resourceDirectory).value, (Compile / resourceDirectory).value)
    }.taskValue
  )

  private def generateCommentedConfigFile(resources: Seq[File], sourceDirectory: File): Seq[File] = {
    def generateFile(outputPath: File, source: File): File = {
      val outputFileName = source.getName.replaceAll("-defaults", "")
      val lines = IO.readLines(source)
      val commentedLines = lines.map { line =>
        if (line.endsWith("{") || line.endsWith("}")) line
        else line.replaceAll(LeadingSpacesRegex, "$1#")
      }
      val fullOutputPath = outputPath / outputFileName
      IO.writeLines(fullOutputPath, commentedLines)
      fullOutputPath
    }

    val outputPath = sourceDirectory / "conf"
    val configFiles = resources.filter(_.getName.endsWith("conf"))
    configFiles.map(generateFile(outputPath, _))
  }

  private def copyGatlingDefaultConfigFile(destDirectory: File, resourceDirectory: File): Seq[File] = {
    val configFile = (resourceDirectory ** new ExactFilter("gatling-defaults.conf")).get.head
    val targetFile = destDirectory / configFile.getName
    IO.copyFile(configFile, targetFile)
    Seq(targetFile.getCanonicalFile)
  }
}
