import com.typesafe.sbt.SbtNativePackager.Universal
import sbt.Keys._
import sbt._

object CopyLogback {

  def copyLogbackXml(fromProject: Project) = Seq(
    resourceGenerators in Compile += Def.task {
      copyDummyLogbackXml((resources in Compile in fromProject).value, (sourceDirectory in Universal).value)
    }.taskValue
  )

  private def copyDummyLogbackXml(resources: Seq[File], sourceDirectory: File): Seq[File] = {
    val configFile = resources.filter(_.getName ==  "logback.dummy").head
    val outputPath = sourceDirectory / "conf"
    val targetFile = outputPath / (configFile.base + ".xml")
    IO.copyFile(configFile, targetFile)
    Seq(targetFile.getCanonicalFile)
  }
}
