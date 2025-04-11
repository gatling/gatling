/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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
import java.time.{ ZoneId, ZoneOffset }
import javax.net.ssl.{ KeyManagerFactory, SSLContext, TrustManagerFactory }

import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

import io.gatling.commons.util.ConfigHelper._
import io.gatling.commons.util.StringHelper._
import io.gatling.commons.util.SystemProps.setSystemPropertyIfUndefined
import io.gatling.core.config.ConfigKeys._
import io.gatling.core.stats.writer._
import io.gatling.shared.util.Ssl

import com.typesafe.config.{ Config, ConfigFactory }
import com.typesafe.scalalogging.StrictLogging
import io.netty.handler.ssl.OpenSsl
import io.netty.util.internal.PlatformDependent

object GatlingConfiguration extends StrictLogging {
  private val GatlingDefaultsConfigFile = "gatling-defaults.conf"
  private val GatlingCustomConfigFile = "gatling.conf"
  private val GatlingCustomConfigFileOverrideSystemProperty = "gatling.conf.file"

  def loadForTest(props: (String, _ <: Any)*): GatlingConfiguration = {
    val defaultsConfig = ConfigFactory.parseResources(getClass.getClassLoader, GatlingDefaultsConfigFile)
    val propertiesConfig = ConfigFactory.parseMap(props.toMap.asJava)
    val config = configChain(ConfigFactory.systemProperties, propertiesConfig, defaultsConfig)
    mapToGatlingConfig(config)
  }

  def load(): GatlingConfiguration = {
    val customConfigFile = sys.props.getOrElse(GatlingCustomConfigFileOverrideSystemProperty, GatlingCustomConfigFile)
    logger.info(s"Gatling will try to load '$customConfigFile' config file as ClassLoader resource.")

    val defaultsConfig = ConfigFactory.parseResources(getClass.getClassLoader, GatlingDefaultsConfigFile)
    val customConfig = ConfigFactory.parseResources(getClass.getClassLoader, customConfigFile)

    val config = configChain(ConfigFactory.systemProperties, customConfig, defaultsConfig)
    mapToGatlingConfig(config)
  }

  private def coreConfiguration(config: Config) =
    new CoreConfiguration(
      encoding = config.getString(core.Encoding),
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
      )
    )

  private def socketConfiguration(config: Config) =
    new SocketConfiguration(
      connectTimeout = config.getInt(socket.ConnectTimeout).millis,
      tcpNoDelay = config.getBoolean(socket.TcpNoDelay),
      soKeepAlive = config.getBoolean(socket.SoKeepAlive)
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
    val useOpenSsl =
      config.getBoolean(ssl.UseOpenSsl) && {
        if (OpenSsl.isAvailable) {
          true
        } else {
          throw new UnsupportedOperationException(
            s"BoringSSL is enabled in your configuration, yet it's not available for your platform ${PlatformDependent
                .normalizedOs()}_${PlatformDependent.normalizedArch()}.",
            OpenSsl.unavailabilityCause()
          )
        }
      }
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
        val storeType = config.getStringOption(ssl.keyStore.Type)
        val storeFile = config.getStringOption(ssl.keyStore.File)
        val storePassword = config.getStringOption(ssl.keyStore.Password)
        val storeAlgorithm = config.getStringOption(ssl.keyStore.Algorithm)
        storeFile.map(Ssl.newKeyManagerFactory(storeType, _, storePassword.getOrElse(""), storeAlgorithm))
      },
      trustManagerFactory = {
        val storeType = config.getStringOption(ssl.trustStore.Type)
        val storeFile = config.getStringOption(ssl.trustStore.File)
        val storePassword = config.getStringOption(ssl.trustStore.Password)
        val storeAlgorithm = config.getStringOption(ssl.trustStore.Algorithm)
        storeFile.map(Ssl.newTrustManagerFactory(storeType, _, storePassword.getOrElse(""), storeAlgorithm))
      }
    )
  }

  private def nettyConfiguration(config: Config) = {
    setSystemPropertyIfUndefined("io.netty.allocator.type", config.getString(netty.Allocator))
    setSystemPropertyIfUndefined("io.netty.maxThreadLocalCharBufferSize", config.getString(netty.MaxThreadLocalCharBufferSize))

    new NettyConfiguration(
      useNativeTransport = config.getBoolean(netty.UseNativeTransport),
      useIoUring = config.getBoolean(netty.UseIoUring)
    )
  }

  private def chartingConfiguration(config: Config) =
    new ReportsConfiguration(
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
      zoneId = if (config.getBoolean(data.UtcDateTime)) ZoneOffset.UTC else ZoneId.systemDefault(),
      dataWriters = config.getStringList(data.Writers).asScala.flatMap(DataWriterType.findByName(_).toList).toSeq,
      console = new ConsoleDataWriterConfiguration(
        light = config.getBoolean(data.console.Light),
        writePeriod = {
          val value = config.getInt(data.console.WritePeriod)
          require(value > 0, s"${data.console.WritePeriod} must be > 0")
          value.seconds
        }
      ),
      enableAnalytics = config.getBoolean(data.EnableAnalytics)
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
      reports = chartingConfiguration(config),
      http = httpConfiguration(config),
      jms = jmsConfiguration(config),
      data = dataConfiguration(config)
    )
}

final class CoreConfiguration(
    val encoding: String,
    val extract: ExtractConfiguration,
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

final class SocketConfiguration(
    val connectTimeout: FiniteDuration,
    val tcpNoDelay: Boolean,
    val soKeepAlive: Boolean
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
    val useIoUring: Boolean
)

final class ReportsConfiguration(
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
    val zoneId: ZoneId,
    val dataWriters: Seq[DataWriterType],
    val console: ConsoleDataWriterConfiguration,
    val enableAnalytics: Boolean
) {
  def fileDataWriterEnabled: Boolean = dataWriters.contains(DataWriterType.File)
}

final class ConsoleDataWriterConfiguration(
    val light: Boolean,
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
    val reports: ReportsConfiguration,
    val http: HttpConfiguration,
    val jms: JmsConfiguration,
    val data: DataConfiguration
)
