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

import scala.collection.JavaConversions.mapAsJavaMap
import scala.collection.mutable
import scala.io.Codec

import com.typesafe.config.{ Config, ConfigFactory }

import io.gatling.core.ConfigKeys._
import io.gatling.core.util.StringHelper.RichString
import com.typesafe.scalalogging.slf4j.StrictLogging

/**
 * Configuration loader of Gatling
 */
object GatlingConfiguration extends StrictLogging {

  // FIXME
  var configuration: GatlingConfiguration = _

  implicit class ConfigStringSeq(val string: String) extends AnyVal {
    def toStringList: List[String] = string.trim match {
      case "" => Nil
      case s  => s.split(",").map(_.trim).toList
    }
  }

  def fakeConfig(props: Map[String, _ <: Any] = Map.empty) = {
    val defaultsConfig = ConfigFactory.parseResources(getClass.getClassLoader, "gatling-defaults.conf")
    val propertiesConfig = ConfigFactory.parseMap(props)
    val config = ConfigFactory.systemProperties.withFallback(propertiesConfig).withFallback(defaultsConfig)
    mapToGatlingConfig(config)
  }

  def setUp(props: mutable.Map[String, _ <: Any] = mutable.Map.empty) {

      def warnAboutRemovedProperties(config: Config) {

          def warnAboutRemovedProperty(path: String) {
            if (config.hasPath(path))
              logger.warn(s"Beware, property $path is still defined but it was removed")
          }

        Vector("gatling.core.extract.xpath.saxParserFactory",
          "gatling.core.extract.xpath.domParserFactory",
          "gatling.core.extract.xpath.expandEntityReferences",
          "gatling.core.extract.xpath.namespaceAware",
          "gatling.core.extract.css.engine",
          "gatling.core.timeOut.actor",
          "gatling.http.baseUrls",
          "gatling.http.proxy.host",
          "gatling.http.proxy.port",
          "gatling.http.proxy.securedPort",
          "gatling.http.proxy.username",
          "gatling.http.proxy.password",
          "gatling.http.followRedirect",
          "gatling.http.autoReferer",
          "gatling.http.cache",
          "gatling.http.discardResponseChunks",
          "gatling.http.shareConnections",
          "gatling.http.basicAuth.username",
          "gatling.http.basicAuth.password",
          "gatling.http.ahc.provider",
          "gatling.http.ahc.requestCompressionLevel",
          "gatling.http.ahc.userAgent",
          "gatling.http.ahc.rfc6265CookieEncoding").foreach(warnAboutRemovedProperty)
      }

    val classLoader = getClass.getClassLoader

    val defaultsConfig = ConfigFactory.parseResources(classLoader, "gatling-defaults.conf")
    val customConfig = ConfigFactory.parseResources(classLoader, "gatling.conf")
    val propertiesConfig = ConfigFactory.parseMap(props)

    val config = ConfigFactory.systemProperties.withFallback(propertiesConfig).withFallback(customConfig).withFallback(defaultsConfig)

    warnAboutRemovedProperties(config)

    configuration = mapToGatlingConfig(config)
  }

  private def mapToGatlingConfig(config: Config) =
    GatlingConfiguration(
      core = CoreConfiguration(
        outputDirectoryBaseName = config.getString(core.OutputDirectoryBaseName).trimToOption,
        runDescription = config.getString(core.RunDescription).trimToOption,
        encoding = config.getString(core.Encoding),
        simulationClass = config.getString(core.SimulationClass).trimToOption,
        disableCompiler = config.getBoolean(core.DisableCompiler),
        muteMode = config.getBoolean(core.Mute),
        extract = ExtractConfiguration(
          regex = RegexConfiguration(
            cache = config.getBoolean(core.extract.regex.Cache)),
          xpath = XPathConfiguration(
            cache = config.getBoolean(core.extract.xpath.Cache)),
          jsonPath = JsonPathConfiguration(
            cache = config.getBoolean(core.extract.jsonPath.Cache),
            jackson = JacksonConfiguration(
              allowComments = config.getBoolean(core.extract.jsonPath.jackson.AllowComments),
              allowUnquotedFieldNames = config.getBoolean(core.extract.jsonPath.jackson.AllowUnquotedFieldNames),
              allowSingleQuotes = config.getBoolean(core.extract.jsonPath.jackson.AllowSingleQuotes))),
          css = CssConfiguration(
            cache = config.getBoolean(core.extract.css.Cache))),
        timeOut = TimeOutConfiguration(
          simulation = config.getInt(core.timeOut.Simulation)),
        directory = DirectoryConfiguration(
          data = config.getString(core.directory.Data),
          requestBodies = config.getString(core.directory.RequestBodies),
          sources = config.getString(core.directory.Simulations),
          binaries = config.getString(core.directory.Binaries).trimToOption,
          reportsOnly = config.getString(core.directory.ReportsOnly).trimToOption,
          results = config.getString(core.directory.Results)),
        zinc = ZincConfiguration(
          jvmArgs = config.getString(core.zinc.JvmArgs).split(" "))),
      charting = ChartingConfiguration(
        noReports = config.getBoolean(charting.NoReports),
        statsTsvSeparator = config.getString(charting.StatsTsvSeparator),
        maxPlotsPerSeries = config.getInt(charting.MaxPlotPerSeries),
        accuracy = config.getInt(charting.Accuracy),
        indicators = IndicatorsConfiguration(
          lowerBound = config.getInt(charting.indicators.LowerBound),
          higherBound = config.getInt(charting.indicators.HigherBound),
          percentile1 = config.getInt(charting.indicators.Percentile1),
          percentile2 = config.getInt(charting.indicators.Percentile2))),
      http = HttpConfiguration(
        cacheELFileBodies = config.getBoolean(http.CacheELFileBodies),
        cacheRawFileBodies = config.getBoolean(http.CacheRawFileBodies),
        warmUpUrl = config.getString(http.WarmUpUrl).trimToOption,
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
        ahc = AHCConfiguration(
          allowPoolingConnection = config.getBoolean(http.ahc.AllowPoolingConnection),
          allowSslConnectionPool = config.getBoolean(http.ahc.AllowSslConnectionPool),
          compressionEnabled = config.getBoolean(http.ahc.CompressionEnabled),
          connectionTimeOut = config.getInt(http.ahc.ConnectionTimeOut),
          idleConnectionInPoolTimeOutInMs = config.getInt(http.ahc.IdleConnectionInPoolTimeoutInMs),
          idleConnectionTimeOutInMs = config.getInt(http.ahc.IdleConnectionTimeoutInMs),
          maxConnectionLifeTimeInMs = config.getInt(http.ahc.MaxConnectionLifeTimeInMs),
          ioThreadMultiplier = config.getInt(http.ahc.IoThreadMultiplier),
          maximumConnectionsPerHost = config.getInt(http.ahc.MaximumConnectionsPerHost),
          maximumConnectionsTotal = config.getInt(http.ahc.MaximumConnectionsTotal),
          maxRetry = config.getInt(http.ahc.MaxRetry),
          requestTimeOutInMs = config.getInt(http.ahc.RequestTimeoutInMs),
          useProxyProperties = config.getBoolean(http.ahc.UseProxyProperties),
          useRawUrl = config.getBoolean(http.ahc.UseRawUrl),
          webSocketIdleTimeoutInMs = config.getInt(http.ahc.WebSocketIdleTimeoutInMs),
          useRelativeURIsWithSSLProxies = config.getBoolean(http.ahc.UseRelativeURIsWithSSLProxies))),
      data = DataConfiguration(
        dataWriterClasses = config.getString(data.Writers).toStringList.map {
          case "console"  => "io.gatling.core.result.writer.ConsoleDataWriter"
          case "file"     => "io.gatling.core.result.writer.FileDataWriter"
          case "graphite" => "io.gatling.metrics.GraphiteDataWriter"
          case "jdbc"     => "io.gatling.jdbc.result.writer.JdbcDataWriter"
          case "leak"     => "io.gatling.core.result.writer.LeakReporterDataWriter"
          case clazz      => clazz
        },
        dataReaderClass = config.getString(data.Reader).trim match {
          case "file" => "io.gatling.charts.result.reader.FileDataReader"
          case clazz  => clazz
        },
        console = ConsoleDataWriterConfiguration(
          light = config.getBoolean(data.console.Light)),
        file = FileDataWriterConfiguration(
          bufferSize = config.getInt(data.file.BufferSize)),
        graphite = GraphiteDataWriterConfiguration(
          light = config.getBoolean(data.graphite.Light),
          host = config.getString(data.graphite.Host),
          port = config.getInt(data.graphite.Port),
          protocol = config.getString(data.graphite.Protocol),
          rootPathPrefix = config.getString(data.graphite.RootPathPrefix),
          maxMeasuredValue = config.getInt(data.graphite.MaxMeasuredValue),
          bufferSize = config.getInt(data.graphite.BufferSize)),
        jdbc = JDBCDataWriterConfiguration(
          db = DBConfiguration(
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
    outputDirectoryBaseName: Option[String],
    runDescription: Option[String],
    encoding: String,
    simulationClass: Option[String],
    disableCompiler: Boolean,
    extract: ExtractConfiguration,
    timeOut: TimeOutConfiguration,
    directory: DirectoryConfiguration,
    muteMode: Boolean,
    zinc: ZincConfiguration) {

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
  cache: Boolean)

case class XPathConfiguration(
  cache: Boolean)

case class JsonPathConfiguration(
  cache: Boolean,
  jackson: JacksonConfiguration)

case class JacksonConfiguration(
  allowComments: Boolean,
  allowUnquotedFieldNames: Boolean,
  allowSingleQuotes: Boolean)

case class CssConfiguration(
  cache: Boolean)

case class DirectoryConfiguration(
  data: String,
  requestBodies: String,
  sources: String,
  binaries: Option[String],
  reportsOnly: Option[String],
  results: String)

case class ZincConfiguration(
  jvmArgs: Array[String])

case class ChartingConfiguration(
  noReports: Boolean,
  statsTsvSeparator: String,
  maxPlotsPerSeries: Int,
  accuracy: Int,
  indicators: IndicatorsConfiguration)

case class IndicatorsConfiguration(
  lowerBound: Int,
  higherBound: Int,
  percentile1: Int,
  percentile2: Int)

case class HttpConfiguration(
  cacheELFileBodies: Boolean,
  cacheRawFileBodies: Boolean,
  warmUpUrl: Option[String],
  ssl: SslConfiguration,
  ahc: AHCConfiguration)

case class AHCConfiguration(
  allowPoolingConnection: Boolean,
  allowSslConnectionPool: Boolean,
  compressionEnabled: Boolean,
  connectionTimeOut: Int,
  idleConnectionInPoolTimeOutInMs: Int,
  idleConnectionTimeOutInMs: Int,
  maxConnectionLifeTimeInMs: Int,
  ioThreadMultiplier: Int,
  maximumConnectionsPerHost: Int,
  maximumConnectionsTotal: Int,
  maxRetry: Int,
  requestTimeOutInMs: Int,
  useProxyProperties: Boolean,
  useRawUrl: Boolean,
  webSocketIdleTimeoutInMs: Int,
  useRelativeURIsWithSSLProxies: Boolean)

case class SslConfiguration(
  trustStore: Option[StoreConfiguration],
  keyStore: Option[StoreConfiguration])

case class StoreConfiguration(
  storeType: Option[String],
  file: String,
  password: String,
  algorithm: Option[String])

case class DataConfiguration(
  dataWriterClasses: Seq[String],
  dataReaderClass: String,
  file: FileDataWriterConfiguration,
  jdbc: JDBCDataWriterConfiguration,
  console: ConsoleDataWriterConfiguration,
  graphite: GraphiteDataWriterConfiguration)

case class FileDataWriterConfiguration(
  bufferSize: Int)

case class DBConfiguration(
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

case class JDBCDataWriterConfiguration(
  db: DBConfiguration,
  bufferSize: Int,
  createStatements: CreateStatements,
  insertStatements: InsertStatements)

case class ConsoleDataWriterConfiguration(
  light: Boolean)

case class GraphiteDataWriterConfiguration(
  light: Boolean,
  host: String,
  port: Int,
  protocol: String,
  rootPathPrefix: String,
  maxMeasuredValue: Int,
  bufferSize: Int)

case class GatlingConfiguration(
  core: CoreConfiguration,
  charting: ChartingConfiguration,
  http: HttpConfiguration,
  data: DataConfiguration,
  config: Config)
