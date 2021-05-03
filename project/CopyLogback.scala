import com.typesafe.sbt.SbtNativePackager.Universal
import sbt.Keys._
import sbt._

object CopyLogback {

  def copyLogbackXml(fromProject: Project) = Seq(
    Compile / resourceGenerators += Def.task {
      copyDummyLogbackXml((fromProject / Compile / resources).value, (Universal / sourceDirectory).value)
    }.taskValue
  )

  private def copyDummyLogbackXml(resources: Seq[File], sourceDirectory: File): Seq[File] = {
    val configFile = resources.filter(_.getName == "logback.dummy").head
    val outputPath = sourceDirectory / "conf"
    val targetFile = outputPath / (configFile.base + ".xml")
    IO.copyFile(configFile, targetFile)
    Seq(targetFile.getCanonicalFile)
  }
}
