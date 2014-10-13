import sbt.Keys._
import sbt._

import com.typesafe.sbt.SbtNativePackager._
import sbt.classpath.ClasspathUtilities

object Bundle {

  val allJars = taskKey[Seq[File]]("List of all jars needed for the bundle")

  val bundleArtifacts = {
    def bundleArtifact(ext: String) = Artifact("gatling-bundle", ext, ext, "bundle")

    Seq(
      addArtifact(bundleArtifact("zip"), packageBin in Universal)
    ).flatMap(_.settings)
  }

  val bundleSettings = packagerSettings ++ bundleArtifacts ++ Seq(
    allJars := (fullClasspath in Runtime).value.map(_.data).filter(ClasspathUtilities.isArchive),
    mappings in Universal ++= allJars.value.map(jar => jar -> buildDestinationJarPath(jar, version.value))
  )

  def buildDestinationJarPath(sourceJarPath: File, version: String): String = {
    if(sourceJarPath.getName.startsWith("gatling") && !sourceJarPath.getName.contains(version))
      s"lib/zinc/${sourceJarPath.base}-$version.${sourceJarPath.ext}"
    else
      s"lib/zinc/${sourceJarPath.getName}"
  }
}
