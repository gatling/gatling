import com.typesafe.sbt.SbtNativePackager.Universal
import sbt._
import sbt.Keys._

object ConfigFiles {

  private val LeadingSpacesRegex = """^(\s+)"""

  def generateConfigFiles(fromProject: Project) = Seq(
    resourceGenerators in Compile += Def.task {
      generateCommentedConfigFile((resources in Compile in fromProject).value, (sourceDirectory in Universal).value)
    }.taskValue
  )

  def copyGatlingDefaults(destProject: Project) = Seq(
    resourceGenerators in Compile in destProject += Def.task {
      copyGatlingDefaultConfigFile((resourceDirectory in Compile in destProject).value, (resourceDirectory in Compile).value)
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
