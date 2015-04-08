import com.typesafe.sbt.SbtNativePackager._
import sbt.Keys._
import sbt._

object Bundle {

  lazy val bundleArtifacts = {
    def bundleArtifact(ext: String) = Artifact("gatling-bundle", ext, ext, "bundle")

    addArtifact(bundleArtifact("zip"), packageBin in Universal).settings
  }

  lazy val bundleSettings = packagerSettings ++ bundleArtifacts ++ Seq(
    mappings in Universal ++= mapSourcesToBundleLocation((sources in Compile).value, (scalaSource in Compile).value)
  )

  def mapSourcesToBundleLocation(sources: Seq[File], sourceDirectory: File): Seq[(File, String)] =
    sources.map { source =>
      val pathInBundle = relativeTo(sourceDirectory)(source).map("user-files/simulations/" + _)
      source -> pathInBundle
    }.collect { case (key, value) if value.isDefined => (key, value.get) }
}
