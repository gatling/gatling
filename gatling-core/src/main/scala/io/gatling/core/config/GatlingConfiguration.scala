/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.config

import java.nio.charset.Charset
import java.util.ArrayList
import java.util.ResourceBundle

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.io.Codec

import com.typesafe.config.{ Config, ConfigFactory }

import io.gatling.core.ConfigKeys._
import io.gatling.core.util.ConfigHelper.configChain
import io.gatling.core.util.StringHelper.RichString
import com.typesafe.scalalogging.StrictLogging

/**
 * Configuration loader of Gatling
 */
object GatlingConfiguration extends StrictLogging {

  // FIXME
  private var thisConfiguration: GatlingConfiguration = _
  def configuration = thisConfiguration

  private[gatling] def set(config: GatlingConfiguration): Unit = thisConfiguration = config

  def setUpForTest(props: mutable.Map[String, _ <: Any] = mutable.Map.empty) = {

    val defaultsConfig = ConfigFactory.parseResources(getClass.getClassLoader, "gatling-defaults.conf")
    val propertiesConfig = ConfigFactory.parseMap(props + (data.Writers -> new ArrayList)) // Disable DataWriters by default
    val config = configChain(ConfigFactory.systemProperties, propertiesConfig, defaultsConfig)
    thisConfiguration = mapToGatlingConfig(config)
    thisConfiguration
  }

  def setUp(props: mutable.Map[String, _ <: Any] = mutable.Map.empty): Unit = {
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
                |${obsoleteUsages.mkString("\n")}""".stripMargin)
        }
      }

    val classLoader = getClass.getClassLoader

    val defaultsConfig = ConfigFactory.parseResources(classLoader, "gatling-defaults.conf")
    val customConfig = ConfigFactory.parseResources(classLoader, "gatling.conf")
    val propertiesConfig = ConfigFactory.parseMap(props)

    val config = configChain(ConfigFactory.systemProperties, propertiesConfig, customConfig, defaultsConfig)

    warnAboutRemovedProperties(config)

    thisConfiguration = mapToGatlingConfig(config)
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
        extract = ExtractConfiguration(
          regex = RegexConfiguration(
            cacheMaxCapacity = config.getLong(core.extract.regex.CacheMaxCapacity)),
          xpath = XPathConfiguration(
            cacheMaxCapacity = config.getLong(core.extract.xpath.CacheMaxCapacity)),
          jsonPath = JsonPathConfiguration(
            cacheMaxCapacity = config.getLong(core.extract.jsonPath.CacheMaxCapacity),
            preferJackson = config.getBoolean(core.extract.jsonPath.PreferJackson),
            jackson = JacksonConfiguration(
              allowComments = config.getBoolean(core.extract.jsonPath.jackson.AllowComments),
              allowUnquotedFieldNames = config.getBoolean(core.extract.jsonPath.jackson.AllowUnquotedFieldNames),
              allowSingleQuotes = config.getBoolean(core.extract.jsonPath.jackson.AllowSingleQuotes))),
          css = CssConfiguration(
            cacheMaxCapacity = config.getLong(core.extract.css.CacheMaxCapacity))),
        timeOut = TimeOutConfiguration(
          simulation = config.getInt(core.timeOut.Simulation)),
        directory = DirectoryConfiguration(
          data = config.getString(core.directory.Data),
          bodies = config.getString(core.directory.Bodies),
          sources = config.getString(core.directory.Simulations),
          binaries = config.getString(core.directory.Binaries).trimToOption,
          reportsOnly = config.getString(core.directory.ReportsOnly).trimToOption,
          results = config.getString(core.directory.Results))),
      charting = ChartingConfiguration(
        noReports = config.getBoolean(charting.NoReports),
        maxPlotsPerSeries = config.getInt(charting.MaxPlotPerSeries),
        accuracy = config.getInt(charting.Accuracy),
        indicators = IndicatorsConfiguration(
          lowerBound = config.getInt(charting.indicators.LowerBound),
          higherBound = config.getInt(charting.indicators.HigherBound),
          percentile1 = config.getDouble(charting.indicators.Percentile1),
          percentile2 = config.getDouble(charting.indicators.Percentile2),
          percentile3 = config.getDouble(charting.indicators.Percentile3),
          percentile4 = config.getDouble(charting.indicators.Percentile4))),
      http = HttpConfiguration(
        elFileBodiesCacheMaxCapacity = config.getLong(http.ElFileBodiesCacheMaxCapacity),
        rawFileBodiesCacheMaxCapacity = config.getLong(http.RawFileBodiesCacheMaxCapacity),
        fetchedCssCacheMaxCapacity = config.getLong(http.FetchedCssCacheMaxCapacity),
        fetchedHtmlCacheMaxCapacity = config.getLong(http.FetchedHtmlCacheMaxCapacity),
        redirectPerUserCacheMaxCapacity = config.getInt(http.RedirectPerUserCacheMaxCapacity),
        expirePerUserCacheMaxCapacity = config.getInt(http.ExpirePerUserCacheMaxCapacity),
        lastModifiedPerUserCacheMaxCapacity = config.getInt(http.LastModifiedPerUserCacheMaxCapacity),
        etagPerUserCacheMaxCapacity = config.getInt(http.EtagPerUserCacheMaxCapacity),
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

          val trustStore = storeConfig(http.ssl.trustStore.Type, http.ssl.trustStore.File, http.ssl.trustStore.Password, http.ssl.trustStore.Algorithm)
          val keyStore = storeConfig(http.ssl.keyStore.Type, http.ssl.keyStore.File, http.ssl.keyStore.Password, http.ssl.keyStore.Algorithm)

          SslConfiguration(trustStore, keyStore)
        },
        ahc = AhcConfiguration(
          allowPoolingConnections = config.getBoolean(http.ahc.AllowPoolingConnections),
          allowPoolingSslConnections = config.getBoolean(http.ahc.AllowPoolingSslConnections),
          compressionEnforced = config.getBoolean(http.ahc.CompressionEnforced),
          connectTimeout = config.getInt(http.ahc.ConnectTimeout),
          pooledConnectionIdleTimeout = config.getInt(http.ahc.PooledConnectionIdleTimeout),
          readTimeout = config.getInt(http.ahc.ReadTimeout),
          connectionTTL = config.getInt(http.ahc.ConnectionTTL),
          ioThreadMultiplier = config.getInt(http.ahc.IoThreadMultiplier),
          maxConnectionsPerHost = config.getInt(http.ahc.MaxConnectionsPerHost),
          maxConnections = config.getInt(http.ahc.MaxConnections),
          maxRetry = config.getInt(http.ahc.MaxRetry),
          requestTimeOut = config.getInt(http.ahc.RequestTimeout),
          useProxyProperties = config.getBoolean(http.ahc.UseProxyProperties),
          webSocketTimeout = config.getInt(http.ahc.WebSocketTimeout),
          useRelativeURIsWithConnectProxies = config.getBoolean(http.ahc.UseRelativeURIsWithConnectProxies),
          acceptAnyCertificate = config.getBoolean(http.ahc.AcceptAnyCertificate),
          httpClientCodecMaxInitialLineLength = config.getInt(http.ahc.HttpClientCodecMaxInitialLineLength),
          httpClientCodecMaxHeaderSize = config.getInt(http.ahc.HttpClientCodecMaxHeaderSize),
          httpClientCodecMaxChunkSize = config.getInt(http.ahc.HttpClientCodecMaxChunkSize),
          keepEncodingHeader = config.getBoolean(http.ahc.KeepEncodingHeader),
          webSocketMaxFrameSize = config.getInt(http.ahc.WebSocketMaxFrameSize),
          httpsEnabledProtocols = config.getStringList(http.ahc.HttpsEnabledProtocols).toList,
          httpsEnabledCipherSuites = config.getStringList(http.ahc.HttpsEnabledCipherSuites).toList)),
      data = DataConfiguration(
        dataWriterClasses = config.getStringList(data.Writers).map { string =>
          DataConfiguration.Aliases.get(string) match {
            case Some(clazz) => clazz
            case None        => string
          }
        },
        dataReaderClass = config.getString(data.Reader).trim match {
          case "file" => "io.gatling.charts.result.reader.FileDataReader"
          case clazz  => clazz
        },
        console = ConsoleDataWriterConfiguration(
          light = config.getBoolean(data.console.Light)),
        file = FileDataWriterConfiguration(
          bufferSize = config.getInt(data.file.BufferSize)),
        leak = LeakDataWriterConfiguration(
          noActivityTimeout = config.getInt(data.leak.NoActivityTimeout)),
        graphite = GraphiteDataWriterConfiguration(
          light = config.getBoolean(data.graphite.Light),
          host = config.getString(data.graphite.Host),
          port = config.getInt(data.graphite.Port),
          protocol = GraphiteProtocol(config.getString(data.graphite.Protocol).trim),
          rootPathPrefix = config.getString(data.graphite.RootPathPrefix),
          bufferSize = config.getInt(data.graphite.BufferSize),
          writeInterval = config.getInt(data.graphite.WriteInterval)),
        jdbc = JdbcDataWriterConfiguration(
          db = DbConfiguration(
            url = config.getString(data.jdbc.Url),
            username = config.getString(data.jdbc.Username),
            password = config.getString(data.jdbc.Password)),
          bufferSize = config.getInt(data.jdbc.BufferSize),
          createStatements = CreateStatements(
            createRunRecordTable = config.getString(data.jdbc.create.CreateRunRecordTable).trimToOption,
            createRequestRecordTable = config.getString(data.jdbc.create.CreateRequestRecordTable).trimToOption,
            createScenarioRecordTable = config.getString(data.jdbc.create.CreateScenarioRecordTable).trimToOption,
            createGroupRecordTable = config.getString(data.jdbc.create.CreateGroupRecordTable).trimToOption),
          insertStatements = InsertStatements(
            insertRunRecord = config.getString(data.jdbc.insert.InsertRunRecord).trimToOption,
            insertRequestRecord = config.getString(data.jdbc.insert.InsertRequestRecord).trimToOption,
            insertScenarioRecord = config.getString(data.jdbc.insert.InsertScenarioRecord).trimToOption,
            insertGroupRecord = config.getString(data.jdbc.insert.InsertGroupRecord).trimToOption))),
      config)

}

case class CoreConfiguration(
    version: String,
    outputDirectoryBaseName: Option[String],
    runDescription: Option[String],
    encoding: String,
    simulationClass: Option[String],
    extract: ExtractConfiguration,
    timeOut: TimeOutConfiguration,
    directory: DirectoryConfiguration,
    muteMode: Boolean) {

  val charset = Charset.forName(encoding)
  val codec: Codec = charset
}

case class TimeOutConfiguration(
  simulation: Int)

case class ExtractConfiguration(
  regex: RegexConfiguration,
  xpath: XPathConfiguration,
  jsonPath: JsonPathConfiguration,
  css: CssConfiguration)

case class RegexConfiguration(
  cacheMaxCapacity: Long)

case class XPathConfiguration(
  cacheMaxCapacity: Long)

case class JsonPathConfiguration(
  cacheMaxCapacity: Long,
  preferJackson: Boolean,
  jackson: JacksonConfiguration)

case class JacksonConfiguration(
  allowComments: Boolean,
  allowUnquotedFieldNames: Boolean,
  allowSingleQuotes: Boolean)

case class CssConfiguration(
  cacheMaxCapacity: Long)

case class DirectoryConfiguration(
  data: String,
  bodies: String,
  sources: String,
  binaries: Option[String],
  reportsOnly: Option[String],
  results: String)

case class ChartingConfiguration(
  noReports: Boolean,
  maxPlotsPerSeries: Int,
  accuracy: Int,
  indicators: IndicatorsConfiguration)

case class IndicatorsConfiguration(
  lowerBound: Int,
  higherBound: Int,
  percentile1: Double,
  percentile2: Double,
  percentile3: Double,
  percentile4: Double)

case class HttpConfiguration(
  elFileBodiesCacheMaxCapacity: Long,
  rawFileBodiesCacheMaxCapacity: Long,
  fetchedCssCacheMaxCapacity: Long,
  fetchedHtmlCacheMaxCapacity: Long,
  redirectPerUserCacheMaxCapacity: Int,
  expirePerUserCacheMaxCapacity: Int,
  lastModifiedPerUserCacheMaxCapacity: Int,
  etagPerUserCacheMaxCapacity: Int,
  warmUpUrl: Option[String],
  enableGA: Boolean,
  ssl: SslConfiguration,
  ahc: AhcConfiguration)

case class AhcConfiguration(
  allowPoolingConnections: Boolean,
  allowPoolingSslConnections: Boolean,
  compressionEnforced: Boolean,
  connectTimeout: Int,
  pooledConnectionIdleTimeout: Int,
  readTimeout: Int,
  connectionTTL: Int,
  ioThreadMultiplier: Int,
  maxConnectionsPerHost: Int,
  maxConnections: Int,
  maxRetry: Int,
  requestTimeOut: Int,
  useProxyProperties: Boolean,
  webSocketTimeout: Int,
  useRelativeURIsWithConnectProxies: Boolean,
  acceptAnyCertificate: Boolean,
  httpClientCodecMaxInitialLineLength: Int,
  httpClientCodecMaxHeaderSize: Int,
  httpClientCodecMaxChunkSize: Int,
  keepEncodingHeader: Boolean,
  webSocketMaxFrameSize: Int,
  httpsEnabledProtocols: List[String],
  httpsEnabledCipherSuites: List[String])

case class SslConfiguration(
  trustStore: Option[StoreConfiguration],
  keyStore: Option[StoreConfiguration])

case class StoreConfiguration(
  storeType: Option[String],
  file: String,
  password: String,
  algorithm: Option[String])

object DataConfiguration {

  case class DataWriterAlias(alias: String, className: String)

  val ConsoleDataWriterAlias = DataWriterAlias("console", "io.gatling.core.result.writer.ConsoleDataWriter")
  val FileDataWriterAlias = DataWriterAlias("file", "io.gatling.core.result.writer.FileDataWriter")
  val GraphiteDataWriterAlias = DataWriterAlias("graphite", "io.gatling.metrics.GraphiteDataWriter")
  val JdbcDataWriterAlias = DataWriterAlias("jdbc", "io.gatling.jdbc.result.writer.JdbcDataWriter")
  val LeakReporterDataWriterAlias = DataWriterAlias("leak", "io.gatling.core.result.writer.LeakReporterDataWriter")

  val Aliases = Seq(ConsoleDataWriterAlias, FileDataWriterAlias, GraphiteDataWriterAlias, JdbcDataWriterAlias, LeakReporterDataWriterAlias)
    .map(alias => alias.alias -> alias.className).toMap
}

case class DataConfiguration(
    dataWriterClasses: Seq[String],
    dataReaderClass: String,
    file: FileDataWriterConfiguration,
    leak: LeakDataWriterConfiguration,
    jdbc: JdbcDataWriterConfiguration,
    console: ConsoleDataWriterConfiguration,
    graphite: GraphiteDataWriterConfiguration) {

  def fileDataWriterEnabled: Boolean = dataWriterClasses.contains(DataConfiguration.FileDataWriterAlias.className)
}

case class FileDataWriterConfiguration(
  bufferSize: Int)

case class LeakDataWriterConfiguration(
  noActivityTimeout: Int)

case class DbConfiguration(
  url: String,
  username: String,
  password: String)

case class CreateStatements(
  createRunRecordTable: Option[String],
  createRequestRecordTable: Option[String],
  createScenarioRecordTable: Option[String],
  createGroupRecordTable: Option[String])

case class InsertStatements(
  insertRunRecord: Option[String],
  insertRequestRecord: Option[String],
  insertScenarioRecord: Option[String],
  insertGroupRecord: Option[String])

case class JdbcDataWriterConfiguration(
  db: DbConfiguration,
  bufferSize: Int,
  createStatements: CreateStatements,
  insertStatements: InsertStatements)

case class ConsoleDataWriterConfiguration(
  light: Boolean)

case class GraphiteDataWriterConfiguration(
  light: Boolean,
  host: String,
  port: Int,
  protocol: GraphiteProtocol,
  rootPathPrefix: String,
  bufferSize: Int,
  writeInterval: Int)

case class GatlingConfiguration(
  core: CoreConfiguration,
  charting: ChartingConfiguration,
  http: HttpConfiguration,
  data: DataConfiguration,
  config: Config)
