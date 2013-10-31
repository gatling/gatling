import sbt._
import sbt.Keys._
import sbt.classpath.ClasspathUtilities
import com.typesafe.sbt.SbtNativePackager._

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
		mappings in Universal ++= allJars.value.map(jar => jar -> ("lib/" + jar.getName))
	)

}