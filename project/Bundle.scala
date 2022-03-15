import com.typesafe.sbt.SbtNativePackager._
import autoImport._
import NativePackagerHelper._
import sbt.Keys._
import sbt._

object Bundle {
  private val LeadingSpacesRegex = """^(\s+)"""

  lazy val bundleArtifacts =
    addArtifact(Artifact("gatling-bundle", "zip", "zip", "bundle"), Universal / packageBin).settings

  def bundleSettings(core: Project, samples: Project, recorder: Project) = bundleArtifacts ++ Seq(
    Universal / mappings ++= mapSamples((samples / Compile / javaSource).value, "user-files/simulations/"),
    Universal / mappings ++= mapSamples((samples / Compile / resourceDirectory).value, "user-files/resources/"),
    Universal / mappings ++= mapLogback((core / Compile / resources).value),
    Universal / mappings ++= commentFiles((core / Compile / resources).value, (Universal / resourceManaged).value),
    Universal / mappings ++= commentFiles((recorder / Compile / resources).value, (Universal / resourceManaged).value)
  )

  private def generateFile(outputPath: File, source: File): File = {
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

  def commentFiles(resources: Seq[File], outputDir: File) = {
    val configFiles = resources.filter(_.getName.endsWith("conf"))
    configFiles.map(generateFile(outputDir / "conf", _))
  } pair relativeTo(outputDir)

  def mapLogback(sources: Seq[File]): Seq[(File, String)] =
    sources.filter(_.getName == "logback.dummy").map { source =>
      source -> "conf/logback.xml"
    }

  def mapSamples(sourcesDir: File, prefix: String): Seq[(File, String)] =
    (sourcesDir ** AllPassFilter) pair relativeTo(sourcesDir).andThen(_.map(prefix + _))
}
