import sbt._
import sbt.Keys._

object Generate {

  private val LeadingSpacesRegex = """^(\s+)"""

  def generateConfigFileSettings(destProject: Project) = Seq(
    resourceGenerators in Compile += Def.task {
      generateCommentedConfigFile(destProject.base, (resourceDirectory in Compile).value)
    }.taskValue,
    mappings in (Compile, packageBin) := {
      val compiledClassesMappings = (mappings in (Compile, packageBin)).value
      compiledClassesMappings.filterNot { case (file, path) => path.endsWith(".conf") && !path.endsWith("-defaults.conf") }
    }
  )

  private def generateCommentedConfigFile(projectPath: File, resourceDirectory: File): Seq[File] = {
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

    val outputPath = projectPath / "src" / "universal" / "conf"
    val configFiles = (resourceDirectory ** new SimpleFileFilter(_.getName.endsWith("conf"))).get
    configFiles.map(generateFile(outputPath, _))
   }
}
