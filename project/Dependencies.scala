import sbt._

object Dependencies {
  // Compile dependencies

  // scalafmt: { align.tokens = ["=", "%", "%%", "classifier"], maxColumn = 250 }
  private def scalaReflect(version: String)  = "org.scala-lang"                                               % "scala-reflect"            % version % Provided
  private val gatlingSharedUtil              = "io.gatling"                                                  %% "gatling-shared-util"      % "0.0.13"
  private val gatlingSharedModel             = "io.gatling"                                                  %% "gatling-shared-model"     % "0.1.2"
  private val gatlingSharedCli               = "io.gatling"                                                   % "gatling-shared-cli"       % "0.0.7"
  private val scalaSwing                     = "org.scala-lang.modules"                                      %% "scala-swing"              % "3.0.0"
  private val scalaParserCombinators         = "org.scala-lang.modules"                                      %% "scala-parser-combinators" % "2.4.0"
  private val nettyHttp                      = "io.netty"                                                     % "netty-codec-http"         % "4.2.7.Final"
  private val nettyBuffer                    = nettyHttp.withName("netty-buffer")
  private val nettyHandler                   = nettyHttp.withName("netty-handler")
  private val nettyProxy                     = nettyHttp.withName("netty-handler-proxy")
  private val nettyDns                       = nettyHttp.withName("netty-resolver-dns")
  private val nettyEpollLinuxX86             = nettyHttp.withName("netty-transport-native-epoll")    classifier "linux-x86_64"
  private val nettyEpollLinuxArm             = nettyEpollLinuxX86                                    classifier "linux-aarch_64"
  private val nettyIoUringLinuxX86           = nettyHttp.withName("netty-transport-native-io_uring") classifier "linux-x86_64"
  private val nettyIoUringLinuxArm           = nettyIoUringLinuxX86                                  classifier "linux-aarch_64"
  private val nettyHttp2                     = nettyHttp.withName("netty-codec-http2")
  private val nettyResolverNativeOsXX86      = nettyHttp.withName("netty-resolver-dns-native-macos") classifier "osx-x86_64"
  private val nettyResolverNativeOsXArm      = nettyResolverNativeOsXX86                             classifier "osx-aarch_64"
  private val nettyTcNative                  = nettyHttp.organization                                         % "netty-tcnative-classes"   % "2.0.74.Final"
  private val nettyTcNativeBoringSsl         = nettyTcNative.withName("netty-tcnative-boringssl-static")
  private val nettyTcNativeBoringSslLinuxX86 = nettyTcNativeBoringSsl                                classifier "linux-x86_64"
  private val nettyTcNativeBoringSslLinuxArm = nettyTcNativeBoringSsl                                classifier "linux-aarch_64"
  private val nettyTcNativeBoringSslOsXX86   = nettyTcNativeBoringSsl                                classifier "osx-x86_64"
  private val nettyTcNativeBoringSslOsXArm   = nettyTcNativeBoringSsl                                classifier "osx-aarch_64"
  private val nettyTcNativeBoringSslWindows  = nettyTcNativeBoringSsl                                classifier "windows-x86_64"
  private val brotli4j                       = "com.aayushatharva.brotli4j"                                   % "brotli4j"                 % "1.20.0"
  private val brotli4jLinuxX86               = brotli4j.withName("native-linux-x86_64")
  private val brotli4jLinuxArm               = brotli4j.withName("native-linux-aarch64")
  private val brotli4cOsXX86                 = brotli4j.withName("native-osx-x86_64")
  private val brotli4cOsXArm                 = brotli4j.withName("native-osx-aarch64")
  private val brotli4jWindows                = brotli4j.withName("native-windows-x86_64")
  private val config                         = "com.typesafe"                                                 % "config"                   % "1.4.5"
  private val saxon                          = "net.sf.saxon"                                                 % "Saxon-HE"                 % "12.9"
  private val xmlresolver                    = "org.xmlresolver"                                              % "xmlresolver"              % "6.0.19"
  private val xmlresolverData                = xmlresolver                                           classifier "data"
  private val slf4jApi                       = "org.slf4j"                                                    % "slf4j-api"                % "2.0.17"
  private val cfor                           = "io.github.metarank"                                          %% "cfor"                     % "0.3"
  private val scopt                          = "com.github.scopt"                                            %% "scopt"                    % "3.7.1"
  private val scalaLogging                   = "com.typesafe.scala-logging"                                  %% "scala-logging"            % "3.9.6"
  private val jackson                        = "com.fasterxml.jackson.core"                                   % "jackson-databind"         % "2.20.1"
  private val sfm = ("org.simpleflatmapper" % "lightning-csv" % "9.0.2")
    .exclude("org.simpleflatmapper", "ow2-asm")
  private val lagarto       = "org.jodd"                      % "jodd-lagarto"               % "6.0.6"
  private val joddUtil      = "org.jodd"                      % "jodd-util"                  % "6.3.0"
  private val jmespath      = "io.burt"                       % "jmespath-jackson"           % "0.6.0"
  private val redisClient   = "net.debasishg"                %% "redisclient"                % "3.42"
  private val commonsPool2  = "org.apache.commons"            % "commons-pool2"              % "2.12.1"
  private val testInterface = "org.scala-sbt"                 % "test-interface"             % "1.0"
  private val jmsApi        = "jakarta.jms"                   % "jakarta.jms-api"            % "3.1.0"
  private val logback       = "ch.qos.logback"                % "logback-classic"            % "1.5.20"
  private val tdigest       = "com.tdunning"                  % "t-digest"                   % "3.3"
  private val hdrHistogram  = "org.hdrhistogram"              % "HdrHistogram"               % "2.2.2"
  private val caffeine      = "com.github.ben-manes.caffeine" % "caffeine"                   % "3.2.3"
  private val bouncyCastle  = "io.gatling"                    % "gatling-recorder-bc-shaded" % "1.82.0"
  private val fastUuid      = "com.eatthepath"                % "fast-uuid"                  % "0.2.0"
  private val pebble        = "io.pebbletemplates"            % "pebble"                     % "3.2.4"
  private val jspecify      = "org.jspecify"                  % "jspecify"                   % "1.0.0"
  private val typetools     = "net.jodah"                     % "typetools"                  % "0.6.3"

  // Test dependencies
  private val scalaTest           = "org.scalatest"                  %% "scalatest"       % "3.2.19"   % Test
  private val scalaTestScalacheck = "org.scalatestplus"              %% "scalacheck-1-18" % "3.2.19.0" % Test
  private val scalaTestMockito    = scalaTestScalacheck.organization %% "mockito-5-18"    % "3.2.19.0" % Test
  private val scalaCheck          = "org.scalacheck"                 %% "scalacheck"      % "1.19.0"   % Test
  private val mockitoCore         = "org.mockito"                     % "mockito-core"    % "5.20.0"   % Test
  private val activemqBroker      = "org.apache.activemq"             % "activemq-broker" % "6.1.8"    % Test
  private val h2                  = "com.h2database"                  % "h2"              % "2.4.240"  % Test
  private val jmh                 = "org.openjdk.jmh"                 % "jmh-core"        % "1.37"     % Test

  private val junit                 = "org.junit.jupiter"    % "junit-jupiter-api"       % "6.0.1"        % Test
  private val junitEngine           = junit.withName("junit-jupiter-engine")
  private val junitPlatformLauncher = "org.junit.platform"   % "junit-platform-launcher" % junit.revision % Test
  private val jupiterInterface      = "com.github.sbt.junit" % "jupiter-interface"       % "0.17.0"       % Test
  private val jetty                 = "org.eclipse.jetty"    % "jetty-server"            % "12.1.3"       % Test
  private val jettyProxy            = jetty.organization     % "jetty-proxy"             % jetty.revision % Test

  private val testDeps = Seq(
    scalaTest,
    scalaTestScalacheck,
    scalaTestMockito,
    scalaCheck,
    mockitoCore
  )
  private val parserDeps = Seq(jackson, saxon, xmlresolver, xmlresolverData, lagarto, joddUtil, jmespath)

  // Dependencies by module

  val nettyUtilDependencies =
    Seq(
      gatlingSharedUtil,
      nettyBuffer,
      nettyEpollLinuxX86,
      nettyEpollLinuxArm,
      nettyIoUringLinuxX86,
      nettyIoUringLinuxArm,
      junit,
      junitEngine,
      junitPlatformLauncher,
      jupiterInterface
    )

  val commonsSharedUnstableDependencies = testDeps

  val commonsDependencies =
    Seq(gatlingSharedUtil, config, cfor, slf4jApi, scalaLogging, logback) ++ testDeps

  val jsonpathDependencies =
    Seq(gatlingSharedUtil, scalaParserCombinators, jackson) ++ testDeps

  def quicklensDependencies(scalaVersion: String) =
    Seq(scalaReflect(scalaVersion))

  val coreDependencies =
    Seq(
      gatlingSharedModel,
      gatlingSharedCli,
      sfm,
      caffeine,
      pebble,
      scalaParserCombinators,
      scopt,
      nettyHandler,
      nettyTcNative,
      nettyTcNativeBoringSsl,
      nettyTcNativeBoringSslLinuxX86,
      nettyTcNativeBoringSslLinuxArm,
      nettyTcNativeBoringSslOsXX86,
      nettyTcNativeBoringSslOsXArm,
      nettyTcNativeBoringSslWindows
    ) ++
      parserDeps ++ testDeps

  val defaultJavaDependencies =
    Seq(jspecify, junit, junitEngine, junitPlatformLauncher, jupiterInterface) ++ testDeps

  val coreJavaDependencies =
    Seq(typetools) ++ defaultJavaDependencies

  val redisDependencies = Seq(redisClient, commonsPool2) ++ testDeps

  val httpClientDependencies = Seq(
    gatlingSharedUtil,
    nettyHttp,
    nettyBuffer,
    nettyHandler,
    nettyProxy,
    nettyDns,
    nettyEpollLinuxX86,
    nettyEpollLinuxArm,
    nettyHttp2,
    nettyResolverNativeOsXX86,
    nettyResolverNativeOsXArm,
    nettyTcNative,
    nettyTcNativeBoringSsl,
    nettyTcNativeBoringSslLinuxX86,
    nettyTcNativeBoringSslLinuxArm,
    nettyTcNativeBoringSslOsXX86,
    nettyTcNativeBoringSslOsXArm,
    nettyTcNativeBoringSslWindows,
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
  )

  val httpDependencies = Seq(saxon, xmlresolver, xmlresolverData) ++ testDeps

  val jmsDependencies = Seq(jmsApi, fastUuid, activemqBroker) ++ testDeps

  val jdbcDependencies = h2 +: testDeps

  val chartsDependencies = tdigest +: testDeps

  val benchmarkDependencies = Seq(jmh)

  val recorderDependencies = Seq(gatlingSharedCli, scalaSwing, jackson, bouncyCastle, nettyHttp) ++ testDeps

  val testFrameworkDependencies = Seq(gatlingSharedCli, testInterface)
}
