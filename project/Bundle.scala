import sbt._
import sbt.Keys._
import sbt.classpath.ClasspathUtilities
import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.universal.Keys.packageZipTarball

object Bundle {

	val bundleAllClasspaths = taskKey[Seq[Classpath]]("Aggregated classpath of all modules")
	val bundleAllDependencies = taskKey[Seq[File]]("Lists of all dependencies' JARs' paths")
	val gatlingJars = taskKey[Seq[File]]("List of all Gatling jars")
	val allJars = taskKey[Seq[File]]("List of all jars needed for the bundle")

	val bundleArtifacts = {
		def bundleArtifact(ext: String) = Artifact("gatling-bundle", ext, ext, "bundle")

		Seq(
			addArtifact(bundleArtifact("zip"), packageBin in Universal),
			addArtifact(bundleArtifact("tgz"), packageZipTarball in Universal)
		).flatMap(_.settings)
	}

	val bundleSettings = packagerSettings ++ bundleArtifacts ++ Seq(
		bundleAllClasspaths <<= (thisProjectRef, buildStructure) flatMap aggregated(dependencyClasspath.task in Runtime),
		bundleAllDependencies := bundleAllClasspaths.value.flatten.map(_.data).filter(ClasspathUtilities.isArchive).distinct,
		gatlingJars <<= (thisProjectRef, buildStructure) flatMap aggregated(packageBin.task in Compile),
		allJars := bundleAllDependencies.value ++ gatlingJars.value,
		mappings in Universal ++= allJars.value.map(jar => jar -> ("lib/" + jar.getName))
	)

	def aggregated[T](task: SettingKey[Task[T]])(projectRef: ProjectRef, structure: BuildStructure) = {
		val projects = aggregatedProjects(projectRef, structure)
		projects flatMap { task in LocalProject(_) get structure.data } join
	}

	def aggregatedProjects(projectRef: ProjectRef, structure: BuildStructure): Seq[String] = {
		val aggregate = Project.getProject(projectRef, structure).toSeq.flatMap(_.aggregate)
		aggregate flatMap { ref => ref.project +: aggregatedProjects(ref, structure) }
	}

}