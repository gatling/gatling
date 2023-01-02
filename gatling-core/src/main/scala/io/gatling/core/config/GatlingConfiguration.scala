/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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
import javax.net.ssl.{ KeyManagerFactory, SSLContext, TrustManagerFactory }

import scala.collection.mutable
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

import io.gatling.commons.shared.unstable.util.Ssl
import io.gatling.commons.util.ConfigHelper._
import io.gatling.commons.util.StringHelper._
import io.gatling.core.ConfigKeys._
import io.gatling.core.stats.writer._

import com.typesafe.config.{ Config, ConfigFactory }
import com.typesafe.scalalogging.StrictLogging

sealed abstract class ObsoleteUsage(val message: String) extends Product with Serializable { def path: String }
final case class Removed(path: String, advice: String) extends ObsoleteUsage(s"'$path' was removed, $advice.")
final case class Renamed(path: String, replacement: String)
    extends ObsoleteUsage(s"'$path' was renamed into $replacement and will be removed in the next minor release. Please rename.")

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
    def loadObsoleteUsagesFromBundle[T <: ObsoleteUsage](bundleName: String, creator: (String, String) => T): Seq[T] = {
      val bundle = ResourceBundle.getBundle(bundleName)
      bundle.getKeys.asScala.map(key => creator(key, bundle.getString(key))).toVector
    }

    def warnAboutRemovedProperties(config: Config, removedProperties: Seq[Removed], renamedProperties: Seq[Renamed]): Unit = {
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

    val customConfigFile = sys.props.getOrElse(GatlingCustomConfigFileOverrideSystemProperty, GatlingCustomConfigFile)
    logger.info(s"Gatling will try to load '$customConfigFile' config file as ClassLoader resource.")

    val defaultsConfig = ConfigFactory.parseResources(GatlingDefaultsConfigFile)
    val customConfig = ConfigFactory.parseResources(customConfigFile)
    val propertiesConfig = ConfigFactory.parseMap(props.asJava)

    val config = configChain(ConfigFactory.systemProperties, customConfig, propertiesConfig, defaultsConfig)

    val removedProperties = loadObsoleteUsagesFromBundle("config-removed", Removed.apply)
    val renamedProperties = loadObsoleteUsagesFromBundle("config-renamed", Renamed.apply)

    warnAboutRemovedProperties(config, removedProperties, renamedProperties)

    mapToGatlingConfig(RenamedAwareConfig(config, renamedProperties))
  }

  private def coreConfiguration(config: Config) =
    new CoreConfiguration(
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
        customResources = config.getString(core.directory.Resources).trimToOption.map(Paths.get(_)),
        binaries = config.getString(core.directory.Binaries).trimToOption.map(Paths.get(_)),
        reportsOnly = config.getString(core.directory.ReportsOnly).trimToOption,
        results = Paths.get(config.getString(core.directory.Results))
      )
    )

  private def socketConfiguration(config: Config) =
    new SocketConfiguration(
      connectTimeout = config.getInt(socket.ConnectTimeout).millis,
      tcpNoDelay = config.getBoolean(socket.TcpNoDelay),
      soKeepAlive = config.getBoolean(socket.SoKeepAlive),
      soReuseAddress = config.getBoolean(socket.SoReuseAddress)
    )

  private def defaultEnabledProtocols(useOpenSsl: Boolean) =
    if (useOpenSsl) {
      List("TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1")
    } else {
      try {
        val ctx = SSLContext.getInstance("TLS")
        ctx.init(null, null, null)
        ctx.getDefaultSSLParameters.getProtocols.filterNot(_ == "SSLv3").toList
      } catch {
        case e: Exception =>
          throw new Error("Failed to initialize the default SSL context", e)
      }
    }

  private def sslConfiguration(config: Config) = {
    val useOpenSsl = config.getBoolean(ssl.UseOpenSsl)
    val enabledProtocols = config.getStringList(ssl.EnabledProtocols).asScala.toList match {
      case Nil                  => defaultEnabledProtocols(useOpenSsl)
      case userDefinedProtocols => userDefinedProtocols
    }

    new SslConfiguration(
      useOpenSsl = useOpenSsl,
      useOpenSslFinalizers = config.getBoolean(ssl.UseOpenSslFinalizers),
      handshakeTimeout = config.getInt(ssl.HandshakeTimeout).millis,
      useInsecureTrustManager = config.getBoolean(ssl.UseInsecureTrustManager),
      enabledProtocols = enabledProtocols,
      enabledCipherSuites = config.getStringList(ssl.EnabledCipherSuites).asScala.toList,
      sessionCacheSize = config.getInt(ssl.SessionCacheSize),
      sessionTimeout = config.getInt(ssl.SessionTimeout).seconds,
      enableSni = config.getBoolean(ssl.EnableSni),
      keyManagerFactory = {
        val storeType = config.getString(ssl.keyStore.Type).trimToOption
        val storeFile = config.getString(ssl.keyStore.File).trimToOption
        val storePassword = config.getString(ssl.keyStore.Password)
        val storeAlgorithm = config.getString(ssl.keyStore.Algorithm).trimToOption
        storeFile.map(Ssl.newKeyManagerFactory(storeType, _, storePassword, storeAlgorithm))
      },
      trustManagerFactory = {
        val storeType = config.getString(ssl.trustStore.Type).trimToOption
        val storeFile = config.getString(ssl.trustStore.File).trimToOption
        val storePassword = config.getString(ssl.trustStore.Password)
        val storeAlgorithm = config.getString(ssl.trustStore.Algorithm).trimToOption
        storeFile.map(Ssl.newTrustManagerFactory(storeType, _, storePassword, storeAlgorithm))
      }
    )
  }

  private def nettyConfiguration(config: Config) =
    new NettyConfiguration(
      useNativeTransport = config.getBoolean(netty.UseNativeTransport),
      useIoUring = config.getBoolean(netty.UseIoUring),
      allocator = config.getString(netty.Allocator),
      maxThreadLocalCharBufferSize = config.getInt(netty.MaxThreadLocalCharBufferSize)
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
      requestTimeout = config.getInt(http.RequestTimeout).millis,
      pooledConnectionIdleTimeout = config.getInt(http.PooledConnectionIdleTimeout).millis,
      enableHostnameVerification = {
        val enable = config.getBoolean(http.EnableHostnameVerification)
        if (!enable) {
          System.setProperty("jdk.tls.allowUnsafeServerCertChange", "true")
          System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true")
        }
        enable
      },
      dns = new DnsConfiguration(
        queryTimeout = config.getInt(http.dns.QueryTimeout).millis,
        maxQueriesPerResolve = config.getInt(http.dns.MaxQueriesPerResolve)
      )
    )

  private def jmsConfiguration(config: Config) =
    new JmsConfiguration(
      replyTimeoutScanPeriod = config.getLong(jms.ReplyTimeoutScanPeriod).millis
    )

  private def dataConfiguration(config: Config) =
    new DataConfiguration(
      dataWriters = config.getStringList(data.Writers).asScala.flatMap(DataWriterType.findByName(_).toList).toSeq,
      console = new ConsoleDataWriterConfiguration(
        light = config.getBoolean(data.console.Light),
        writePeriod = config.getInt(data.console.WritePeriod).seconds
      ),
      file = new FileDataWriterConfiguration(
        bufferSize = config.getInt(data.file.BufferSize)
      ),
      leak = new LeakDataWriterConfiguration(
        noActivityTimeout = config.getInt(data.leak.NoActivityTimeout).seconds
      ),
      graphite = new GraphiteDataWriterConfiguration(
        light = config.getBoolean(data.graphite.Light),
        host = config.getString(data.graphite.Host),
        port = config.getInt(data.graphite.Port),
        protocol = TransportProtocol(config.getString(data.graphite.Protocol).trim),
        rootPathPrefix = config.getString(data.graphite.RootPathPrefix),
        bufferSize = config.getInt(data.graphite.BufferSize),
        writePeriod = config.getInt(data.graphite.WritePeriod).seconds
      ),
      enableAnalytics = config.getBoolean(data.EnableAnalytics),
      launcher = config.getStringOption(data.Launcher),
      buildToolVersion = config.getStringOption(data.BuildToolVersion)
    )

  private def mapToGatlingConfig(config: Config) =
    new GatlingConfiguration(
      // [e]
      //
      //
      // [e]
      core = coreConfiguration(config),
      socket = socketConfiguration(config),
      ssl = sslConfiguration(config),
      netty = nettyConfiguration(config),
      charting = chartingConfiguration(config),
      http = httpConfiguration(config),
      jms = jmsConfiguration(config),
      data = dataConfiguration(config)
    )
}

final class CoreConfiguration(
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
    val customResources: Option[Path],
    val binaries: Option[Path],
    val reportsOnly: Option[String],
    val results: Path
)

final class SocketConfiguration(
    val connectTimeout: FiniteDuration,
    val tcpNoDelay: Boolean,
    val soKeepAlive: Boolean,
    val soReuseAddress: Boolean
)

final class SslConfiguration(
    val useOpenSsl: Boolean,
    val useOpenSslFinalizers: Boolean,
    val handshakeTimeout: FiniteDuration,
    val useInsecureTrustManager: Boolean,
    val enabledProtocols: List[String],
    val enabledCipherSuites: List[String],
    val sessionCacheSize: Int,
    val sessionTimeout: FiniteDuration,
    val enableSni: Boolean,
    val keyManagerFactory: Option[KeyManagerFactory],
    val trustManagerFactory: Option[TrustManagerFactory]
)

final class NettyConfiguration(
    val useNativeTransport: Boolean,
    val useIoUring: Boolean,
    val allocator: String,
    val maxThreadLocalCharBufferSize: Int
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
    val pooledConnectionIdleTimeout: FiniteDuration,
    val requestTimeout: FiniteDuration,
    val enableHostnameVerification: Boolean,
    val dns: DnsConfiguration
)

final class JmsConfiguration(
    val replyTimeoutScanPeriod: FiniteDuration
)

final class DnsConfiguration(
    val queryTimeout: FiniteDuration,
    val maxQueriesPerResolve: Int
)

final class DataConfiguration(
    val dataWriters: Seq[DataWriterType],
    val file: FileDataWriterConfiguration,
    val leak: LeakDataWriterConfiguration,
    val console: ConsoleDataWriterConfiguration,
    val graphite: GraphiteDataWriterConfiguration,
    val enableAnalytics: Boolean,
    val launcher: Option[String],
    val buildToolVersion: Option[String]
) {
  def fileDataWriterEnabled: Boolean = dataWriters.contains(DataWriterType.File)
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

final class GatlingConfiguration(
    // [e]
    //
    //
    // [e]
    val core: CoreConfiguration,
    val socket: SocketConfiguration,
    val netty: NettyConfiguration,
    val ssl: SslConfiguration,
    val charting: ChartingConfiguration,
    val http: HttpConfiguration,
    val jms: JmsConfiguration,
    val data: DataConfiguration
)
