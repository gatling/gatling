/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
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
import java.nio.file.{ Path, Paths }
import java.util.ResourceBundle

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.duration._

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

  def loadForTest(): GatlingConfiguration = loadForTest(mutable.Map.empty)

  def loadForTest(props: mutable.Map[String, _ <: Any]): GatlingConfiguration = {

    val defaultsConfig = ConfigFactory.parseResources(getClass.getClassLoader, GatlingDefaultsConfigFile)
    val propertiesConfig = ConfigFactory.parseMap(props.asJava)
    val config = configChain(ConfigFactory.systemProperties, propertiesConfig, defaultsConfig)
    mapToGatlingConfig(config)
  }

  def load(props: mutable.Map[String, _ <: Any]): GatlingConfiguration = {
    sealed abstract class ObsoleteUsage(val message: String) { def path: String }
    final case class Removed(path: String, advice: String) extends ObsoleteUsage(s"'$path' was removed, $advice.")
    final case class Renamed(path: String, replacement: String) extends ObsoleteUsage(s"'$path' was renamed into $replacement.")

    def loadObsoleteUsagesFromBundle[T <: ObsoleteUsage](bundleName: String, creator: (String, String) => T): Seq[ObsoleteUsage] = {
      val bundle = ResourceBundle.getBundle(bundleName)
      bundle.getKeys.asScala.map(key => creator(key, bundle.getString(key))).toVector
    }

    def warnAboutRemovedProperties(config: Config): Unit = {
      val removedProperties = loadObsoleteUsagesFromBundle("config-removed", Removed.apply)
      val renamedProperties = loadObsoleteUsagesFromBundle("config-renamed", Renamed.apply)

      val obsoleteUsages =
        (removedProperties ++ renamedProperties).collect { case obs if config.hasPath(obs.path) => obs.message }

      if (obsoleteUsages.nonEmpty) {
        logger.warn(
          s"""|Your Gatling configuration options are outdated, some properties have been renamed or removed.
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

  private def coreConfiguration(config: Config) =
    new CoreConfiguration(
      version = ResourceBundle.getBundle("gatling-version").getString("version"),
      outputDirectoryBaseName = config.getString(core.OutputDirectoryBaseName).trimToOption,
      runDescription = config.getString(core.RunDescription).trimToOption,
      encoding = config.getString(core.Encoding),
      simulationClass = config.getString(core.SimulationClass).trimToOption,
      elFileBodiesCacheMaxCapacity = config.getLong(core.ElFileBodiesCacheMaxCapacity),
      rawFileBodiesCacheMaxCapacity = config.getLong(core.RawFileBodiesCacheMaxCapacity),
      rawFileBodiesInMemoryMaxSize = config.getLong(core.RawFileBodiesInMemoryMaxSize),
      pebbleFileBodiesCacheMaxCapacity = config.getLong(core.PebbleFileBodiesCacheMaxCapacity),
      feederAdaptiveLoadModeThreshold = config.getLong(core.FeederAdaptiveLoadModeThreshold) * 1048576,
      shutdownTimeout = config.getLong(core.ShutdownTimeout),
      extract = new ExtractConfiguration(
        regex = new RegexConfiguration(
          cacheMaxCapacity = config.getLong(core.extract.regex.CacheMaxCapacity)
        ),
        xpath = new XPathConfiguration(
          cacheMaxCapacity = config.getLong(core.extract.xpath.CacheMaxCapacity)
        ),
        jsonPath = new JsonPathConfiguration(
          cacheMaxCapacity = config.getLong(core.extract.jsonPath.CacheMaxCapacity)
        ),
        css = new CssConfiguration(
          cacheMaxCapacity = config.getLong(core.extract.css.CacheMaxCapacity)
        )
      ),
      directory = new DirectoryConfiguration(
        simulations = Paths.get(config.getString(core.directory.Simulations)),
        resources = Paths.get(config.getString(core.directory.Resources)),
        binaries = config.getString(core.directory.Binaries).trimToOption.map(Paths.get(_)),
        reportsOnly = config.getString(core.directory.ReportsOnly).trimToOption,
        results = Paths.get(config.getString(core.directory.Results))
      )
    )

  private def chartingConfiguration(config: Config) =
    new ChartingConfiguration(
      noReports = config.getBoolean(charting.NoReports),
      maxPlotsPerSeries = config.getInt(charting.MaxPlotPerSeries),
      useGroupDurationMetric = config.getBoolean(charting.UseGroupDurationMetric),
      indicators = new IndicatorsConfiguration(
        lowerBound = config.getInt(charting.indicators.LowerBound),
        higherBound = config.getInt(charting.indicators.HigherBound),
        percentile1 = config.getDouble(charting.indicators.Percentile1),
        percentile2 = config.getDouble(charting.indicators.Percentile2),
        percentile3 = config.getDouble(charting.indicators.Percentile3),
        percentile4 = config.getDouble(charting.indicators.Percentile4)
      )
    )

  private def httpConfiguration(config: Config) =
    new HttpConfiguration(
      fetchedCssCacheMaxCapacity = config.getLong(http.FetchedCssCacheMaxCapacity),
      fetchedHtmlCacheMaxCapacity = config.getLong(http.FetchedHtmlCacheMaxCapacity),
      perUserCacheMaxCapacity = config.getInt(http.PerUserCacheMaxCapacity),
      warmUpUrl = config.getString(http.WarmUpUrl).trimToOption,
      enableGA = config.getBoolean(http.EnableGA),
      ssl = new SslConfiguration(
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
      ),
      advanced = new AdvancedConfiguration(
        connectTimeout = config.getInt(http.ahc.ConnectTimeout) millis,
        handshakeTimeout = config.getInt(http.ahc.HandshakeTimeout) millis,
        pooledConnectionIdleTimeout = config.getInt(http.ahc.PooledConnectionIdleTimeout) millis,
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
        useOpenSslFinalizers = config.getBoolean(http.ahc.UseOpenSslFinalizers),
        useNativeTransport = config.getBoolean(http.ahc.UseNativeTransport),
        tcpNoDelay = config.getBoolean(http.ahc.TcpNoDelay),
        soKeepAlive = config.getBoolean(http.ahc.SoKeepAlive),
        soReuseAddress = config.getBoolean(http.ahc.SoReuseAddress),
        allocator = config.getString(http.ahc.Allocator),
        maxThreadLocalCharBufferSize = config.getInt(http.ahc.MaxThreadLocalCharBufferSize)
      ),
      dns = new DnsConfiguration(
        queryTimeout = config.getInt(http.dns.QueryTimeout) millis,
        maxQueriesPerResolve = config.getInt(http.dns.MaxQueriesPerResolve)
      )
    )

  private def jmsConfiguration(config: Config) =
    new JmsConfiguration(
      replyTimeoutScanPeriod = config.getLong(jms.ReplyTimeoutScanPeriod) millis
    )

  private def dataConfiguration(config: Config) =
    new DataConfiguration(
      dataWriters = config.getStringList(data.Writers).asScala.flatMap(DataWriterType.findByName(_).toList),
      console = new ConsoleDataWriterConfiguration(
        light = config.getBoolean(data.console.Light),
        writePeriod = config.getInt(data.console.WritePeriod) seconds
      ),
      file = new FileDataWriterConfiguration(
        bufferSize = config.getInt(data.file.BufferSize)
      ),
      leak = new LeakDataWriterConfiguration(
        noActivityTimeout = config.getInt(data.leak.NoActivityTimeout) seconds
      ),
      graphite = new GraphiteDataWriterConfiguration(
        light = config.getBoolean(data.graphite.Light),
        host = config.getString(data.graphite.Host),
        port = config.getInt(data.graphite.Port),
        protocol = TransportProtocol(config.getString(data.graphite.Protocol).trim),
        rootPathPrefix = config.getString(data.graphite.RootPathPrefix),
        bufferSize = config.getInt(data.graphite.BufferSize),
        writePeriod = config.getInt(data.graphite.WritePeriod) seconds
      )
    )

  private def mapToGatlingConfig(config: Config) =
    new GatlingConfiguration(
      core = coreConfiguration(config),
      charting = chartingConfiguration(config),
      http = httpConfiguration(config),
      jms = jmsConfiguration(config),
      data = dataConfiguration(config),
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
      //
      // [fl]
      config = config
    )
}

final class CoreConfiguration(
    val version: String,
    val outputDirectoryBaseName: Option[String],
    val runDescription: Option[String],
    val encoding: String,
    val simulationClass: Option[String],
    val extract: ExtractConfiguration,
    val directory: DirectoryConfiguration,
    val elFileBodiesCacheMaxCapacity: Long,
    val rawFileBodiesCacheMaxCapacity: Long,
    val rawFileBodiesInMemoryMaxSize: Long,
    val pebbleFileBodiesCacheMaxCapacity: Long,
    val feederAdaptiveLoadModeThreshold: Long,
    val shutdownTimeout: Long
) {
  val charset: Charset = Charset.forName(encoding)
}

final class ExtractConfiguration(
    val regex: RegexConfiguration,
    val xpath: XPathConfiguration,
    val jsonPath: JsonPathConfiguration,
    val css: CssConfiguration
)

final class RegexConfiguration(
    val cacheMaxCapacity: Long
)

final class XPathConfiguration(
    val cacheMaxCapacity: Long
)

final class JsonPathConfiguration(
    val cacheMaxCapacity: Long
)

final class CssConfiguration(
    val cacheMaxCapacity: Long
)

final class DirectoryConfiguration(
    val simulations: Path,
    val resources: Path,
    val binaries: Option[Path],
    val reportsOnly: Option[String],
    val results: Path
)

final class ChartingConfiguration(
    val noReports: Boolean,
    val maxPlotsPerSeries: Int,
    val useGroupDurationMetric: Boolean,
    val indicators: IndicatorsConfiguration
)

final class IndicatorsConfiguration(
    val lowerBound: Int,
    val higherBound: Int,
    val percentile1: Double,
    val percentile2: Double,
    val percentile3: Double,
    val percentile4: Double
)

final class HttpConfiguration(
    val fetchedCssCacheMaxCapacity: Long,
    val fetchedHtmlCacheMaxCapacity: Long,
    val perUserCacheMaxCapacity: Int,
    val warmUpUrl: Option[String],
    val enableGA: Boolean,
    val ssl: SslConfiguration,
    val advanced: AdvancedConfiguration,
    val dns: DnsConfiguration
)

final class JmsConfiguration(
    val replyTimeoutScanPeriod: FiniteDuration
)

final class AdvancedConfiguration(
    val connectTimeout: FiniteDuration,
    val handshakeTimeout: FiniteDuration,
    val pooledConnectionIdleTimeout: FiniteDuration,
    val requestTimeout: FiniteDuration,
    val enableSni: Boolean,
    val enableHostnameVerification: Boolean,
    val useInsecureTrustManager: Boolean,
    val sslEnabledProtocols: List[String],
    val sslEnabledCipherSuites: List[String],
    val sslSessionCacheSize: Int,
    val sslSessionTimeout: FiniteDuration,
    val useOpenSsl: Boolean,
    val useOpenSslFinalizers: Boolean,
    val useNativeTransport: Boolean,
    val tcpNoDelay: Boolean,
    val soKeepAlive: Boolean,
    val soReuseAddress: Boolean,
    val allocator: String,
    val maxThreadLocalCharBufferSize: Int
)

final class DnsConfiguration(
    val queryTimeout: FiniteDuration,
    val maxQueriesPerResolve: Int
)

final class SslConfiguration(
    val keyManagerFactory: Option[KeyManagerFactory],
    val trustManagerFactory: Option[TrustManagerFactory]
)

final class DataConfiguration(
    val dataWriters: Seq[DataWriterType],
    val file: FileDataWriterConfiguration,
    val leak: LeakDataWriterConfiguration,
    val console: ConsoleDataWriterConfiguration,
    val graphite: GraphiteDataWriterConfiguration
) {
  def fileDataWriterEnabled: Boolean = dataWriters.contains(FileDataWriterType)
}

final class FileDataWriterConfiguration(
    val bufferSize: Int
)

final class LeakDataWriterConfiguration(
    val noActivityTimeout: FiniteDuration
)

final class ConsoleDataWriterConfiguration(
    val light: Boolean,
    val writePeriod: FiniteDuration
)

final class GraphiteDataWriterConfiguration(
    val light: Boolean,
    val host: String,
    val port: Int,
    val protocol: TransportProtocol,
    val rootPathPrefix: String,
    val bufferSize: Int,
    val writePeriod: FiniteDuration
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
//
// [fl]

final class GatlingConfiguration(
    val core: CoreConfiguration,
    val charting: ChartingConfiguration,
    val http: HttpConfiguration,
    val jms: JmsConfiguration,
    val data: DataConfiguration,
    // [fl]
    //
    // [fl]
    val config: Config
) {
  def resolve[T](value: T): T = value

  // [fl]
  //
  //
  //
  // [fl]
}
