/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.core.config

import java.nio.charset.Charset
import java.util.ResourceBundle

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.duration._
import scala.io.Codec

import io.gatling.commons.util.ConfigHelper._
import io.gatling.commons.util.Ssl
import io.gatling.commons.util.StringHelper._
import io.gatling.core.ConfigKeys._
import io.gatling.core.stats.writer._

import com.typesafe.config.{ Config, ConfigFactory }
import com.typesafe.scalalogging.StrictLogging
import javax.net.ssl.{ KeyManagerFactory, TrustManagerFactory }

/**
 * Configuration loader of Gatling
 */
object GatlingConfiguration extends StrictLogging {

  private val GatlingDefaultsConfigFile = "gatling-defaults.conf"
  private val GatlingCustomConfigFile = "gatling.conf"
  private val GatlingCustomConfigFileOverrideSystemProperty = "gatling.conf.file"
  private val ActorSystemDefaultsConfigFile = "gatling-akka-defaults.conf"
  private val ActorSystemConfigFile = "gatling-akka.conf"

  def loadActorSystemConfiguration(): Config = {
    val classLoader = getClass.getClassLoader

    val defaultsConfig = ConfigFactory.parseResources(classLoader, ActorSystemDefaultsConfigFile)
    val customConfig = ConfigFactory.parseResources(classLoader, ActorSystemConfigFile)

    configChain(customConfig, defaultsConfig)
  }

  def loadForTest(props: mutable.Map[String, _ <: Any] = mutable.Map.empty): GatlingConfiguration = {

    val defaultsConfig = ConfigFactory.parseResources(getClass.getClassLoader, GatlingDefaultsConfigFile)
    val propertiesConfig = ConfigFactory.parseMap(props.asJava)
    val config = configChain(ConfigFactory.systemProperties, propertiesConfig, defaultsConfig)
    mapToGatlingConfig(config)
  }

  def load(props: mutable.Map[String, _ <: Any] = mutable.Map.empty): GatlingConfiguration = {
    sealed abstract class ObsoleteUsage(val message: String) { def path: String }
    case class Removed(path: String, advice: String) extends ObsoleteUsage(s"'$path' was removed, $advice.")
    case class Renamed(path: String, replacement: String) extends ObsoleteUsage(s"'$path' was renamed into $replacement.")

    def loadObsoleteUsagesFromBundle[T <: ObsoleteUsage](bundleName: String, creator: (String, String) => T): Vector[T] = {
      val bundle = ResourceBundle.getBundle(bundleName)
      bundle.getKeys.asScala.map(key => creator(key, bundle.getString(key))).toVector
    }

    def warnAboutRemovedProperties(config: Config): Unit = {
      val removedProperties = loadObsoleteUsagesFromBundle("config-removed", Removed.apply)
      val renamedProperties = loadObsoleteUsagesFromBundle("config-renamed", Renamed.apply)

      val obsoleteUsages =
        (removedProperties ++ renamedProperties).collect { case obs if config.hasPath(obs.path) => obs.message }

      if (obsoleteUsages.nonEmpty) {
        logger.error(
          s"""|Your gatling.conf file is outdated, some properties have been renamed or removed.
                |Please update (check gatling.conf in Gatling bundle, or gatling-defaults.conf in gatling-core jar).
                |Enabled obsolete properties:
                |${obsoleteUsages.mkString("\n")}""".stripMargin
        )
      }
    }

    val classLoader = getClass.getClassLoader

    val customConfigFile = sys.props.getOrElse(GatlingCustomConfigFileOverrideSystemProperty, GatlingCustomConfigFile)
    logger.info(s"Gatling will try to use '$customConfigFile' as custom config file.")

    val defaultsConfig = ConfigFactory.parseResources(classLoader, GatlingDefaultsConfigFile)
    val customConfig = ConfigFactory.parseResources(classLoader, customConfigFile)
    val propertiesConfig = ConfigFactory.parseMap(props.asJava)

    val config = configChain(ConfigFactory.systemProperties, customConfig, propertiesConfig, defaultsConfig)

    warnAboutRemovedProperties(config)

    mapToGatlingConfig(config)
  }

  private def mapToGatlingConfig(config: Config) =
    GatlingConfiguration(
      core = CoreConfiguration(
        version = ResourceBundle.getBundle("gatling-version").getString("version"),
        outputDirectoryBaseName = config.getString(core.OutputDirectoryBaseName).trimToOption,
        runDescription = config.getString(core.RunDescription).trimToOption,
        encoding = config.getString(core.Encoding),
        simulationClass = config.getString(core.SimulationClass).trimToOption,
        elFileBodiesCacheMaxCapacity = config.getLong(core.ElFileBodiesCacheMaxCapacity),
        rawFileBodiesCacheMaxCapacity = config.getLong(core.RawFileBodiesCacheMaxCapacity),
        rawFileBodiesInMemoryMaxSize = config.getLong(core.RawFileBodiesInMemoryMaxSize),
        pebbleFileBodiesCacheMaxCapacity = config.getLong(core.PebbleFileBodiesCacheMaxCapacity),
        shutdownTimeout = config.getLong(core.ShutdownTimeout),
        extract = ExtractConfiguration(
          regex = RegexConfiguration(
            cacheMaxCapacity = config.getLong(core.extract.regex.CacheMaxCapacity)
          ),
          xpath = XPathConfiguration(
            cacheMaxCapacity = config.getLong(core.extract.xpath.CacheMaxCapacity)
          ),
          jsonPath = JsonPathConfiguration(
            cacheMaxCapacity = config.getLong(core.extract.jsonPath.CacheMaxCapacity),
            preferJackson = config.getBoolean(core.extract.jsonPath.PreferJackson)
          ),
          css = CssConfiguration(
            cacheMaxCapacity = config.getLong(core.extract.css.CacheMaxCapacity)
          )
        ),
        directory = DirectoryConfiguration(
          simulations = config.getString(core.directory.Simulations),
          resources = config.getString(core.directory.Resources),
          binaries = config.getString(core.directory.Binaries).trimToOption,
          reportsOnly = config.getString(core.directory.ReportsOnly).trimToOption,
          results = config.getString(core.directory.Results)
        )
      ),
      charting = ChartingConfiguration(
        noReports = config.getBoolean(charting.NoReports),
        maxPlotsPerSeries = config.getInt(charting.MaxPlotPerSeries),
        useGroupDurationMetric = config.getBoolean(charting.UseGroupDurationMetric),
        indicators = IndicatorsConfiguration(
          lowerBound = config.getInt(charting.indicators.LowerBound),
          higherBound = config.getInt(charting.indicators.HigherBound),
          percentile1 = config.getDouble(charting.indicators.Percentile1),
          percentile2 = config.getDouble(charting.indicators.Percentile2),
          percentile3 = config.getDouble(charting.indicators.Percentile3),
          percentile4 = config.getDouble(charting.indicators.Percentile4)
        )
      ),
      http = HttpConfiguration(
        fetchedCssCacheMaxCapacity = config.getLong(http.FetchedCssCacheMaxCapacity),
        fetchedHtmlCacheMaxCapacity = config.getLong(http.FetchedHtmlCacheMaxCapacity),
        perUserCacheMaxCapacity = config.getInt(http.PerUserCacheMaxCapacity),
        warmUpUrl = config.getString(http.WarmUpUrl).trimToOption,
        enableGA = config.getBoolean(http.EnableGA),
        ssl = {
          SslConfiguration(
            keyManagerFactory = {
              val storeType = config.getString(http.ssl.keyStore.Type).trimToOption
              val storeFile = config.getString(http.ssl.keyStore.File).trimToOption
              val storePassword = config.getString(http.ssl.keyStore.Password)
              val storeAlgorithm = config.getString(http.ssl.keyStore.Algorithm).trimToOption
              storeFile.map(Ssl.newKeyManagerFactory(storeType, _, storePassword, storeAlgorithm))
            },
            trustManagerFactory = {
              val storeType = config.getString(http.ssl.trustStore.Type).trimToOption
              val storeFile = config.getString(http.ssl.trustStore.File).trimToOption
              val storePassword = config.getString(http.ssl.trustStore.Password)
              val storeAlgorithm = config.getString(http.ssl.trustStore.Algorithm).trimToOption
              storeFile.map(Ssl.newTrustManagerFactory(storeType, _, storePassword, storeAlgorithm))
            }
          )
        },
        advanced = AdvancedConfiguration(
          connectTimeout = config.getInt(http.ahc.ConnectTimeout) millis,
          handshakeTimeout = config.getInt(http.ahc.HandshakeTimeout) millis,
          pooledConnectionIdleTimeout = config.getInt(http.ahc.PooledConnectionIdleTimeout) millis,
          maxRetry = config.getInt(http.ahc.MaxRetry),
          requestTimeout = config.getInt(http.ahc.RequestTimeout) millis,
          enableSni = config.getBoolean(http.ahc.EnableSni),
          enableHostnameVerification = {
            val enable = config.getBoolean(http.ahc.EnableHostnameVerification)
            if (!enable) {
              System.setProperty("jdk.tls.allowUnsafeServerCertChange", "true")
              System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true")
            }
            enable
          },
          useInsecureTrustManager = config.getBoolean(http.ahc.UseInsecureTrustManager),
          sslEnabledProtocols = config.getStringList(http.ahc.SslEnabledProtocols).asScala.toList,
          sslEnabledCipherSuites = config.getStringList(http.ahc.SslEnabledCipherSuites).asScala.toList,
          sslSessionCacheSize = config.getInt(http.ahc.SslSessionCacheSize),
          sslSessionTimeout = config.getInt(http.ahc.SslSessionTimeout) seconds,
          useOpenSsl = config.getBoolean(http.ahc.UseOpenSsl),
          useNativeTransport = config.getBoolean(http.ahc.UseNativeTransport),
          enableZeroCopy = config.getBoolean(http.ahc.EnableZeroCopy),
          tcpNoDelay = config.getBoolean(http.ahc.TcpNoDelay),
          soReuseAddress = config.getBoolean(http.ahc.SoReuseAddress),
          allocator = config.getString(http.ahc.Allocator),
          maxThreadLocalCharBufferSize = config.getInt(http.ahc.MaxThreadLocalCharBufferSize)
        ),
        dns = DnsConfiguration(
          queryTimeout = config.getInt(http.dns.QueryTimeout) millis,
          maxQueriesPerResolve = config.getInt(http.dns.MaxQueriesPerResolve)
        )
      ),
      jms = JmsConfiguration(
        replyTimeoutScanPeriod = config.getLong(jms.ReplyTimeoutScanPeriod) millis
      ),
      data = DataConfiguration(
        dataWriters = config.getStringList(data.Writers).asScala.flatMap(DataWriterType.findByName),
        console = ConsoleDataWriterConfiguration(
          light = config.getBoolean(data.console.Light),
          writePeriod = config.getInt(data.console.WritePeriod) seconds
        ),
        file = FileDataWriterConfiguration(
          bufferSize = config.getInt(data.file.BufferSize)
        ),
        leak = LeakDataWriterConfiguration(
          noActivityTimeout = config.getInt(data.leak.NoActivityTimeout) seconds
        ),
        graphite = GraphiteDataWriterConfiguration(
          light = config.getBoolean(data.graphite.Light),
          host = config.getString(data.graphite.Host),
          port = config.getInt(data.graphite.Port),
          protocol = TransportProtocol(config.getString(data.graphite.Protocol).trim),
          rootPathPrefix = config.getString(data.graphite.RootPathPrefix),
          bufferSize = config.getInt(data.graphite.BufferSize),
          writePeriod = config.getInt(data.graphite.WritePeriod) seconds
        )
      ),
      // [fl]
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      // [fl]
      config = config
    )
}

case class CoreConfiguration(
    version:                          String,
    outputDirectoryBaseName:          Option[String],
    runDescription:                   Option[String],
    encoding:                         String,
    simulationClass:                  Option[String],
    extract:                          ExtractConfiguration,
    directory:                        DirectoryConfiguration,
    elFileBodiesCacheMaxCapacity:     Long,
    rawFileBodiesCacheMaxCapacity:    Long,
    rawFileBodiesInMemoryMaxSize:     Long,
    pebbleFileBodiesCacheMaxCapacity: Long,
    shutdownTimeout:                  Long
) {

  val charset: Charset = Charset.forName(encoding)
  val codec: Codec = charset
}

case class ExtractConfiguration(
    regex:    RegexConfiguration,
    xpath:    XPathConfiguration,
    jsonPath: JsonPathConfiguration,
    css:      CssConfiguration
)

case class RegexConfiguration(
    cacheMaxCapacity: Long
)

case class XPathConfiguration(
    cacheMaxCapacity: Long
)

case class JsonPathConfiguration(
    cacheMaxCapacity: Long,
    preferJackson:    Boolean
)

case class CssConfiguration(
    cacheMaxCapacity: Long
)

case class DirectoryConfiguration(
    simulations: String,
    resources:   String,
    binaries:    Option[String],
    reportsOnly: Option[String],
    results:     String
)

case class ChartingConfiguration(
    noReports:              Boolean,
    maxPlotsPerSeries:      Int,
    useGroupDurationMetric: Boolean,
    indicators:             IndicatorsConfiguration
)

case class IndicatorsConfiguration(
    lowerBound:  Int,
    higherBound: Int,
    percentile1: Double,
    percentile2: Double,
    percentile3: Double,
    percentile4: Double
)

case class HttpConfiguration(
    fetchedCssCacheMaxCapacity:  Long,
    fetchedHtmlCacheMaxCapacity: Long,
    perUserCacheMaxCapacity:     Int,
    warmUpUrl:                   Option[String],
    enableGA:                    Boolean,
    ssl:                         SslConfiguration,
    advanced:                    AdvancedConfiguration,
    dns:                         DnsConfiguration
)

case class JmsConfiguration(
    replyTimeoutScanPeriod: FiniteDuration
)

case class AdvancedConfiguration(
    connectTimeout:               FiniteDuration,
    handshakeTimeout:             FiniteDuration,
    pooledConnectionIdleTimeout:  FiniteDuration,
    maxRetry:                     Int,
    requestTimeout:               FiniteDuration,
    enableSni:                    Boolean,
    enableHostnameVerification:   Boolean,
    useInsecureTrustManager:      Boolean,
    sslEnabledProtocols:          List[String],
    sslEnabledCipherSuites:       List[String],
    sslSessionCacheSize:          Int,
    sslSessionTimeout:            FiniteDuration,
    useOpenSsl:                   Boolean,
    useNativeTransport:           Boolean,
    enableZeroCopy:               Boolean,
    tcpNoDelay:                   Boolean,
    soReuseAddress:               Boolean,
    allocator:                    String,
    maxThreadLocalCharBufferSize: Int

)

case class DnsConfiguration(
    queryTimeout:         FiniteDuration,
    maxQueriesPerResolve: Int
)

case class SslConfiguration(
    keyManagerFactory:   Option[KeyManagerFactory],
    trustManagerFactory: Option[TrustManagerFactory]
)

case class DataConfiguration(
    dataWriters: Seq[DataWriterType],
    file:        FileDataWriterConfiguration,
    leak:        LeakDataWriterConfiguration,
    console:     ConsoleDataWriterConfiguration,
    graphite:    GraphiteDataWriterConfiguration
) {

  def fileDataWriterEnabled: Boolean = dataWriters.contains(FileDataWriterType)
}

case class FileDataWriterConfiguration(
    bufferSize: Int
)

case class LeakDataWriterConfiguration(
    noActivityTimeout: FiniteDuration
)

case class ConsoleDataWriterConfiguration(
    light:       Boolean,
    writePeriod: FiniteDuration
)

case class GraphiteDataWriterConfiguration(
    light:          Boolean,
    host:           String,
    port:           Int,
    protocol:       TransportProtocol,
    rootPathPrefix: String,
    bufferSize:     Int,
    writePeriod:    FiniteDuration
)

// [fl]
//
//
//
//
//
//
//
//
// [fl]

case class GatlingConfiguration(
    core:     CoreConfiguration,
    charting: ChartingConfiguration,
    http:     HttpConfiguration,
    jms:      JmsConfiguration,
    data:     DataConfiguration,
    // [fl]
    //
    // [fl]
    config: Config
) {
  def resolve[T](value: T): T = value

  // [fl]
  //
  //
  // [fl]
}
