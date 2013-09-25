import sbt._
import sbt.Keys._

import BuildSettings._
import Dependencies._
import Bundle._

object GatlingBuild extends Build {

	override lazy val settings = super.settings ++ {
		shellPrompt := { state => Project.extract(state).currentProject.id + " > " }
	}

	/******************/
	/** Root project **/
	/******************/

	lazy val root = Project("gatling-parent", file("."))
		.aggregate(core, jdbc, redis, http, charts, metrics, app, recorder, bundle)
		.settings(basicSettings: _*)
		.settings(noCodeToPublish: _*) // Still publish main JAR with aether-deploy
		.settings(docSettings: _*)

	/*************/
	/** Modules **/
	/*************/

	def gatlingModule(id: String) = Project(id, file(id)).settings(gatlingModuleSettings: _*)

	lazy val core = gatlingModule("gatling-core")
		.settings(libraryDependencies ++= coreDeps)

	lazy val jdbc = gatlingModule("gatling-jdbc")
		.dependsOn(core)

	lazy val redis = gatlingModule("gatling-redis")
		.dependsOn(core)
		.settings(libraryDependencies ++= redisDeps)

	lazy val http = gatlingModule("gatling-http")
		.dependsOn(core)
		.settings(libraryDependencies ++= httpDeps)

	lazy val charts = gatlingModule("gatling-charts")
		.dependsOn(core)
		.settings(libraryDependencies ++= chartsDeps)
		.settings(excludeDummyComponentLibrary: _*)
		.settings(chartTestsSettings: _*)

	lazy val metrics = gatlingModule("gatling-metrics")
		.dependsOn(core)
		.settings(libraryDependencies ++= metricsDeps)

	lazy val app = gatlingModule("gatling-app")
		.dependsOn(core, http, jdbc, redis, charts, metrics)
		.settings(libraryDependencies ++= appDeps)

	lazy val recorder = gatlingModule("gatling-recorder")
		.dependsOn(core, http)
		.settings(libraryDependencies ++= recorderDeps)
		.settings(compileOrder := CompileOrder.JavaThenScala)

	lazy val bundle = gatlingModule("gatling-bundle")
		.dependsOn(Seq(app, recorder).map(_ % "runtime->runtime"): _*)
		.settings(bundleSettings: _*)
		.settings(noCodeToPublish: _*) // Still publish main JAR with aether-deploy
		.settings(exportJars := false) // Don't export gatling-bundle's jar 
}