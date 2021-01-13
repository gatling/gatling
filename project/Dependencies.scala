import sbt._

object Dependencies {

  // Compile dependencies

  // format: OFF
  private def scalaReflect(version: String)  = "org.scala-lang"                       % "scala-reflect"                   % version
  private def scalaCompiler(version: String) = ("org.scala-lang"                      % "scala-compiler"                  % version)
    .exclude("org.jline", "jline")
  private val scalaSwing                     = "org.scala-lang.modules"              %% "scala-swing"                     % "3.0.0"
  private val scalaParserCombinators         = "org.scala-lang.modules"              %% "scala-parser-combinators"        % "1.1.2"
  private val netty                          = "io.netty"                             % "netty-codec-http"                % "4.1.58.Final"
  private val nettyBuffer                    = netty.organization                     % "netty-buffer"                    % netty.revision
  private val nettyHandler                   = netty.organization                     % "netty-handler"                   % netty.revision
  private val nettyMqtt                      = netty.organization                     % "netty-codec-mqtt"                % netty.revision
  private val nettyProxy                     = netty.organization                     % "netty-handler-proxy"             % netty.revision
  private val nettyDns                       = netty.organization                     % "netty-resolver-dns"              % netty.revision
  private val nettyEpoll                     = netty.organization                     % "netty-transport-native-epoll"    % netty.revision classifier "linux-x86_64"
  private val nettyHttp2                     = netty.organization                     % "netty-codec-http2"               % netty.revision
  private val nettyBoringSsl                 = netty.organization                     % "netty-tcnative-boringssl-static" % "2.0.36.Final"
  private val brotli4j                       = "com.aayushatharva.brotli4j"           % "brotli4j"                        % "1.2.2"
  private val brotli4jMacOs                  = brotli4j.withName("native-macos_x86-64")
  private val brotli4jLinux                  = brotli4j.withName("native-linux_x86-64")
  private val brotli4jWindows                = brotli4j.withName("native-windows_x86-64")
  private val akka                           = "com.typesafe.akka"                   %% "akka-actor"                      % "2.6.10"
  private val akkaSlf4j                      = akka.organization                     %% "akka-slf4j"                      % akka.revision
  private val config                         = "com.typesafe"                         % "config"                          % "1.4.1"
  private val saxon                          = "net.sf.saxon"                        % "Saxon-HE"                        % "10.3"
  private val slf4jApi                       = "org.slf4j"                            % "slf4j-api"                       % "1.7.30"
  private val spire                          = ("org.typelevel"                      %% "spire-macros"                    % "0.17.0")
    .exclude("org.typelevel", "machinist_2.13")
    .exclude("org.typelevel", "algebra_2.13")
    .exclude("org.scala-lang.modules", "scala-collection-compat_2.13")
  private val scopt                          = "com.github.scopt"                    %% "scopt"                           % "3.7.1"
  private val scalaLogging                   = "com.typesafe.scala-logging"          %% "scala-logging"                   % "3.9.2"
  private val jackson                        = "com.fasterxml.jackson.core"           % "jackson-databind"                % "2.12.1"
  private val sfm                            = ("org.simpleflatmapper"                % "lightning-csv"                   % "8.2.3")
    .exclude("org.simpleflatmapper", "ow2-asm")
  private val lagarto                        = "org.jodd"                             % "jodd-lagarto"                    % "6.0.3"
  private val jmespath                       = "io.burt"                              % "jmespath-jackson"                % "0.5.0"
  private val boopickle                      = "io.suzaku"                           %% "boopickle"                       % "1.3.3"
  private val redisClient                    = "net.debasishg"                       %% "redisclient"                     % "3.30"
  private val zinc                           = ("org.scala-sbt"                      %% "zinc"                            % "1.4.4")
    .exclude("org.jline", "jline")
    .exclude("org.scala-sbt.jline3", "jline-terminal")
    .exclude("org.jline", "jline-terminal-jna")
    .exclude("org.jline", "jline-terminal-jansi")
    .exclude("org.scala-sbt.jline", "jline")
    .exclude("org.scala-lang.modules", "scala-parser-combinators_2.13")
    .exclude("org.scala-lang.modules", "scala-xml_2.13")
    .exclude("org.scala-sbt", "launcher-interface")
    .exclude("org.scala-sbt", "sbinary_2.13")
    .exclude("org.scala-sbt", "zinc-ivy-integration_2.13")
    .exclude("com.eed3si9n", "sjson-new-core_2.13")
    .exclude("com.eed3si9n", "sjson-new-scalajson_2.13")
    .exclude("com.lihaoyi", "fastparse_2.13")
    .exclude("com.lmax", "disruptor")
    .exclude("org.apache.logging.log4j", "log4j-api")
    .exclude("org.apache.logging.log4j", "log4j-core")
  private val compilerBridge                 = zinc.organization                     %% "compiler-bridge"                 % zinc.revision
  private val testInterface                  = zinc.organization                      % "test-interface"                  % "1.0"
  private val jmsApi                         = "javax.jms"                            % "javax.jms-api"                   % "2.0.1"
  private val logback                        = "ch.qos.logback"                       % "logback-classic"                 % "1.2.3"
  private val tdigest                        = "com.tdunning"                         % "t-digest"                        % "3.1"
  private val hdrHistogram                   = "org.hdrhistogram"                     % "HdrHistogram"                    % "2.1.12"
  private val caffeine                       = "com.github.ben-manes.caffeine"        % "caffeine"                        % "2.8.8"
  private val bouncyCastle                   = "org.bouncycastle"                     % "bcpkix-jdk15on"                  % "1.68"
  private val quicklens                      = "com.softwaremill.quicklens"          %% "quicklens"                       % "1.6.1"
  private val fastUuid                       = "com.eatthepath"                       % "fast-uuid"                       % "0.1"
  private val pebble                         = "io.pebbletemplates"                   % "pebble"                          % "3.1.4"

  // Test dependencies
  private val scalaTest                      = "org.scalatest"                       %% "scalatest"                       % "3.2.3"             % Test
  private val scalaTestScalacheck            = "org.scalatestplus"                   %% "scalacheck-1-15"                 % "3.2.3.0"           % Test
  private val scalaTestMockito               = scalaTestScalacheck.organization      %% "mockito-3-4"                     % "3.2.3.0"           % Test
  private val scalaCheck                     = "org.scalacheck"                      %% "scalacheck"                      % "1.15.2"            % Test
  private val akkaTestKit                    = akka.organization                     %% "akka-testkit"                    % akka.revision       % Test
  private val mockitoCore                    = "org.mockito"                          % "mockito-core"                    % "3.7.0"             % Test
  private val activemqBroker                 = ("org.apache.activemq"                 % "activemq-broker"                 % "5.16.0"            % Test)
    .exclude("org.apache.geronimo.specs", "geronimo-jms_1.1_spec")
  private val h2                             = "com.h2database"                       % "h2"                              % "1.4.200"           % Test
  private val jmh                            = "org.openjdk.jmh"                      % "jmh-core"                        % "1.27"

  private val junit                          = "org.junit.jupiter"                    % "junit-jupiter-api"               % "5.5.2"             % Test
  private val jupiterInterface               = "net.aichler"                          % "jupiter-interface"               % "0.8.3"             % Test

  private val jetty                          = "org.eclipse.jetty"                    % "jetty-server"                    % "9.4.35.v20201120"  % Test
  private val jettyProxy                     = jetty.organization                     % "jetty-proxy"                     % jetty.revision      % Test

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
  private val parserDeps = Seq(jackson, saxon, lagarto, jmespath)

  // Dependencies by module

  val nettyUtilDependencies =
    Seq(nettyBuffer, nettyEpoll, junit, jupiterInterface)

  def commonsSharedDependencies(scalaVersion: String) =
    Seq(scalaReflect(scalaVersion), boopickle) ++ testDeps

  val commonsSharedUnstableDependencies = testDeps

  val commonsDependencies =
    Seq(config, spire) ++ loggingDeps ++ testDeps

  val jsonpathDependencies =
    Seq(scalaParserCombinators, jackson) ++ testDeps

  val coreDependencies =
    Seq(
      akka,
      akkaSlf4j,
      sfm,
      caffeine,
      pebble,
      scalaParserCombinators,
      scopt,
      nettyHandler,
      quicklens
    ) ++
      parserDeps ++ testDeps

  val redisDependencies = redisClient +: testDeps

  val httpClientDependencies = Seq(
    netty,
    nettyBuffer,
    nettyHandler,
    nettyProxy,
    nettyDns,
    nettyEpoll,
    nettyHttp2,
    nettyBoringSsl,
    brotli4j,
    brotli4jLinux,
    brotli4jMacOs,
    brotli4jWindows,
    junit,
    jupiterInterface,
    jetty,
    jettyProxy
  ) ++ loggingDeps

  val httpDependencies = Seq(saxon) ++ testDeps

  val jmsDependencies = Seq(jmsApi, fastUuid, activemqBroker) ++ testDeps

  val jdbcDependencies = h2 +: testDeps

  val mqttDependencies = Seq(nettyHandler, nettyMqtt, nettyEpoll)

  val chartsDependencies = tdigest +: testDeps

  val graphiteDependencies = hdrHistogram +: testDeps

  val benchmarkDependencies = Seq(jmh)

  def compilerDependencies(scalaVersion: String) =
    Seq(
      scalaCompiler(scalaVersion),
      scalaReflect(scalaVersion),
      config,
      slf4jApi,
      logback,
      zinc,
      compilerBridge,
      scopt
    )

  val recorderDependencies = Seq(scalaSwing, jackson, bouncyCastle, netty, akka) ++ testDeps

  val testFrameworkDependencies = Seq(testInterface)

  val docDependencies = Seq(activemqBroker)
}
