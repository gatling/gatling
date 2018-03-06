import sbt._

object Dependencies {

  // Compile dependencies

  // format: OFF
  private def scalaReflect(version: String)  = "org.scala-lang"                         % "scala-reflect"                 % version
  private def scalaCompiler(version: String) = "org.scala-lang"                         % "scala-compiler"                % version
  private val scalaSwing                     = "org.scala-lang.modules"                %% "scala-swing"                   % "2.0.1"
  private val scalaXml                       = "org.scala-lang.modules"                %% "scala-xml"                     % "1.1.0"
  private val scalaParserCombinators         = "org.scala-lang.modules"                %% "scala-parser-combinators"      % "1.1.0"
  private val java8Compat                    = "org.scala-lang.modules"                %% "scala-java8-compat"            % "0.8.0"
  private val ahc                            = "org.asynchttpclient"                    % "async-http-client"             % "2.4.3"
  private val ahcNettyUtils                  = "org.asynchttpclient"                    % "async-http-client-netty-utils" % ahc.revision
  private val netty                          = "io.netty"                               % "netty-codec-http"              % "4.1.22.Final"
  private val nettyNativeTransport           = "io.netty"                               % "netty-transport-native-epoll"  % netty.revision classifier "linux-x86_64"
  private val akkaActor                      = "com.typesafe.akka"                     %% "akka-actor"                    % "2.5.10"
  private val akkaSlf4j                      = "com.typesafe.akka"                     %% "akka-slf4j"                    % akkaActor.revision
  private val config                         = "com.typesafe"                           % "config"                        % "1.3.3"
  private val saxon                          = "net.sf.saxon"                           % "Saxon-HE"                      % "9.8.0-8"
  private val slf4jApi                       = "org.slf4j"                              % "slf4j-api"                     % "1.7.25"
  private val fastring                       = "com.dongxiguo"                         %% "fastring"                      % "0.3.1"
  private val spire                          = ("org.typelevel"                         %% "spire-macros"                  % "0.15.0")
    .exclude("org.typelevel", "machinist_2.12")
    .exclude("org.typelevel", "algebra_2.12")
  private val scopt                          = "com.github.scopt"                      %% "scopt"                         % "3.7.0"
  private val scalaLogging                   = "com.typesafe.scala-logging"            %% "scala-logging"                 % "3.8.0"
  private val jackson                        = "com.fasterxml.jackson.core"             % "jackson-databind"              % "2.9.4"
  private val sfm                            = ("org.simpleflatmapper"                  % "sfm-csv"                       % "3.15.10")
    .exclude("org.simpleflatmapper", "sfm-reflect")
    .exclude("org.simpleflatmapper", "sfm-tuples")
  private val sfmUtil                        = "org.simpleflatmapper"                   % "sfm-util"                      % sfm.revision
  private val json4sJackson                  = "org.json4s"                            %% "json4s-jackson"                % "3.5.3"
  private val boon                           = "io.advantageous.boon"                   % "boon-json"                     % "0.6.6" exclude("org.slf4j", "slf4j-api")
  private val jsonpath                       = "io.gatling"                            %% "jsonpath"                      % "0.6.11"
  private val joddLagarto                    = "org.jodd"                               % "jodd-lagarto"                  % "4.1.4"
  private val boopickle                      = "io.suzaku"                             %% "boopickle"                     % "1.2.6"
  private val redisClient                    = "net.debasishg"                         %% "redisclient"                   % "3.4"
  private val zinc                           = ("org.scala-sbt"                        %% "zinc"                          % "1.1.1")
    .exclude("org.scala-lang.modules", "scala-parser-combinators_2.12")
    .exclude("org.scala-lang.modules", "scala-xml_2.12")
    .exclude("org.scala-sbt", "launcher-interface")
    .exclude("org.scala-sbt", "sbinary_2.12")
    .exclude("org.scala-sbt", "zinc-ivy-integration_2.12")
    .exclude("com.eed3si9n", "sjson-new-core_2.12")
    .exclude("com.eed3si9n", "sjson-new-scalajson_2.12")
    .exclude("com.lihaoyi", "fastparse_2.12")
    .exclude("com.lmax", "disruptor")
    .exclude("jline", "jline")
    .exclude("org.apache.logging.log4j", "log4j-api")
    .exclude("org.apache.logging.log4j", "log4j-core")
  private val compilerBridge                 = "org.scala-sbt"                         %% "compiler-bridge"               % zinc.revision
  private val jmsApi                         = "org.apache.geronimo.specs"              % "geronimo-jms_1.1_spec"         % "1.1.1"
  private val logback                        = "ch.qos.logback"                         % "logback-classic"               % "1.2.3"
  private val tdigest                        = "com.tdunning"                           % "t-digest"                      % "3.1"
  private val hdrHistogram                   = "org.hdrhistogram"                       % "HdrHistogram"                  % "2.1.10"
  private val caffeine                       = "com.github.ben-manes.caffeine"          % "caffeine"                      % "2.6.2"
  private val bouncycastle                   = "org.bouncycastle"                       % "bcpkix-jdk15on"                % "1.59"
  private val quicklens                      = "com.softwaremill.quicklens"            %% "quicklens"                     % "1.4.11"
  private val fastUuid                       = "com.eatthepath"                         % "fast-uuid"                     % "0.1"
  private val testInterface                  = "org.scala-sbt"                          % "test-interface"                % "1.0"
  private val pebble                         = "io.gatling"                             % "pebble"                        % "2.4.0.2"
  private val findbugs                       = "com.google.code.findbugs"               % "jsr305"                        % "3.0.2"

  // Test dependencies

  private val scalaTest                      = "org.scalatest"                         %% "scalatest"                    % "3.0.5"             % "test"
  private val scalaCheck                     = "org.scalacheck"                        %% "scalacheck"                   % "1.13.5"            % "test"
  private val akkaTestKit                    = "com.typesafe.akka"                     %% "akka-testkit"                 % akkaActor.revision  % "test"
  private val mockitoCore                    = "org.mockito"                            % "mockito-core"                 % "2.15.0"            % "test"
  private val activemqBroker                 = "org.apache.activemq"                    % "activemq-broker"              % "5.15.3"            % "test"
  private val h2                             = "com.h2database"                         % "h2"                           % "1.4.196"           % "test"
  private val jmh                            = "org.openjdk.jmh"                        % "jmh-core"                     % "1.20"
  // format: ON

  private val loggingDeps = Seq(slf4jApi, scalaLogging, logback)
  private val testDeps = Seq(scalaTest, scalaCheck, akkaTestKit, mockitoCore)
  private val parserDeps = Seq(jsonpath, jackson, boon, saxon, joddLagarto)

  // Dependencies by module

  def commonsDependencies(scalaVersion: String) =
    Seq(scalaReflect(scalaVersion), config, fastring, boopickle, spire, quicklens, java8Compat, ahcNettyUtils, findbugs, fastUuid) ++ loggingDeps ++ testDeps

  val coreDependencies =
    Seq(akkaActor, akkaSlf4j, sfm, sfmUtil, java8Compat, caffeine, pebble, scalaParserCombinators, scopt) ++
      parserDeps ++ testDeps

  val redisDependencies = redisClient +: testDeps

  val httpDependencies = Seq(ahc, nettyNativeTransport, scalaXml) ++ testDeps

  val jmsDependencies = Seq(jmsApi, activemqBroker) ++ testDeps

  val jdbcDependencies = h2 +: testDeps

  val chartsDependencies = tdigest +: testDeps

  val metricsDependencies = hdrHistogram +: testDeps

  val benchmarkDependencies = Seq(jmh)

  def compilerDependencies(scalaVersion: String) =
    Seq(scalaCompiler(scalaVersion), scalaReflect(scalaVersion), config, slf4jApi, logback, zinc, compilerBridge, scopt)

  val recorderDependencies = Seq(scalaSwing, jackson, json4sJackson, bouncycastle, netty, akkaActor) ++ testDeps

  val testFrameworkDependencies = Seq(testInterface)

  val docDependencies = Seq(activemqBroker)
}
