import sbt._

object Dependencies { 

	/**************************/
	/** Compile dependencies **/
	/**************************/

	private def scalaCompiler(version: String) = "org.scala-lang"             % "scala-compiler"     % version
	private def scalaReflect(version: String)  = "org.scala-lang"             % "scala-reflect"      % version
	private def scalaSwing(version: String)    = "org.scala-lang"             % "scala-swing"        % version
	private val jsr166e                        = "io.gatling"                 % "jsr166e"            % "1.0"
	private val ahc                            = "com.ning"                   % "async-http-client"  % "1.8.4"
	private val netty                          = "io.netty"                   % "netty"              % "3.9.0.Final"
	private val akkaActor                      = "com.typesafe.akka"         %% "akka-actor"         % "2.2.4"
	private val config                         = "com.typesafe"               % "config"             % "1.2.0"
	private val saxon                          = "net.sf.saxon"               % "Saxon-HE"           % "9.5.1-5"    classifier "compressed"
	private val slf4jApi                       = "org.slf4j"                  % "slf4j-api"          % "1.7.6"
	private val fastring                       = "com.dongxiguo"             %% "fastring"           % "0.2.2"
	private val jodaTime                       = "joda-time"                  % "joda-time"          % "2.3"
	private val jodaConvert                    = "org.joda"                   % "joda-convert"       % "1.5"
	private val scopt                          = "com.github.scopt"          %% "scopt"              % "3.2.0"
	private val scalalogging                   = "com.typesafe"              %% "scalalogging-slf4j" % "1.1.0"
	private val jackson                        = "com.fasterxml.jackson.core" % "jackson-databind"   % "2.3.2"
	private val boon                           = "io.fastjson"                % "boon"               % "0.13"
	private val jsonpath                       = "io.gatling"                %% "jsonpath"           % "0.4.0"
	private val commonsMath                    = "org.apache.commons"         % "commons-math3"      % "3.2"
	private val joddLagarto                    = "org.jodd"                   % "jodd-lagarto"       % "3.5"
	private val jzlib                          = "com.jcraft"                 % "jzlib"              % "1.1.3"
	private val commonsIo                      = "commons-io"                 % "commons-io"         % "2.4"
	private val redisClient                    =  "net.debasishg"             %% "redisclient"        % "2.11"        exclude("org.scala-lang", "scala-actors")
	private val zinc                           = "com.typesafe.zinc"          % "zinc"               % "0.3.2-M1"
	private val openCsv                        = "net.sf.opencsv"             % "opencsv"            % "2.3"
	private val jmsApi                         = "javax.jms"                  % "jms-api"            % "1.1-rev-1"
	private val logbackClassic                 = "ch.qos.logback"             % "logback-classic"    % "1.0.13"

	/***********************/
	/** Test dependencies **/
	/***********************/

	private val junit                          = "junit"                      % "junit"              % "4.11"        % "test"
	private val specs2                         = "org.specs2"                %% "specs2"             % "2.3.10"      % "test"
	private val akkaTestKit                    = "com.typesafe.akka"         %% "akka-testkit"       % "2.2.4"       % "test"
	private val mockitoCore                    = "org.mockito"                % "mockito-core"       % "1.9.5"       % "test"

	private val testDeps = Seq(junit, specs2, akkaTestKit, mockitoCore)

	/****************************/
	/** Dependencies by module **/
	/****************************/

	def coreDependencies(scalaVersion: String) = Seq(
		scalaCompiler(scalaVersion), jsr166e, akkaActor, saxon, jodaTime, jodaConvert, slf4jApi, scalalogging,
		scalaReflect(scalaVersion), jsonpath, jackson, boon, commonsMath, joddLagarto, commonsIo, config, fastring,
		openCsv, logbackClassic) ++ testDeps

	val redisDependencies = redisClient +: testDeps

	val httpDependencies = Seq(ahc, netty, jzlib) ++ testDeps

	val jmsDependencies = jmsApi +: testDeps

	val chartsDependencies = testDeps

	val metricsDependencies = testDeps

	val appDependencies = Seq(scopt, zinc)

	def recorderDependencies(scalaVersion: String) = Seq(scalaSwing(scalaVersion), scopt, jackson) ++ testDeps
}
