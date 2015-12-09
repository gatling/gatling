import sbt._

object Dependencies { 

  /**************************/
  /** Compile dependencies **/
  /**************************/

  private def scalaReflect(version: String) = "org.scala-lang"                         % "scala-reflect"                % version
  private val scalaSwing                    = "org.scala-lang.modules"                %% "scala-swing"                  % "1.0.2"
  private val scalaXml                      = "org.scala-lang.modules"                %% "scala-xml"                    % "1.0.5"
  private val scalaParserCombinators        = "org.scala-lang.modules"                %% "scala-parser-combinators"     % "1.0.4"
  private val ahc                           = "org.asynchttpclient"                    % "async-http-client"            % "2.0.0-RC2"
  private val netty                         = "io.netty"                               % "netty-codec-http"             % "4.0.33.Final"
  private val nettyNativeTransport          = "io.netty"                               % "netty-transport-native-epoll" % netty.revision classifier "linux-x86_64"
  private val dnsJava                       = "dnsjava"                                % "dnsjava"                      % "2.1.7"
  private val akkaActor                     = "com.typesafe.akka"                     %% "akka-actor"                   % "2.4.1"
  private val config                        = "com.typesafe"                           % "config"                       % "1.3.0"
  private val saxon                         = "net.sf.saxon"                           % "Saxon-HE"                     % "9.7.0-1"
  private val slf4jApi                      = "org.slf4j"                              % "slf4j-api"                    % "1.7.13"
  private val fastring                      = "com.dongxiguo"                         %% "fastring"                     % "0.2.4"
  private val scopt                         = "com.github.scopt"                      %% "scopt"                        % "3.3.0"
  private val scalalogging                  = "com.typesafe.scala-logging"            %% "scala-logging"                % "3.1.0"
  private val jackson                       = "com.fasterxml.jackson.core"             % "jackson-databind"             % "2.7.0-rc1"
  private val jacksonCsv                    = "com.fasterxml.jackson.dataformat"       % "jackson-dataformat-csv"       % jackson.revision
  private val boon                          = "io.advantageous.boon"                   % "boon-json"                    % "0.5.5"
  private val jsonpath                      = "io.gatling"                            %% "jsonpath"                     % "0.6.4"
  private val joddLagarto                   = "org.jodd"                               % "jodd-lagarto"                 % "3.6.7"
  private val boopickle                     = "me.chrons"                             %% "boopickle"                    % "1.1.0"
  private val jzlib                         = "com.jcraft"                             % "jzlib"                        % "1.1.3"
  private val redisClient                   = "net.debasishg"                         %% "redisclient"                  % "3.0"
  private val zinc                          = "com.typesafe.zinc"                      % "zinc"                         % "0.3.9" exclude("org.scala-lang", "scala-compiler")
  private val jmsApi                        = "org.apache.geronimo.specs"              % "geronimo-jms_1.1_spec"        % "1.1.1"
  private val logbackClassic                = "ch.qos.logback"                         % "logback-classic"              % "1.1.3"
  private val tdigest                       = "com.tdunning"                           % "t-digest"                     % "3.1"
  private val hdrHistogram                  = "org.hdrhistogram"                       % "HdrHistogram"                 % "2.1.8"
  private val lru                           = "com.googlecode.concurrentlinkedhashmap" % "concurrentlinkedhashmap-lru"  % "1.4.2"
  private val bouncycastle                  = "org.bouncycastle"                       % "bcpkix-jdk15on"               % "1.53"
  private val quicklens                     = "com.softwaremill.quicklens"            %% "quicklens"                    % "1.4.2"
  private val testInterface                 = "org.scala-sbt"                          % "test-interface"               % "1.0"

  /***********************/
  /** Test dependencies **/
  /***********************/

  private val scalaTest                      = "org.scalatest"                         %% "scalatest"                   % "2.2.5"             % "test"
  private val scalaCheck                     = "org.scalacheck"                        %% "scalacheck"                  % "1.12.4"            % "test"
  private val akkaTestKit                    = "com.typesafe.akka"                     %% "akka-testkit"                % akkaActor.revision  % "test"
  private val mockitoCore                    = "org.mockito"                            % "mockito-core"                % "1.10.19"           % "test"
  private val activemqCore                   = "org.apache.activemq"                    % "activemq-broker"             % "5.8.0"             % "test"
  private val h2                             = "com.h2database"                         % "h2"                          % "1.4.187"           % "test"
  private val ffmq                           = "net.timewalker.ffmq"                    % "ffmq3-core"                  % "3.0.7"             % "test" exclude("log4j", "log4j") exclude("javax.jms", "jms")

  private val loggingDeps = Seq(slf4jApi, scalalogging, logbackClassic)
  private val testDeps = Seq(scalaTest, scalaCheck, akkaTestKit, mockitoCore)
  private val parserDeps = Seq(jsonpath, jackson, boon, saxon, joddLagarto)

  /****************************/
  /** Dependencies by module **/
  /****************************/

  def commonsDependencies(scalaVersion: String) =
    Seq(scalaReflect(scalaVersion), config, fastring, boopickle, quicklens) ++ loggingDeps ++ testDeps

  val coreDependencies =
    Seq(akkaActor, jacksonCsv, boopickle, lru, scalaParserCombinators, scopt, jzlib) ++
      parserDeps ++ testDeps

  val redisDependencies = redisClient +: testDeps

  val httpDependencies = Seq(ahc, nettyNativeTransport, dnsJava, scalaXml) ++ testDeps

  val jmsDependencies = Seq(jmsApi, activemqCore) ++ testDeps

  val jdbcDependencies = h2 +: testDeps

  val chartsDependencies = tdigest +: testDeps

  val metricsDependencies = hdrHistogram +: testDeps

  def compilerDependencies(scalaVersion: String) =
    Seq(scalaReflect(scalaVersion), config, slf4jApi, logbackClassic, zinc, scopt)

  val recorderDependencies = Seq(scalaSwing, jackson, bouncycastle, netty) ++ testDeps

  val testFrameworkDependencies = Seq(testInterface)

  val docDependencies = Seq(ffmq)
}
