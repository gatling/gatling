import sbt._
import sbt.Keys._

import aether.WagonWrapper
import aether.Aether._
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import com.typesafe.sbt.SbtSite.site
import sbtrelease.ReleasePlugin._
import net.virtualvoid.sbt.graph.Plugin.graphSettings

import Dependencies._

object BuildSettings {

	lazy val basicSettings = Seq(
		homepage              := Some(new URL("http://gatling.io")),
		organization          := "io.gatling",
		organizationHomepage  := Some(new URL("http://gatling.io")),
		startYear             := Some(2011),
		licenses              := Seq("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.html")),
		resolvers             := Seq(excilysNexus),
		scalaVersion          := "2.10.2",
		crossPaths            := false,
		pomExtra              := scm ++ developersXml(developers),
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
	) ++ publishingSettings ++ releaseSettings

	lazy val gatlingModuleSettings =
		basicSettings ++ formattingSettings ++ graphSettings ++ Seq(
			exportJars := true
		)

	/*************************/
	/** Publishing settings **/
	/*************************/

	// TODO : split into snapshotSettings and releaseSettings
	lazy val publishingSettings = 
		aetherSettings ++ aetherPublishSettings ++ aetherPublishLocalSettings ++ Seq(
			pomIncludeRepository := { _ => false },
			wagons := {
				if(isSnapshot.value) 
					Seq(WagonWrapper("davs", "org.apache.maven.wagon.providers.webdav.WebDavWagon"))
				else Seq.empty
			},
			publishTo := { if(isSnapshot.value) Some(cloudbeesSnapshots) else Some(excilysReleases) },
			credentials += { if(isSnapshot.value) Credentials(file("/private/gatling/.credentials")) else Credentials(Path.userHome / ".sbt" / ".credentials") }
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

	/************************/
	/** POM extra metadata **/
	/************************/

	private val scm = {
		<scm>
			<connection>scm:git:git@github.com:excilys/gatling.git</connection>
			<developerConnection>scm:git:git@github.com:excilys/gatling.git</developerConnection>
			<url>https://github.com/excilys/gatling</url>
			<tag>HEAD</tag>
		</scm>
	}

	case class GatlingDeveloper(emailAddress: String, name: String, isEbiz: Boolean)

	val developers = Seq(
		GatlingDeveloper("slandelle@excilys.com", "Stephane Landelle", true),
		GatlingDeveloper("rsertelon@excilys.com", "Romain Sertelon", true),
		GatlingDeveloper("ybenkhaled@excilys.com", "Yassine Ben Khaled", true),
		GatlingDeveloper("hcordier@excilys.com", "Hugo Cordier", true),
		GatlingDeveloper("nicolas.remond@gmail.com", "Nicolas Rémond", false),
		GatlingDeveloper("skuenzli@gmail.com", "Stephen Kuenzli", false),
		GatlingDeveloper("pdalpra@excilys.com", "Pierre Dal-Pra", true),
		GatlingDeveloper("gcoutant@excilys.com", "Grégory Coutant", true),
		GatlingDeveloper("blemale@excilys.com", "Bastien Lemale", true),
		GatlingDeveloper("aduffy@gilt.com", "Andrew Duffy", false)
	)

	private def developersXml(devs: Seq[GatlingDeveloper]) = {
		<developers>
		{
			for(dev <- devs)
			yield {
				<developer>
					<id>{dev.emailAddress}</id>
					<name>{dev.name}</name>
					{ if (dev.isEbiz) <organization>eBusiness Information, Excilys Group</organization> }
				</developer>
			}
		}
		</developers>
	}

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