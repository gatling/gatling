import sbt._

object Dependencies { 

	/**************************/
	/** Compile dependencies **/
	/**************************/

	private val scalaCompiler     = "org.scala-lang"             % "scala-compiler"     % "2.10.3"
	private val scalaReflect      = "org.scala-lang"             % "scala-reflect"      % "2.10.3"
	private val scalaSwing        = "org.scala-lang"             % "scala-swing"        % "2.10.3"
	private val jsr166e           = "io.gatling"                 % "jsr166e"            % "1.0"
	private val ahc               = "com.ning"                   % "async-http-client"  % "1.7.23"
	private val netty             = "io.netty"                   % "netty"              % "3.9.0.Final"
	private val akkaActor         = "com.typesafe.akka"         %% "akka-actor"         % "2.2.3"
	private val config            = "com.typesafe"               % "config"             % "1.0.2"
	private val saxon             = "net.sf.saxon"               % "Saxon-HE"           % "9.5.1-3"    classifier "compressed"
	private val slf4jApi          = "org.slf4j"                  % "slf4j-api"          % "1.7.5"
	private val fastring          = "com.dongxiguo"             %% "fastring"           % "0.2.2"
	private val jodaTime          = "joda-time"                  % "joda-time"          % "2.3"
	private val jodaConvert       = "org.joda"                   % "joda-convert"       % "1.5"
	private val scopt             = "com.github.scopt"          %% "scopt"              % "3.2.0"
	private val scalalogging      = "com.typesafe"              %% "scalalogging-slf4j" % "1.0.1"
	private val jackson           = "com.fasterxml.jackson.core" % "jackson-databind"   % "2.3.0"
	private val boon              = "io.fastjson"                % "boon"               % "0.6"
	private val jsonpath          = "io.gatling"                %% "jsonpath"           % "0.3.0"
	private val commonsMath       = "org.apache.commons"         % "commons-math3"      % "3.2"
	private val joddLagarto       = "org.jodd"                   % "jodd-lagarto"       % "3.4.10"
	private val jzlib             = "com.jcraft"                 % "jzlib"              % "1.1.3"
	private val commonsIo         = "commons-io"                 % "commons-io"         % "2.4"
	private val redisClient       = "net.debasishg"             %% "redisclient"        % "2.11"        exclude("org.scala-lang", "scala-actors")
	private val zinc              = "com.typesafe.zinc"          % "zinc"               % "0.3.1-M1"
	private val openCsv           = "net.sf.opencsv"             % "opencsv"            % "2.3"
	private val jmsApi            = "javax.jms"                  % "jms-api"            % "1.1-rev-1"
	private val logbackClassic    = "ch.qos.logback"             % "logback-classic"    % "1.0.13"

	/***************************/
	/** Optional dependencies **/
	/***************************/

	private val grizzlyWebsockets = "org.glassfish.grizzly"      % "grizzly-websockets" % "2.3.6"       % "provided"

	/***********************/
	/** Test dependencies **/
	/***********************/

	private val junit             = "junit"                      % "junit"              % "4.11"        % "test"
	private val specs2            = "org.specs2"                %% "specs2"             % "2.0"         % "test"
	private val akkaTestKit       = "com.typesafe.akka"         %% "akka-testkit"       % "2.2.3"       % "test"
	private val mockitoCore       = "org.mockito"                % "mockito-core"       % "1.9.5"       % "test"

	private val testDeps = Seq(junit, specs2, akkaTestKit, mockitoCore)

	/****************************/
	/** Dependencies by module **/
	/****************************/

	val coreDeps = Seq(
		scalaCompiler, jsr166e, akkaActor, saxon, jodaTime, jodaConvert, slf4jApi, scalalogging, scalaReflect, jsonpath,
		jackson, boon, commonsMath, joddLagarto, commonsIo, config, fastring, openCsv, logbackClassic) ++ testDeps

	val redisDeps = redisClient +: testDeps

	val httpDeps = Seq(ahc, netty, jzlib, grizzlyWebsockets) ++ testDeps

	val jmsDeps = jmsApi +: testDeps

	val chartsDeps = testDeps

	val metricsDeps = testDeps

	val appDeps = Seq(scopt, zinc)

	val recorderDeps = Seq(scalaSwing, scopt, jackson) ++ testDeps
}
