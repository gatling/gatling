import com.typesafe.sbt.SbtNativePackager._
import autoImport._
import NativePackagerHelper._
import sbt.Keys._
import sbt._

object Bundle {

  lazy val bundleArtifacts =
    addArtifact(Artifact("gatling-bundle", "zip", "zip", "bundle"), Universal / packageBin).settings

  lazy val bundleSettings = bundleArtifacts ++ Seq(
    Universal / mappings ++= mapSourcesToBundleLocation((Compile / sources).value, (Compile / scalaSource).value)
  )

  def mapSourcesToBundleLocation(sources: Seq[File], sourceDirectory: File): Seq[(File, String)] =
    sources
      .map { source =>
        val pathInBundle = relativeTo(sourceDirectory)(source).map("user-files/simulations/" + _)
        source -> pathInBundle
      }
      .collect { case (key, value) if value.isDefined => (key, value.get) }
}
