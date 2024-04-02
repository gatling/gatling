import sbt._

object Dependencies {
  // Compile dependencies

  // format: OFF
  private val gatlingSharedUtil              = "io.gatling"                          %% "gatling-shared-util"               % "0.0.8"
  private val gatlingSharedModel             = "io.gatling"                          %% "gatling-shared-model"              % "0.0.6"
  private val scalaSwing                     = "org.scala-lang.modules"              %% "scala-swing"                       % "3.0.0"
  private val scalaParserCombinators         = "org.scala-lang.modules"              %% "scala-parser-combinators"          % "2.3.0"
  private val netty                          = "io.netty"                             % "netty-codec-http"                  % "4.1.108.Final"
  private val nettyBuffer                    = netty.withName("netty-buffer")
  private val nettyHandler                   = netty.withName("netty-handler")
  private val nettyProxy                     = netty.withName("netty-handler-proxy")
  private val nettyDns                       = netty.withName("netty-resolver-dns")
  private val nettyEpollLinuxX86             = netty.withName("netty-transport-native-epoll")                               classifier "linux-x86_64"
  private val nettyEpollLinuxArm             = netty.withName("netty-transport-native-epoll")                               classifier "linux-aarch_64"
  private val nettyIoUringLinuxX86           = "io.netty.incubator"                   % "netty-incubator-transport-native-io_uring" % "0.0.25.Final" classifier "linux-x86_64"
  private val nettyIoUringLinuxArm           = nettyIoUringLinuxX86                                                                                  classifier "linux-aarch_64"
  private val nettyHttp2                     = netty.withName("netty-codec-http2")
  private val nettyResolverNativeOsXX86      = netty.withName("netty-resolver-dns-native-macos") classifier "osx-x86_64"
  private val nettyResolverNativeOsXArm      = nettyResolverNativeOsXX86                                 classifier "osx-aarch_64"
  private val nettyTcNative                  = netty.organization                     % "netty-tcnative-classes"            % "2.0.65.Final"
  private val nettyTcNativeBoringSsl         = nettyTcNative.withName("netty-tcnative-boringssl-static")
  private val nettyTcNativeBoringSslLinuxX86 = nettyTcNativeBoringSsl  classifier "linux-x86_64"
  private val nettyTcNativeBoringSslLinuxArm = nettyTcNativeBoringSsl  classifier "linux-aarch_64"
  private val nettyTcNativeBoringSslOsXX86   = nettyTcNativeBoringSsl  classifier "osx-x86_64"
  private val nettyTcNativeBoringSslOsXArm   = nettyTcNativeBoringSsl  classifier "osx-aarch_64"
  private val nettyTcNativeBoringSslWindows  = nettyTcNativeBoringSsl  classifier "windows-x86_64"
  private val brotli4j                       = "com.aayushatharva.brotli4j"           % "brotli4j"                          % "1.16.0"
  private val brotli4jLinuxX86               = brotli4j.withName("native-linux-x86_64")
  private val brotli4jLinuxArm               = brotli4j.withName("native-linux-aarch64")
  private val brotli4cOsXX86                 = brotli4j.withName("native-osx-x86_64")
  private val brotli4cOsXArm                 = brotli4j.withName("native-osx-aarch64")
  private val brotli4jWindows                = brotli4j.withName("native-windows-x86_64")
  private val akka                           = "com.typesafe.akka"                   %% "akka-actor"                        % "2.6.20"
  private val akkaSlf4j                      = akka.withName("akka-slf4j")
  private val config                         = "com.typesafe"                         % "config"                            % "1.4.3"
  private val saxon                          = "net.sf.saxon"                         % "Saxon-HE"                          % "10.6"
  private val slf4jApi                       = "org.slf4j"                            % "slf4j-api"                         % "2.0.12"
  private val cfor                           = "io.github.metarank"                  %% "cfor"                              % "0.3"
  private val scopt                          = "com.github.scopt"                    %% "scopt"                             % "3.7.1"
  private val scalaLogging                   = "com.typesafe.scala-logging"          %% "scala-logging"                     % "3.9.5"
  private val jackson                        = "com.fasterxml.jackson.core"           % "jackson-databind"                  % "2.17.0"
  private val sfm                            = ("org.simpleflatmapper"                % "lightning-csv"                     % "8.2.3")
    .exclude("org.simpleflatmapper", "ow2-asm")
  private val lagarto                        = "org.jodd"                             % "jodd-lagarto"                      % "6.0.6"
  private val joddUtil                       = "org.jodd"                             % "jodd-util"                         % "6.2.2"
  private val jmespath                       = "io.burt"                              % "jmespath-jackson"                  % "0.6.0"
  private val boopickle                      = "io.suzaku"                           %% "boopickle"                         % "1.4.0"
  private val redisClient                    = "net.debasishg"                       %% "redisclient"                       % "3.42"
  private val testInterface                  = "org.scala-sbt"                        % "test-interface"                    % "1.0"
  private val jmsApi                         = "javax.jms"                            % "javax.jms-api"                     % "2.0.1"
  private val logback                        = "ch.qos.logback"                       % "logback-classic"                   % "1.5.3"
  private val tdigest                        = "com.tdunning"                         % "t-digest"                          % "3.1"
  private val hdrHistogram                   = "org.hdrhistogram"                     % "HdrHistogram"                      % "2.1.12"
  private val caffeine                       = "com.github.ben-manes.caffeine"        % "caffeine"                          % "3.1.8"
  private val bouncyCastle                   = "io.gatling"                           % "gatling-recorder-bc-shaded"        % "1.77.0"
  private val quicklens                      = "com.softwaremill.quicklens"          %% "quicklens"                         % "1.9.7"
  private val fastUuid                       = "com.eatthepath"                       % "fast-uuid"                         % "0.2.0"
  private val pebble                         = "io.pebbletemplates"                   % "pebble"                            % "3.2.2"
  private val spotbugs                       = "com.github.spotbugs"                  % "spotbugs-annotations"              % "4.8.3"
  private val typetools                      = "net.jodah"                            % "typetools"                         % "0.6.3"

  // Test dependencies
  private val scalaTest                      = "org.scalatest"                       %% "scalatest"                         % "3.2.18"            % Test
  private val scalaTestScalacheck            = "org.scalatestplus"                   %% "scalacheck-1-16"                   % "3.2.14.0"          % Test
  private val scalaTestMockito               = scalaTestScalacheck.organization      %% "mockito-3-4"                       % "3.2.10.0"          % Test
  private val scalaCheck                     = "org.scalacheck"                      %% "scalacheck"                        % "1.17.0"            % Test
  private val akkaTestKit                    = akka.withName("akka-testkit")                                                                      % Test
  private val mockitoCore                    = "org.mockito"                          % "mockito-core"                      % "4.11.0"            % Test
  private val activemqBroker                 = ("org.apache.activemq"                 % "activemq-broker"                   % "5.18.3"            % Test)
    .exclude("org.apache.geronimo.specs", "geronimo-jms_1.1_spec")
  private val h2                             = "com.h2database"                       % "h2"                                % "2.2.224"           % Test
  private val jmh                            = "org.openjdk.jmh"                      % "jmh-core"                          % "1.27"              % Test

  private val junit                          = "org.junit.jupiter"                    % "junit-jupiter-api"                 % "5.10.1"            % Test
  private val junitEngine                    = junit.withName("junit-jupiter-engine")
  private val jupiterInterface               = "net.aichler"                          % "jupiter-interface"                 % "0.11.1"            % Test

  private val jetty                          = "org.eclipse.jetty"                    % "jetty-server"                      % "9.4.54.v20240208"  % Test
  private val jettyProxy                     = jetty.organization                     % "jetty-proxy"                       % jetty.revision      % Test

  // Docs dependencies
  private val commonsLang                    = "org.apache.commons"                   % "commons-lang3"                     % "3.14.0"
  private val commonsCodec                   = "commons-codec"                        % "commons-codec"                     % "1.16.1"

  // format: ON
  private val loggingDeps = Seq(slf4jApi, scalaLogging, logback)
  private val testDeps = Seq(
    scalaTest,
    scalaTestScalacheck,
    scalaTestMockito,
    scalaCheck,
    akkaTestKit,
    mockitoCore
  )
  private val parserDeps = Seq(jackson, saxon, lagarto, joddUtil, jmespath)

  // Dependencies by module

  private val gatlingGrpcVersion = "3.10.5"
  private val gatlingMqttVersion = "3.10.5.1"
  val docSamplesDependencies =
    Seq(
      commonsLang,
      commonsCodec,
      "io.gatling" % "gatling-grpc"      % gatlingGrpcVersion,
      "io.gatling" % "gatling-grpc-java" % gatlingGrpcVersion,
      "io.gatling" % "gatling-mqtt"      % gatlingMqttVersion,
      "io.gatling" % "gatling-mqtt-java" % gatlingMqttVersion
    )

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
      jupiterInterface
    )

  val sharedModelDependencies =
    Seq(gatlingSharedUtil, boopickle) ++ testDeps

  val commonsSharedUnstableDependencies = testDeps

  val commonsDependencies =
    Seq(gatlingSharedUtil, config, cfor) ++ loggingDeps ++ testDeps

  val jsonpathDependencies =
    Seq(gatlingSharedUtil, scalaParserCombinators, jackson) ++ testDeps

  val coreDependencies =
    Seq(
      gatlingSharedModel,
      akka,
      akkaSlf4j,
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
      nettyTcNativeBoringSslWindows,
      quicklens
    ) ++
      parserDeps ++ testDeps

  val defaultJavaDependencies =
    Seq(spotbugs, junit, junitEngine, jupiterInterface) ++ testDeps

  val coreJavaDependencies =
    Seq(typetools) ++ defaultJavaDependencies

  val redisDependencies = redisClient +: testDeps

  val httpClientDependencies = Seq(
    gatlingSharedUtil,
    netty,
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
    jupiterInterface,
    jetty,
    jettyProxy
  ) ++ loggingDeps

  val httpDependencies = Seq(saxon) ++ testDeps

  val jmsDependencies = Seq(jmsApi, fastUuid, activemqBroker) ++ testDeps

  val jdbcDependencies = h2 +: testDeps

  val chartsDependencies = tdigest +: testDeps

  val graphiteDependencies = hdrHistogram +: testDeps

  val benchmarkDependencies = Seq(jmh)

  val recorderDependencies = Seq(scalaSwing, jackson, bouncyCastle, netty, akka, quicklens) ++ testDeps

  val testFrameworkDependencies = Seq(testInterface)

  val docDependencies = Seq(activemqBroker)
}
