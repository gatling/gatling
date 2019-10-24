import com.typesafe.sbt.SbtNativePackager._
import autoImport._
import NativePackagerHelper._
import sbt.Keys._
import sbt._

object Bundle {

  lazy val bundleArtifacts =
    addArtifact(Artifact("gatling-bundle", "zip", "zip", "bundle"), packageBin in Universal).settings

  lazy val bundleSettings = bundleArtifacts ++ Seq(
    mappings in Universal ++= mapSourcesToBundleLocation((sources in Compile).value, (scalaSource in Compile).value)
  )

  def mapSourcesToBundleLocation(sources: Seq[File], sourceDirectory: File): Seq[(File, String)] =
    sources
      .map { source =>
        val pathInBundle = relativeTo(sourceDirectory)(source).map("user-files/simulations/" + _)
        source -> pathInBundle
      }
      .collect { case (key, value) if value.isDefined => (key, value.get) }
}
