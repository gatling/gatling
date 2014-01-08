import sbt._
import sbt.Keys._

import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import com.typesafe.sbt.SbtSite.site
import net.virtualvoid.sbt.graph.Plugin.graphSettings

import Resolvers._

object BuildSettings {

	lazy val basicSettings = Seq(
		homepage              := Some(new URL("http://gatling.io")),
		organization          := "io.gatling",
		organizationHomepage  := Some(new URL("http://gatling.io")),
		startYear             := Some(2011),
		licenses              := Seq("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.html")),
		scalaVersion          := "2.10.3",
		resolvers             := Seq(sonatypeSnapshots, localMaven),
		scalacOptions         := Seq(
			"-encoding",
			"UTF-8",
			"-target:jvm-1.6",
			"-deprecation",
			"-feature",
			"-unchecked",
			"-language:implicitConversions",
			"-language:postfixOps"
		)
	) ++ Publish.settings ++ Release.settings

	lazy val gatlingModuleSettings =
		basicSettings ++ formattingSettings ++ graphSettings ++ Seq(
			exportJars := true
		)

	lazy val noCodeToPublish = Seq(
		publishArtifact in Compile := false
	)

	/****************************/
	/** Documentation settings **/
	/****************************/

	lazy val docSettings = site.settings ++ site.sphinxSupport()

	/**************************************/
	/** gatling-charts specific settings **/
	/**************************************/

	lazy val chartTestsSettings = Seq(
		fork := true,
		javaOptions in Test := Seq("-DGATLING_HOME=gatling-charts") // Allows FileDataReaderSpec to run
	)

	lazy val excludeDummyComponentLibrary =  Seq(
		mappings in (Compile, packageBin) := {
			val compiledClassesMappings = (mappings in (Compile, packageBin)).value 
			compiledClassesMappings.filter { case (file, path) => !path.contains("io/gatling/charts/component/impl") }
		}
	)

	/*************************/
	/** Formatting settings **/
	/*************************/

	lazy val formattingSettings = SbtScalariform.scalariformSettings ++ Seq(
		ScalariformKeys.preferences := formattingPreferences
	)

	import scalariform.formatter.preferences._

	def formattingPreferences = 
		FormattingPreferences()
			.setPreference(DoubleIndentClassDeclaration, false)
			.setPreference(IndentWithTabs, true)

}
