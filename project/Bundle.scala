import sbt.Keys._
import sbt._

import com.typesafe.sbt.SbtNativePackager._
import sbt.classpath.ClasspathUtilities

object Bundle {

  val bundleArtifacts = {
    def bundleArtifact(ext: String) = Artifact("gatling-bundle", ext, ext, "bundle")

    Seq(
      addArtifact(bundleArtifact("zip"), packageBin in Universal)
    ).flatMap(_.settings)
  }

  val bundleSettings = packagerSettings ++ bundleArtifacts

}
