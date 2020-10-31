import sbt._

object Dependencies {

  // Compile dependencies

  // format: OFF
  private def scalaReflect(version: String)  = "org.scala-lang"                       % "scala-reflect"                   % version
  private def scalaCompiler(version: String) = "org.scala-lang"                       % "scala-compiler"                  % version
  private val scalaSwing                     = "org.scala-lang.modules"              %% "scala-swing"                     % "2.1.1"
  private val scalaParserCombinators         = "org.scala-lang.modules"              %% "scala-parser-combinators"        % "1.1.2"
  private val java8Compat                    = "org.scala-lang.modules"              %% "scala-java8-compat"              % "0.9.1"
  private val netty                          = "io.netty"                             % "netty-codec-http"                % "4.1.51.Final"
  private val nettyBuffer                    = netty.organization                     % "netty-buffer"                    % netty.revision
  private val nettyHandler                   = netty.organization                     % "netty-handler"                   % netty.revision
  private val nettyMqtt                      = netty.organization                     % "netty-codec-mqtt"                % netty.revision
  private val nettyProxy                     = netty.organization                     % "netty-handler-proxy"             % netty.revision
  private val nettyDns                       = netty.organization                     % "netty-resolver-dns"              % netty.revision
  private val nettyEpoll                     = netty.organization                     % "netty-transport-native-epoll"    % netty.revision classifier "linux-x86_64"
  private val nettyHttp2                     = netty.organization                     % "netty-codec-http2"               % netty.revision
  private val nettyBoringSsl                 = netty.organization                     % "netty-tcnative-boringssl-static" % "2.0.34.Final"
  private val akka                           = "com.typesafe.akka"                   %% "akka-actor"                      % "2.6.8"
  private val akkaSlf4j                      = akka.organization                     %% "akka-slf4j"                      % akka.revision
  private val config                         = "com.typesafe"                         % "config"                          % "1.4.1"
  private val saxon                          = ("net.sf.saxon"                        % "Saxon-HE"                        % "10.3")
    .exclude("jline", "jline")
  private val slf4jApi                       = "org.slf4j"                            % "slf4j-api"                       % "1.7.30"
  private val spire                          = ("org.typelevel"                      %% "spire-macros"                    % "0.17.0")
    .exclude("org.typelevel", "machinist_2.12")
    .exclude("org.typelevel", "algebra_2.12")
  private val scopt                          = "com.github.scopt"                    %% "scopt"                           % "3.7.1"
  private val scalaLogging                   = "com.typesafe.scala-logging"          %% "scala-logging"                   % "3.9.2"
  private val jackson                        = "com.fasterxml.jackson.core"           % "jackson-databind"                % "2.11.2"
  private val sfm                            = ("org.simpleflatmapper"                % "lightning-csv"                   % "8.2.3")
    .exclude("org.simpleflatmapper", "ow2-asm")
  private val lagarto                        = "org.jodd"                             % "jodd-lagarto"                    % "6.0.2"
  private val jmespath                       = "io.burt"                              % "jmespath-jackson"                % "0.5.0"
  private val boopickle                      = "io.suzaku"                           %% "boopickle"                       % "1.3.3"
  private val redisClient                    = "net.debasishg"                       %% "redisclient"                     % "3.30"
  private val zinc                           = ("org.scala-sbt"                      %% "zinc"                            % "1.4.1")
    .exclude("org.scala-lang.modules", "scala-parser-combinators_2.12")
    .exclude("org.scala-lang.modules", "scala-xml_2.12")
    .exclude("org.scala-sbt", "launcher-interface")
    .exclude("org.scala-sbt", "sbinary_2.12")
    .exclude("org.scala-sbt", "zinc-ivy-integration_2.12")
    .exclude("com.eed3si9n", "sjson-new-core_2.12")
    .exclude("com.eed3si9n", "sjson-new-scalajson_2.12")
    .exclude("com.lihaoyi", "fastparse_2.12")
    .exclude("com.lmax", "disruptor")
    .exclude("org.apache.logging.log4j", "log4j-api")
    .exclude("org.apache.logging.log4j", "log4j-core")
  private val compilerBridge                 = zinc.organization                     %% "compiler-bridge"                 % zinc.revision
  private val testInterface                  = zinc.organization                      % "test-interface"                  % "1.0"
  private val jmsApi                         = "javax.jms"                            % "javax.jms-api"                   % "2.0.1"
  private val logback                        = "ch.qos.logback"                       % "logback-classic"                 % "1.2.3"
  private val tdigest                        = "com.tdunning"                         % "t-digest"                        % "3.1"
  private val hdrHistogram                   = "org.hdrhistogram"                     % "HdrHistogram"                    % "2.1.12"
  private val caffeine                       = "com.github.ben-manes.caffeine"        % "caffeine"                        % "2.8.5"
  private val bouncyCastle                   = "org.bouncycastle"                     % "bcpkix-jdk15on"                  % "1.66"
  private val quicklens                      = "com.softwaremill.quicklens"          %% "quicklens"                       % "1.6.1"
  private val fastUuid                       = "com.eatthepath"                       % "fast-uuid"                       % "0.1"
  private val pebble                         = "io.pebbletemplates"                   % "pebble"                          % "3.1.4"

  // Test dependencies
  private val scalaTest                      = "org.scalatest"                       %% "scalatest"                       % "3.2.2"             % "test"
  private val scalaTestScalacheck            = "org.scalatestplus"                   %% "scalacheck-1-14"                 % "3.2.2.0"           % "test"
  private val scalaTestMockito               = scalaTestScalacheck.organization      %% "mockito-3-4"                     % "3.2.2.0"           % "test"
  private val scalaCheck                     = "org.scalacheck"                      %% "scalacheck"                      % "1.15.0"            % "test"
  private val akkaTestKit                    = akka.organization                     %% "akka-testkit"                    % akka.revision       % "test"
  private val mockitoCore                    = "org.mockito"                          % "mockito-core"                    % "3.6.0"             % "test"
  private val activemqBroker                 = ("org.apache.activemq"                 % "activemq-broker"                 % "5.16.0"           % "test")
    .exclude("org.apache.geronimo.specs", "geronimo-jms_1.1_spec")
  private val h2                             = "com.h2database"                       % "h2"                              % "1.4.200"           % "test"
  private val jmh                            = "org.openjdk.jmh"                      % "jmh-core"                        % "1.25.2"

  private val junit                          = "org.junit.jupiter"                    % "junit-jupiter-api"               % "5.5.2"             % "test"
  private val jetty                          = "org.eclipse.jetty"                    % "jetty-server"                    % "9.4.33.v20201020"  % "test"
  private val jettyProxy                     = jetty.organization                     % "jetty-proxy"                     % jetty.revision      % "test"

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
    Seq(nettyBuffer, nettyEpoll, junit)

  def commonsSharedDependencies(scalaVersion: String) =
    Seq(scalaReflect(scalaVersion), spire, boopickle) ++ testDeps

  val commonsDependencies =
    Seq(config, java8Compat) ++ loggingDeps ++ testDeps

  val jsonpathDependencies =
    Seq(scalaParserCombinators, jackson) ++ testDeps

  val coreDependencies =
    Seq(
      akka,
      akkaSlf4j,
      sfm,
      java8Compat,
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
    junit,
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
