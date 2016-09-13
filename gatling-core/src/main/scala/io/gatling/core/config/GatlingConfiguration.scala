/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.io.Codec

import io.gatling.commons.util.{StringHelper, ConfigHelper}
import io.gatling.core.ConfigKeys._
import io.gatling.core.stats.writer._
import ConfigHelper.configChain
import StringHelper.RichString

import com.typesafe.config.{ Config, ConfigFactory }
import com.typesafe.scalalogging.StrictLogging

/**
 * Configuration loader of Gatling
 */
object GatlingConfiguration extends StrictLogging {

  private val GatlingDefaultsConfigFile = "gatling-defaults.conf"
  private val GatlingConfigFile = "gatling.conf"
  private val ActorSystemDefaultsConfigFile = "gatling-akka-defaults.conf"
  private val ActorSystemConfigFile = "gatling-akka.conf"

  def loadActorSystemConfiguration() = {
    val classLoader = getClass.getClassLoader

    val defaultsConfig = ConfigFactory.parseResources(classLoader, ActorSystemDefaultsConfigFile)
    val customConfig = ConfigFactory.parseResources(classLoader, ActorSystemConfigFile)

    configChain(customConfig, defaultsConfig)
  }

  def loadForTest(props: mutable.Map[String, _ <: Any] = mutable.Map.empty): GatlingConfiguration = {

    val defaultsConfig = ConfigFactory.parseResources(getClass.getClassLoader, GatlingDefaultsConfigFile)
    val propertiesConfig = ConfigFactory.parseMap(props)
    val config = configChain(ConfigFactory.systemProperties, propertiesConfig, defaultsConfig)
    mapToGatlingConfig(config)
  }

  def load(props: mutable.Map[String, _ <: Any] = mutable.Map.empty): GatlingConfiguration = {
    sealed abstract class ObsoleteUsage(val message: String) { def path: String }
    case class Removed(path: String, advice: String) extends ObsoleteUsage(s"'$path' was removed, $advice.")
    case class Renamed(path: String, replacement: String) extends ObsoleteUsage(s"'$path' was renamed into $replacement.")

      def loadObsoleteUsagesFromBundle[T <: ObsoleteUsage](bundleName: String, creator: (String, String) => T): Vector[T] = {
        val bundle = ResourceBundle.getBundle(bundleName)
        bundle.getKeys.map(key => creator(key, bundle.getString(key))).toVector
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

    val defaultsConfig = ConfigFactory.parseResources(classLoader, GatlingDefaultsConfigFile)
    val customConfig = ConfigFactory.parseResources(classLoader, GatlingConfigFile)
    val propertiesConfig = ConfigFactory.parseMap(props)

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
        muteMode = config.getBoolean(core.Mute),
        elFileBodiesCacheMaxCapacity = config.getLong(core.ElFileBodiesCacheMaxCapacity),
        rawFileBodiesCacheMaxCapacity = config.getLong(core.RawFileBodiesCacheMaxCapacity),
        rawFileBodiesInMemoryMaxSize = config.getLong(core.RawFileBodiesInMemoryMaxSize),
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
          data = config.getString(core.directory.Data),
          bodies = config.getString(core.directory.Bodies),
          sources = config.getString(core.directory.Simulations),
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
            def storeConfig(typeKey: String, fileKey: String, passwordKey: String, algorithmKey: String) = {

              val storeType = config.getString(typeKey).trimToOption
              val storeFile = config.getString(fileKey).trimToOption
              val storePassword = config.getString(passwordKey)
              val storeAlgorithm = config.getString(algorithmKey).trimToOption

              storeFile.map(StoreConfiguration(storeType, _, storePassword, storeAlgorithm))
            }

          SslConfiguration(
            keyStore = storeConfig(http.ssl.keyStore.Type, http.ssl.keyStore.File, http.ssl.keyStore.Password, http.ssl.keyStore.Algorithm),
            trustStore = storeConfig(http.ssl.trustStore.Type, http.ssl.trustStore.File, http.ssl.trustStore.Password, http.ssl.trustStore.Algorithm))
        },
        ahc = AhcConfiguration(
          keepAlive = config.getBoolean(http.ahc.KeepAlive),
          connectTimeout = config.getInt(http.ahc.ConnectTimeout),
          handshakeTimeout = config.getInt(http.ahc.HandshakeTimeout),
          pooledConnectionIdleTimeout = config.getInt(http.ahc.PooledConnectionIdleTimeout),
          readTimeout = config.getInt(http.ahc.ReadTimeout),
          maxRetry = config.getInt(http.ahc.MaxRetry),
          requestTimeOut = config.getInt(http.ahc.RequestTimeout),
          acceptAnyCertificate = {
            val accept = config.getBoolean(http.ahc.AcceptAnyCertificate)
            if (accept) {
              System.setProperty("jdk.tls.allowUnsafeServerCertChange", "true")
              System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true")
            }
            accept
          },
          httpClientCodecMaxInitialLineLength = config.getInt(http.ahc.HttpClientCodecMaxInitialLineLength),
          httpClientCodecMaxHeaderSize = config.getInt(http.ahc.HttpClientCodecMaxHeaderSize),
          httpClientCodecMaxChunkSize = config.getInt(http.ahc.HttpClientCodecMaxChunkSize),
          webSocketMaxFrameSize = config.getInt(http.ahc.WebSocketMaxFrameSize),
          sslEnabledProtocols = config.getStringList(http.ahc.SslEnabledProtocols).toList,
          sslEnabledCipherSuites = config.getStringList(http.ahc.SslEnabledCipherSuites).toList,
          sslSessionCacheSize = config.getInt(http.ahc.SslSessionCacheSize),
          sslSessionTimeout = config.getInt(http.ahc.SslSessionTimeout),
          useOpenSsl = config.getBoolean(http.ahc.UseOpenSsl),
          useNativeTransport = config.getBoolean(http.ahc.UseNativeTransport),
          usePooledMemory = config.getBoolean(http.ahc.UsePooledMemory),
          tcpNoDelay = config.getBoolean(http.ahc.TcpNoDelay),
          soReuseAddress = config.getBoolean(http.ahc.SoReuseAddress),
          soLinger = config.getInt(http.ahc.SoLinger),
          soSndBuf = config.getInt(http.ahc.SoSndBuf),
          soRcvBuf = config.getInt(http.ahc.SoRcvBuf),
          allocator = config.getString(http.ahc.Allocator),
          maxThreadLocalCharBufferSize = config.getInt(http.ahc.MaxThreadLocalCharBufferSize)
        ),
        dns = DnsConfiguration(
          queryTimeout = config.getInt(http.dns.QueryTimeout),
          maxQueriesPerResolve = config.getInt(http.dns.MaxQueriesPerResolve)
        )
      ),
      jms = JmsConfiguration(
        acknowledgedMessagesBufferSize = config.getInt(jms.AcknowledgedMessagesBufferSize)
      ),
      data = DataConfiguration(
        dataWriters = config.getStringList(data.Writers).flatMap(DataWriterType.findByName),
        console = ConsoleDataWriterConfiguration(
          light = config.getBoolean(data.console.Light)
        ),
        file = FileDataWriterConfiguration(
          bufferSize = config.getInt(data.file.BufferSize)
        ),
        leak = LeakDataWriterConfiguration(
          noActivityTimeout = config.getInt(data.leak.NoActivityTimeout)
        ),
        graphite = GraphiteDataWriterConfiguration(
          light = config.getBoolean(data.graphite.Light),
          host = config.getString(data.graphite.Host),
          port = config.getInt(data.graphite.Port),
          protocol = TransportProtocol(config.getString(data.graphite.Protocol).trim),
          rootPathPrefix = config.getString(data.graphite.RootPathPrefix),
          bufferSize = config.getInt(data.graphite.BufferSize),
          writeInterval = config.getInt(data.graphite.WriteInterval)
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
      // [fl]
      config = config
    )

}

case class CoreConfiguration(
    version:                       String,
    outputDirectoryBaseName:       Option[String],
    runDescription:                Option[String],
    encoding:                      String,
    simulationClass:               Option[String],
    extract:                       ExtractConfiguration,
    directory:                     DirectoryConfiguration,
    muteMode:                      Boolean,
    elFileBodiesCacheMaxCapacity:  Long,
    rawFileBodiesCacheMaxCapacity: Long,
    rawFileBodiesInMemoryMaxSize:  Long
) {

  val charset = Charset.forName(encoding)
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
  data:        String,
  bodies:      String,
  sources:     String,
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
  ahc:                         AhcConfiguration,
  dns:                         DnsConfiguration
)

case class AhcConfiguration(
  keepAlive:                           Boolean,
  connectTimeout:                      Int,
  handshakeTimeout:                    Int,
  pooledConnectionIdleTimeout:         Int,
  readTimeout:                         Int,
  maxRetry:                            Int,
  requestTimeOut:                      Int,
  acceptAnyCertificate:                Boolean,
  httpClientCodecMaxInitialLineLength: Int,
  httpClientCodecMaxHeaderSize:        Int,
  httpClientCodecMaxChunkSize:         Int,
  webSocketMaxFrameSize:               Int,
  sslEnabledProtocols:                 List[String],
  sslEnabledCipherSuites:              List[String],
  sslSessionCacheSize:                 Int,
  sslSessionTimeout:                   Int,
  useOpenSsl:                          Boolean,
  useNativeTransport:                  Boolean,
  usePooledMemory:                     Boolean,
  tcpNoDelay:                          Boolean,
  soReuseAddress:                      Boolean,
  soLinger:                            Int,
  soSndBuf:                            Int,
  soRcvBuf:                            Int,
  allocator:                           String,
  maxThreadLocalCharBufferSize:        Int
)

case class DnsConfiguration(
  queryTimeout: Int,
  maxQueriesPerResolve: Int
)

case class SslConfiguration(
  keyStore:   Option[StoreConfiguration],
  trustStore: Option[StoreConfiguration]
)

case class StoreConfiguration(
  storeType: Option[String],
  file:      String,
  password:  String,
  algorithm: Option[String]
)

case class JmsConfiguration(
  acknowledgedMessagesBufferSize: Int
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
  noActivityTimeout: Int
)

case class ConsoleDataWriterConfiguration(
  light: Boolean
)

case class GraphiteDataWriterConfiguration(
  light:          Boolean,
  host:           String,
  port:           Int,
  protocol:       TransportProtocol,
  rootPathPrefix: String,
  bufferSize:     Int,
  writeInterval:  Int
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
//
//
//
//
// [fl]

case class GatlingConfiguration(
     core:      CoreConfiguration,
     charting:  ChartingConfiguration,
     http:      HttpConfiguration,
     jms:       JmsConfiguration,
     data:      DataConfiguration,
     // [fl]
     //
     // [fl]
     config:    Config
) {

  def resolve[T](value: T): T = value

  // [fl]
  //
  //
  // [fl]
}
