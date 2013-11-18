import sbt._

object Dependencies { 

	private val scalaCompiler     = "org.scala-lang"        % "scala-compiler"     % "2.10.3"
	private val scalaReflect      = "org.scala-lang"        % "scala-reflect"      % "2.10.3"
	private val scalaSwing        = "org.scala-lang"        % "scala-swing"        % "2.10.3"
	private val ahc               = "com.ning"              % "async-http-client"  % "1.7.21"
	private val netty             = "io.netty"              % "netty"              % "3.8.0.Final"
	private val akkaActor         = "com.typesafe.akka"    %% "akka-actor"         % "2.2.3"
	private val config            = "com.typesafe"          % "config"             % "1.0.2"
	private val saxon             = "net.sf.saxon"          % "Saxon-HE"           % "9.5.1-3"    classifier "compressed"
	private val slf4jApi          = "org.slf4j"             % "slf4j-api"          % "1.7.5"
	private val fastring          = "com.dongxiguo"        %% "fastring"           % "0.2.2"
	private val jodaTime          = "joda-time"             % "joda-time"          % "2.3"
	private val jodaConvert       = "org.joda"              % "joda-convert"       % "1.5"
	private val scopt             = "com.github.scopt"     %% "scopt"              % "3.1.0"
	private val scalalogging      = "com.typesafe"         %% "scalalogging-slf4j" % "1.0.1"
	private val jsonSmart         = "net.minidev"           % "json-smart"         % "1.2"
	private val jsonpath          = "io.gatling"           %% "jsonpath"           % "0.2.4"
	private val commonsMath       = "org.apache.commons"    % "commons-math3"      % "3.2"
	private val joddLagarto       = "org.jodd"              % "jodd-lagarto"       % "3.4.8"
	private val jzlib             = "com.jcraft"            % "jzlib"              % "1.1.3"
	private val commonsIo         = "commons-io"            % "commons-io"         % "2.4"
	private val redisClient       = "net.debasishg"        %% "redisclient"        % "2.11"        exclude("org.scala-lang", "scala-actors")
	private val zinc              = "com.typesafe.zinc"     % "zinc"               % "0.3.1-M1"
	private val openCsv           = "net.sf.opencsv"        % "opencsv"            % "2.3"

	private val grizzlyWebsockets = "org.glassfish.grizzly" % "grizzly-websockets" % "2.3.6"       % "provided" 

	private val logbackClassic    = "ch.qos.logback"        % "logback-classic"    % "1.0.13"      % "runtime"

	private val junit             = "junit"                 % "junit"              % "4.11"        % "test"
	private val specs2            = "org.specs2"           %% "specs2"             % "2.0"         % "test"
	private val akkaTestKit       = "com.typesafe.akka"    %% "akka-testkit"       % "2.2.3"       % "test"
	private val mockitoCore       = "org.mockito"           % "mockito-core"       % "1.9.5"       % "test"

	private val testDeps = Seq(junit, specs2, akkaTestKit, mockitoCore)

	val coreDeps = Seq(
		scalaCompiler, akkaActor, saxon, jodaTime, jodaConvert, slf4jApi, scalalogging, scalaReflect, jsonSmart,
		jsonpath, commonsMath, joddLagarto, commonsIo, config, fastring, openCsv, logbackClassic, netty
	) ++ testDeps

	val redisDeps = Seq(redisClient) ++ testDeps

	val httpDeps = Seq(ahc, jzlib, grizzlyWebsockets) ++ testDeps

	val chartsDeps = testDeps

	val metricsDeps = testDeps

	val appDeps = Seq(scopt, zinc)

	val recorderDeps = Seq(scalaSwing, scopt) ++ testDeps
}
