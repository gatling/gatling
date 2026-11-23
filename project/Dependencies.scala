import sbt.*

object Dependencies {
  // One ModuleID per classifier: sbt 2.1's PomGenerator only writes the first explicit artifact of a ModuleID,
  // so chaining .classifier(...) drops all but one classifier from the published POM
  private def withClassifiers(module: ModuleID, classifiers: String*): Seq[ModuleID] =
    classifiers.map(module.classifier)

  // format: off
  // Compile dependencies
  private def scalaReflect(version: String)  = "org.scala-lang"                % "scala-reflect"               % version % Provided
  private val scalaParserCombinators         = "org.scala-lang.modules"       %% "scala-parser-combinators"    % "2.5.0"
  private val gatlingSharedCli               = "io.gatling"                    % "gatling-shared-cli"          % "0.1.0"
  private val gatlingSharedModel             = "io.gatling"                   %% "gatling-shared-model"        % "0.2.0"
  private val gatlingSharedUtil              = "io.gatling"                   %% "gatling-shared-util"         % "0.1.0"
  private val nettyHttp                      = "io.netty"                      % "netty-codec-http"            % "4.2.21.Final"
  private val nettyBuffer                    = nettyHttp.withName("netty-buffer")
  private val nettyHandler                   = nettyHttp.withName("netty-handler")
  private val nettyProxy                     = nettyHttp.withName("netty-handler-proxy")
  private val nettyDns                       = nettyHttp.withName("netty-resolver-dns")
  private val nettyEpoll                     = withClassifiers(nettyHttp.withName("netty-transport-native-epoll"), "linux-x86_64", "linux-aarch_64")
  private val nettyIoUring                   = withClassifiers(nettyHttp.withName("netty-transport-native-io_uring"), "linux-x86_64", "linux-aarch_64")
  private val nettyHttp2                     = nettyHttp.withName("netty-codec-http2")
  private val nettyResolverNativeOsX         = withClassifiers(nettyHttp.withName("netty-resolver-dns-native-macos"), "osx-x86_64", "osx-aarch_64")
  private val nettyTcNative                  = "io.netty"                      % "netty-tcnative-classes"      % "2.0.86.Final"
  private val nettyTcNativeBoringSsl         = withClassifiers(nettyTcNative.withName("netty-tcnative-boringssl-static"), "linux-x86_64", "linux-aarch_64", "osx-x86_64", "osx-aarch_64", "windows-x86_64")
  private val brotli4j                       = "com.aayushatharva.brotli4j"    % "brotli4j"                    % "1.24.0"
  private val brotli4jLinuxArm               = brotli4j.withName("native-linux-aarch64")
  private val brotli4jLinuxX86               = brotli4j.withName("native-linux-x86_64")
  private val brotli4cOsXArm                 = brotli4j.withName("native-osx-aarch64")
  private val brotli4cOsXX86                 = brotli4j.withName("native-osx-x86_64")
  private val brotli4jWindows                = brotli4j.withName("native-windows-x86_64")
  private val caffeine                       = "com.github.ben-manes.caffeine" % "caffeine"                    % "3.3.1"
  private val cfor                           = "io.github.metarank"           %% "cfor"                        % "0.4"
  private val commonsPool2                   = "org.apache.commons"            % "commons-pool2"               % "2.13.1"
  private val config                         = "com.typesafe"                  % "config"                      % "1.4.10"
  private val fastUuid                       = "com.eatthepath"                % "fast-uuid"                   % "0.2.0"
  private val jackson                        = "com.fasterxml.jackson.core"    % "jackson-databind"            % "2.23.0"
  private val jmespath                       = "io.burt"                       % "jmespath-jackson"            % "0.6.0"
  private val jmsApi                         = "jakarta.jms"                   % "jakarta.jms-api"             % "3.1.0"
  private val joddUtil                       = "org.jodd"                      % "jodd-util"                   % "6.3.0"
  private val jspecify                       = "org.jspecify"                  % "jspecify"                    % "1.0.1"
  private val lagarto                        = "io.gatling"                    % "jodd-lagarto"                % "6.1.0"
  private val logback                        = "ch.qos.logback"                % "logback-classic"             % "1.6.8"
  private val pebble                         = "io.pebbletemplates"            % "pebble"                      % "4.1.3"
  private val redisClient                    = "net.debasishg"                %% "redisclient"                 % "3.42"
  private val saxon                          = "net.sf.saxon"                  % "Saxon-HE"                    % "12.10"
  private val scalaLogging                   = "com.typesafe.scala-logging"   %% "scala-logging"               % "3.9.6"
  private val scopt                          = "com.github.scopt"             %% "scopt"                       % "4.2.0"
  private val sfm                            = "org.simpleflatmapper"          % "lightning-csv"               % "9.0.2" exclude("org.simpleflatmapper", "ow2-asm")
  private val slf4jApi                       = "org.slf4j"                     % "slf4j-api"                   % "2.0.20"
  private val julToSlf4j                     = "org.slf4j"                     % "jul-to-slf4j"                % slf4jApi.revision
  private val testInterface                  = "org.scala-sbt"                 % "test-interface"              % "1.0"
  private val typetools                      = "net.jodah"                     % "typetools"                   % "0.7.0"
  private val xmlresolver                    = "org.xmlresolver"               % "xmlresolver"                 % "6.1.1"
  private val xmlresolverData                = xmlresolver                                           classifier "data"

  // Charts dependencies
  private val tdigest                        = "org.elasticsearch"             % "elasticsearch-tdigest"       % "8.15.5"

  // Recorder dependencies
  private val scalaSwing                     = "org.scala-lang.modules"       %% "scala-swing"                 % "3.0.0"
  private val bouncyCastle                   = "io.gatling"                    % "gatling-recorder-bc-shaded"  % "1.85.0"

  // Test dependencies
  private val activemqBroker                 = "org.apache.activemq"           % "activemq-broker"             % "6.4.0"        % Test
  private val jetty                          = "org.eclipse.jetty"             % "jetty-server"                % "12.1.15"      % Test
  private val jettyProxy                     = jetty.withName("jetty-proxy")
  private val junit                          = "org.junit.jupiter"             % "junit-jupiter-api"           % "6.1.3"        % Test
  private val junitEngine                    = junit.withName("junit-jupiter-engine")
  private val junitPlatformLauncher          = "org.junit.platform"            % "junit-platform-launcher"     % junit.revision % Test
  private val jupiterInterface               = "com.github.sbt.junit"          % "jupiter-interface"           % "0.20.0"       % Test
  private val mockitoCore                    = "org.mockito"                   % "mockito-core"                % "5.24.0"       % Test
  private val scalaCheck                     = "org.scalacheck"               %% "scalacheck"                  % "1.20.0"       % Test
  private val scalaTest                      = "org.scalatest"                %% "scalatest"                   % "3.2.20"       % Test
  private val scalaTestScalacheck            = "org.scalatestplus"            %% "scalacheck-1-20"             % "3.2.20.0"     % Test
  private val scalaTestMockito               = "org.scalatestplus"            %% "mockito-5-23"                % "3.2.20.0"     % Test
  // format: on

  private val testDeps = Seq(
    scalaTest,
    scalaTestScalacheck,
    scalaTestMockito,
    scalaCheck,
    mockitoCore
  )
  private val parserDeps = Seq(jackson, saxon, xmlresolver, xmlresolverData, lagarto, joddUtil, jmespath)

  // Dependencies by module

  val nettyUtilDependencies: Seq[ModuleID] =
    Seq(
      gatlingSharedUtil,
      nettyBuffer,
      junit,
      junitEngine,
      junitPlatformLauncher,
      jupiterInterface
    ) ++ nettyEpoll ++ nettyIoUring

  val commonsSharedUnstableDependencies: Seq[ModuleID] = testDeps

  val commonsDependencies: Seq[ModuleID] =
    Seq(gatlingSharedUtil, config, cfor, slf4jApi, julToSlf4j, scalaLogging, logback) ++ testDeps

  val jsonpathDependencies: Seq[ModuleID] =
    Seq(gatlingSharedUtil, scalaParserCombinators, jackson) ++ testDeps

  def quicklensDependencies(scalaVersion: String): Seq[ModuleID] =
    Seq(scalaReflect(scalaVersion))

  val coreDependencies: Seq[ModuleID] =
    Seq(
      gatlingSharedModel,
      gatlingSharedCli,
      sfm,
      caffeine,
      pebble,
      scalaParserCombinators,
      scopt,
      nettyHandler,
      nettyTcNative
    ) ++
      nettyTcNativeBoringSsl ++ parserDeps ++ testDeps

  val defaultJavaDependencies: Seq[ModuleID] =
    Seq(jspecify, junit, junitEngine, junitPlatformLauncher, jupiterInterface) ++ testDeps

  val coreJavaDependencies: Seq[ModuleID] =
    Seq(typetools) ++ defaultJavaDependencies

  val redisDependencies: Seq[ModuleID] = Seq(redisClient, commonsPool2) ++ testDeps

  val httpClientDependencies: Seq[ModuleID] = Seq(
    gatlingSharedUtil,
    nettyHttp,
    nettyBuffer,
    nettyHandler,
    nettyProxy,
    nettyDns,
    nettyHttp2,
    nettyTcNative,
    brotli4j,
    brotli4jLinuxX86,
    brotli4jLinuxArm,
    brotli4cOsXX86,
    brotli4cOsXArm,
    brotli4jWindows,
    junit,
    junitEngine,
    junitPlatformLauncher,
    jupiterInterface,
    jetty,
    jettyProxy,
    slf4jApi,
    logback
  ) ++ nettyEpoll ++ nettyResolverNativeOsX ++ nettyTcNativeBoringSsl

  val httpDependencies: Seq[ModuleID] = Seq(saxon, xmlresolver, xmlresolverData) ++ testDeps

  val jmsDependencies: Seq[ModuleID] = Seq(jmsApi, fastUuid, activemqBroker) ++ testDeps

  val chartsDependencies: Seq[ModuleID] = tdigest +: testDeps

  val recorderDependencies: Seq[ModuleID] = Seq(gatlingSharedCli, scalaSwing, jackson, bouncyCastle, nettyHttp) ++ testDeps

  val testFrameworkDependencies: Seq[ModuleID] = Seq(gatlingSharedCli, testInterface)
}
