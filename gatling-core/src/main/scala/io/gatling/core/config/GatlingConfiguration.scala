/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
import com.typesafe.config.{ Config, ConfigFactory }
import io.gatling.core.ConfigurationConstants._
import io.gatling.core.util.StringHelper.RichString
import scala.io.Codec

/**
 * Configuration loader of Gatling
 */
object GatlingConfiguration {

	// FIXME
	var configuration: GatlingConfiguration = _

	implicit class ConfigStringSeq(val string: String) extends AnyVal {
		def toStringSeq: Seq[String] = string.trim match {
			case "" => Nil
			case s => s.split(",").map(_.trim)
		}
	}

	def setUp(props: mutable.Map[String, _ <: Any] = mutable.Map.empty) {
		val classLoader = getClass.getClassLoader

		val defaultsConfig = ConfigFactory.parseResources(classLoader, "gatling-defaults.conf")
		val customConfig = ConfigFactory.parseResources(classLoader, "gatling.conf")
		val propertiesConfig = ConfigFactory.parseMap(props)

		val config = ConfigFactory.systemProperties.withFallback(propertiesConfig).withFallback(customConfig).withFallback(defaultsConfig)

		configuration = GatlingConfiguration(
			core = CoreConfiguration(
				outputDirectoryBaseName = config.getString(CONF_CORE_OUTPUT_DIRECTORY_BASE_NAME).trimToOption,
				runDescription = config.getString(CONF_CORE_RUN_DESCRIPTION).trimToOption,
				encoding = config.getString(CONF_CORE_ENCODING),
				simulationClass = config.getString(CONF_CORE_SIMULATION_CLASS).trimToOption,
				disableCompiler = config.getBoolean(CONF_CORE_DISABLE_COMPILER),
				extract = ExtractConfiguration(
					regex = RegexConfiguration(
						cache = config.getBoolean(CONF_CORE_EXTRACT_REGEXP_CACHE)),
					xpath = XPathConfiguration(
						cache = config.getBoolean(CONF_CORE_EXTRACT_XPATH_CACHE),
						saxParserFactory = config.getString(CONF_CORE_EXTRACT_XPATH_SAX_PARSER_FACTORY),
						domParserFactory = config.getString(CONF_CORE_EXTRACT_XPATH_DOM_PARSER_FACTORY),
						expandEntityReferences = config.getBoolean(CONF_CORE_EXTRACT_XPATH_EXPAND_ENTITY_REFERENCES),
						namespaceAware = config.getBoolean(CONF_CORE_EXTRACT_XPATH_NAMESPACE_AWARE)),
					jsonPath = JsonPathConfiguration(
						cache = config.getBoolean(CONF_CORE_EXTRACT_JSONPATH_CACHE)),
					css = CssConfiguration(
						cache = config.getBoolean(CONF_CORE_EXTRACT_CSS_CACHE))),
				timeOut = TimeOutConfiguration(
					simulation = config.getInt(CONF_CORE_TIMEOUT_SIMULATION),
					actor = config.getInt(CONF_CORE_TIMEOUT_ACTOR)),
				directory = DirectoryConfiguration(
					data = config.getString(CONF_CORE_DIRECTORY_DATA),
					requestBodies = config.getString(CONF_CORE_DIRECTORY_REQUEST_BODIES),
					sources = config.getString(CONF_CORE_DIRECTORY_SIMULATIONS),
					binaries = config.getString(CONF_CORE_DIRECTORY_BINARIES).trimToOption,
					reportsOnly = config.getString(CONF_CORE_DIRECTORY_REPORTS_ONLY).trimToOption,
					results = config.getString(CONF_CORE_DIRECTORY_RESULTS)),
				zinc = ZincConfiguration(
					jvmArgs = config.getString(CONF_CORE_ZINC_JVM_ARGS).split(" "))),
			charting = ChartingConfiguration(
				noReports = config.getBoolean(CONF_CHARTING_NO_REPORTS),
				statsTsvSeparator = config.getString(CONF_CHARTING_STATS_TSV_SEPARATOR),
				maxPlotsPerSeries = config.getInt(CONF_CHARTING_MAX_PLOTS_PER_SERIES),
				accuracy = config.getInt(CONF_CHARTING_ACCURACY),
				indicators = IndicatorsConfiguration(
					lowerBound = config.getInt(CONF_CHARTING_INDICATORS_LOWER_BOUND),
					higherBound = config.getInt(CONF_CHARTING_INDICATORS_HIGHER_BOUND),
					percentile1 = config.getInt(CONF_CHARTING_INDICATORS_PERCENTILE1),
					percentile2 = config.getInt(CONF_CHARTING_INDICATORS_PERCENTILE2))),
			http = HttpConfiguration(
				baseURLs = config.getString(CONF_HTTP_BASE_URLS).toStringSeq,
				proxy = config.getString(CONF_HTTP_PROXY_HOST).trimToOption.map { host =>
					val port = config.getInt(CONF_HTTP_PROXY_PORT)
					val securedPort = config.getInt(CONF_HTTP_PROXY_SECURED_PORT) match {
						case -1 => None
						case p => Some(p)
					}
					val credentials = config.getString(CONF_HTTP_PROXY_USERNAME).trimToOption.map(username => Credentials(username, config.getString(CONF_HTTP_PROXY_PASSWORD)))
					Proxy(host, port, securedPort, credentials)
				},
				followRedirect = config.getBoolean(CONF_HTTP_FOLLOW_REDIRECT),
				autoReferer = config.getBoolean(CONF_HTTP_AUTO_REFERER),
				cache = config.getBoolean(CONF_HTTP_CACHE),
				cacheELFileBodies = config.getBoolean(CONF_HTTP_CACHE_EL_FILE_BODIES),
				cacheRawFileBodies = config.getBoolean(CONF_HTTP_CACHE_RAW_FILE_BODIES),
				discardResponseChunks = config.getBoolean(CONF_HTTP_DISCARD_RESPONSE_CHUNKS),
				shareClient = config.getBoolean(CONF_HTTP_SHARE_CLIENT),
				shareConnections = config.getBoolean(CONF_HTTP_SHARE_CONNECTIONS),
				basicAuth = config.getString(CONF_HTTP_BASIC_AUTH_USERNAME).trimToOption.map(username => Credentials(username, config.getString(CONF_HTTP_BASIC_AUTH_PASSWORD))),
				warmUpUrl = config.getString(CONF_HTTP_WARM_UP_URL).trimToOption,
				ssl = {
					def storeConfig(typeKey: String, fileKey: String, passwordKey: String, algorithmKey: String) = {

						val storeType = config.getString(typeKey).trimToOption
						val storeFile = config.getString(fileKey).trimToOption
						val storePassword = config.getString(passwordKey)
						val storeAlgorithm = config.getString(algorithmKey).trimToOption

						storeFile.map(StoreConfiguration(storeType, _, storePassword, storeAlgorithm))
					}

					val trustStore = storeConfig(CONF_HTTP_SSL_TRUST_STORE_TYPE, CONF_HTTP_SSL_TRUST_STORE_FILE, CONF_HTTP_SSL_TRUST_STORE_PASSWORD, CONF_HTTP_SSL_TRUST_STORE_ALGORITHM)
					val keyStore = storeConfig(CONF_HTTP_SSL_KEY_STORE_TYPE, CONF_HTTP_SSL_KEY_STORE_FILE, CONF_HTTP_SSL_KEY_STORE_PASSWORD, CONF_HTTP_SSL_KEY_STORE_ALGORITHM)

					SslConfiguration(trustStore, keyStore)
				},
				ahc = AHCConfiguration(
					provider = config.getString(CONF_HTTP_AHC_PROVIDER),
					allowPoolingConnection = config.getBoolean(CONF_HTTP_AHC_ALLOW_POOLING_CONNECTION),
					allowSslConnectionPool = config.getBoolean(CONF_HTTP_AHC_ALLOW_SSL_CONNECTION_POOL),
					compressionEnabled = config.getBoolean(CONF_HTTP_AHC_COMPRESSION_ENABLED),
					connectionTimeOut = config.getInt(CONF_HTTP_AHC_CONNECTION_TIMEOUT),
					idleConnectionInPoolTimeOutInMs = config.getInt(CONF_HTTP_AHC_IDLE_CONNECTION_IN_POOL_TIMEOUT_IN_MS),
					idleConnectionTimeOutInMs = config.getInt(CONF_HTTP_AHC_IDLE_CONNECTION_TIMEOUT_IN_MS),
					maxConnectionLifeTimeInMs = config.getInt(CONF_HTTP_AHC_MAX_CONNECTION_LIFETIME_IN_MS),
					ioThreadMultiplier = config.getInt(CONF_HTTP_AHC_IO_THREAD_MULTIPLIER),
					maximumConnectionsPerHost = config.getInt(CONF_HTTP_AHC_MAXIMUM_CONNECTIONS_PER_HOST),
					maximumConnectionsTotal = config.getInt(CONF_HTTP_AHC_MAXIMUM_CONNECTIONS_TOTAL),
					maxRetry = config.getInt(CONF_HTTP_AHC_MAX_RETRY),
					requestCompressionLevel = config.getInt(CONF_HTTP_AHC_REQUEST_COMPRESSION_LEVEL),
					requestTimeOutInMs = config.getInt(CONF_HTTP_AHC_REQUEST_TIMEOUT_IN_MS),
					useProxyProperties = config.getBoolean(CONF_HTTP_AHC_USE_PROXY_PROPERTIES),
					userAgent = config.getString(CONF_HTTP_AHC_USER_AGENT),
					useRawUrl = config.getBoolean(CONF_HTTP_AHC_USE_RAW_URL),
					rfc6265CookieEncoding = config.getBoolean(CONF_HTTP_AHC_RFC6265_COOKIE_ENCODING))),
			data = DataConfiguration(
				dataWriterClasses = config.getString(CONF_DATA_WRITER_CLASS_NAMES).toStringSeq.map {
					case "console" => "io.gatling.core.result.writer.ConsoleDataWriter"
					case "file" => "io.gatling.core.result.writer.FileDataWriter"
					case "graphite" => "io.gatling.metrics.GraphiteDataWriter"
					case "jdbc" => "io.gatling.jdbc.result.writer.JdbcDataWriter"
					case clazz => clazz
				},
				dataReaderClass = (config.getString(CONF_DATA_READER_CLASS_NAME)).trim match {
					case "file" => "io.gatling.charts.result.reader.FileDataReader"
					case clazz => clazz
				},
				console = ConsoleDataWriterConfiguration(
					light = config.getBoolean(CONF_DATA_CONSOLE_LIGHT)),
				file = FileDataWriterConfiguration(
					bufferSize = config.getInt(CONF_DATA_FILE_BUFFER_SIZE)),
				graphite = GraphiteDataWriterConfiguration(
					light = config.getBoolean(CONF_DATA_GRAPHITE_LIGHT),
					host = config.getString(CONF_DATA_GRAPHITE_HOST),
					port = config.getInt(CONF_DATA_GRAPHITE_PORT),
					protocol = config.getString(CONF_DATA_GRAPHITE_PROTOCOL),
					rootPathPrefix = config.getString(CONF_DATA_GRAPHITE_ROOT_PATH_PREFIX),
					bucketWidth = config.getInt(CONF_DATA_GRAPHITE_BUCKET_WIDTH),
					bufferSize = config.getInt(CONF_DATA_GRAPHITE_BUFFER_SIZE)),
				jdbc = JDBCDataWriterConfiguration(
					db = DBConfiguration(
						url = config.getString(CONF_DATA_JDBC_DB_URL),
						username = config.getString(CONF_DATA_JDBC_DB_USERNAME),
						password = config.getString(CONF_DATA_JDBC_DB_PASSWORD)),
					bufferSize = config.getInt(CONF_DATA_JDBC_BUFFER_SIZE),
					createStatements = CreateStatements(
						createRunRecordTable = config.getString(CONF_DATA_JDBC_CREATE_RUN_RECORD_TABLE).trimToOption,
						createRequestRecordTable = config.getString(CONF_DATA_JDBC_CREATE_REQUEST_RECORD_TABLE).trimToOption,
						createScenarioRecordTable = config.getString(CONF_DATA_JDBC_CREATE_SCENARIO_RECORD_TABLE).trimToOption,
						createGroupRecordTable = config.getString(CONF_DATA_JDBC_CREATE_GROUP_RECORD_TABLE).trimToOption),
					insertStatements = InsertStatements(
						insertRunRecord = config.getString(CONF_DATA_JDBC_INSERT_RUN_RECORD).trimToOption,
						insertRequestRecord = config.getString(CONF_DATA_JDBC_INSERT_REQUEST_RECORD).trimToOption,
						insertScenarioRecord = config.getString(CONF_DATA_JDBC_INSERT_SCENARIO_RECORD).trimToOption,
						insertGroupRecord = config.getString(CONF_DATA_JDBC_INSERT_GROUP_RECORD).trimToOption))),
			config)
	}
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
	zinc: ZincConfiguration) {

	val charSet = Charset.forName(encoding)
	val codec: Codec = charSet
}

case class TimeOutConfiguration(
	simulation: Int,
	actor: Int)

case class ExtractConfiguration(
	regex: RegexConfiguration,
	xpath: XPathConfiguration,
	jsonPath: JsonPathConfiguration,
	css: CssConfiguration)

case class RegexConfiguration(
	cache: Boolean)

case class XPathConfiguration(
	cache: Boolean,
	saxParserFactory: String,
	domParserFactory: String,
	expandEntityReferences: Boolean,
	namespaceAware: Boolean)

case class JsonPathConfiguration(
	cache: Boolean)

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
	baseURLs: Seq[String],
	proxy: Option[Proxy],
	followRedirect: Boolean,
	autoReferer: Boolean,
	cache: Boolean,
	cacheELFileBodies: Boolean,
	cacheRawFileBodies: Boolean,
	discardResponseChunks: Boolean,
	shareClient: Boolean,
	shareConnections: Boolean,
	basicAuth: Option[Credentials],
	warmUpUrl: Option[String],
	ssl: SslConfiguration,
	ahc: AHCConfiguration)

case class Proxy(
	host: String,
	port: Int,
	securePort: Option[Int] = None,
	credentials: Option[Credentials] = None)

case class Credentials(
	username: String,
	password: String)

case class AHCConfiguration(
	provider: String,
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
	requestCompressionLevel: Int,
	requestTimeOutInMs: Int,
	useProxyProperties: Boolean,
	userAgent: String,
	useRawUrl: Boolean,
	rfc6265CookieEncoding: Boolean)

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
	bucketWidth: Int,
	bufferSize: Int)

case class GatlingConfiguration(
	core: CoreConfiguration,
	charting: ChartingConfiguration,
	http: HttpConfiguration,
	data: DataConfiguration,
	config: Config)
