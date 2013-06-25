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
package com.excilys.ebi.gatling.core.config

import java.util.{ HashMap => JHashMap, Map => JMap }

import scala.collection.JavaConversions.asScalaBuffer

import com.excilys.ebi.gatling.core.ConfigurationConstants._
import com.excilys.ebi.gatling.core.util.StringHelper.trimToOption
import com.typesafe.config.{ Config, ConfigFactory }

import grizzled.slf4j.Logging

/**
 * Configuration loader of Gatling
 */
object GatlingConfiguration extends Logging {

	var configuration: GatlingConfiguration = _

	def setUp(props: JMap[String, Any] = new JHashMap) {

		def toStringSeq(string: String): Seq[String] = string.trim match {
			case "" => Nil
			case s => s.split(",").map(_.trim)
		}

		val classLoader = getClass.getClassLoader

		val defaultsConfig = ConfigFactory.parseResources(classLoader, "gatling-defaults.conf")
		val customConfig = ConfigFactory.parseResources(classLoader, "gatling.conf")
		val propertiesConfig = ConfigFactory.parseMap(props)

		val config = ConfigFactory.systemProperties.withFallback(propertiesConfig).withFallback(customConfig).withFallback(defaultsConfig)

		configuration = GatlingConfiguration(
			core = CoreConfiguration(
				outputDirectoryBaseName = trimToOption(config.getString(CONF_CORE_OUTPUT_DIRECTORY_BASE_NAME)),
				runDescription = trimToOption(config.getString(CONF_CORE_RUN_DESCRIPTION)),
				encoding = config.getString(CONF_CORE_ENCODING),
				simulationClass = trimToOption(config.getString(CONF_CORE_SIMULATION_CLASS)),
				extract = ExtractConfiguration(
					regex = RegexConfiguration(
						cache = config.getBoolean(CONF_CORE_EXTRACT_REGEXP_CACHE)),
					xpath = XPathConfiguration(
						cache = config.getBoolean(CONF_CORE_EXTRACT_XPATH_CACHE)),
					jsonPath = JsonPathConfiguration(
						cache = config.getBoolean(CONF_CORE_EXTRACT_JSONPATH_CACHE)),
					css = CssConfiguration(
						engine = config.getString(CONF_CORE_EXTRACT_CSS_ENGINE) match {
							case "jsoup" => Jsoup
							case _ => Jodd
						})),
				timeOut = TimeOutConfiguration(
					simulation = config.getInt(CONF_CORE_TIMEOUT_SIMULATION),
					actor = config.getInt(CONF_CORE_TIMEOUT_ACTOR)),
				directory = DirectoryConfiguration(
					data = config.getString(CONF_CORE_DIRECTORY_DATA),
					requestBodies = config.getString(CONF_CORE_DIRECTORY_REQUEST_BODIES),
					sources = config.getString(CONF_CORE_DIRECTORY_SIMULATIONS),
					binaries = trimToOption(config.getString(CONF_CORE_DIRECTORY_BINARIES)),
					reportsOnly = trimToOption(config.getString(CONF_CORE_DIRECTORY_REPORTS_ONLY)),
					results = config.getString(CONF_CORE_DIRECTORY_RESULTS))),
			charting = ChartingConfiguration(
				noReports = config.getBoolean(CONF_CHARTING_NO_REPORTS),
				maxPlotsPerSeries = config.getInt(CONF_CHARTING_MAX_PLOTS_PER_SERIES),
				accuracy = config.getInt(CONF_CHARTING_ACCURACY),
				indicators = IndicatorsConfiguration(
					lowerBound = config.getInt(CONF_CHARTING_INDICATORS_LOWER_BOUND),
					higherBound = config.getInt(CONF_CHARTING_INDICATORS_HIGHER_BOUND),
					percentile1 = config.getInt(CONF_CHARTING_INDICATORS_PERCENTILE1),
					percentile2 = config.getInt(CONF_CHARTING_INDICATORS_PERCENTILE2))),
			http = HttpConfiguration(
				allowPoolingConnection = config.getBoolean(CONF_HTTP_ALLOW_POOLING_CONNECTION),
				allowSslConnectionPool = config.getBoolean(CONF_HTTP_ALLOW_SSL_CONNECTION_POOL),
				compressionEnabled = config.getBoolean(CONF_HTTP_COMPRESSION_ENABLED),
				connectionTimeOut = config.getInt(CONF_HTTP_CONNECTION_TIMEOUT),
				idleConnectionInPoolTimeOutInMs = config.getInt(CONF_HTTP_IDLE_CONNECTION_IN_POOL_TIMEOUT_IN_MS),
				idleConnectionTimeOutInMs = config.getInt(CONF_HTTP_IDLE_CONNECTION_TIMEOUT_IN_MS),
				maxConnectionLifeTimeInMs = config.getInt(CONF_HTTP_MAX_CONNECTION_LIFETIME_IN_MS),
				ioThreadMultiplier = config.getInt(CONF_HTTP_IO_THREAD_MULTIPLIER),
				maximumConnectionsPerHost = config.getInt(CONF_HTTP_MAXIMUM_CONNECTIONS_PER_HOST),
				maximumConnectionsTotal = config.getInt(CONF_HTTP_MAXIMUM_CONNECTIONS_TOTAL),
				maxRetry = config.getInt(CONF_HTTP_MAX_RETRY),
				requestCompressionLevel = config.getInt(CONF_HTTP_REQUEST_COMPRESSION_LEVEL),
				requestTimeOutInMs = config.getInt(CONF_HTTP_REQUEST_TIMEOUT_IN_MS),
				useProxyProperties = config.getBoolean(CONF_HTTP_USE_PROXY_PROPERTIES),
				userAgent = config.getString(CONF_HTTP_USER_AGENT),
				useRawUrl = config.getBoolean(CONF_HTTP_USE_RAW_URL),
				warmUpUrl = {
					val value = config.getString(CONF_HTTP_WARM_UP_URL).trim
					if (value.isEmpty) None else Some(value)
				},
				rfc6265CookieEncoding = config.getBoolean(CONF_HTTP_RFC6265_COOKIE_ENCODING),
				ssl = {
					def storeConfig(typeKey: String, fileKey: String, passwordKey: String, algorithmKey: String) = {

						def toOption(string: String) = {
							val trimmed = string.trim
							if (trimmed.isEmpty) None else Some(trimmed)
						}

						val storeType = toOption(config.getString(typeKey))
						val storeFile = toOption(config.getString(fileKey))
						val storePassword = config.getString(passwordKey)
						val storeAlgorithm = toOption(config.getString(algorithmKey))

						storeType.map { t =>
							StoreConfiguration(t, storeFile.getOrElse(throw new IllegalArgumentException(typeKey + " defined as " + t + " but store file isn't defined")), storePassword, storeAlgorithm)
						}
					}

					val trustStore = storeConfig(CONF_HTTP_SSL_TRUST_STORE_TYPE, CONF_HTTP_SSL_TRUST_STORE_FILE, CONF_HTTP_SSL_TRUST_STORE_PASSWORD, CONF_HTTP_SSL_TRUST_STORE_ALGORITHM)
					val keyStore = storeConfig(CONF_HTTP_SSL_KEY_STORE_TYPE, CONF_HTTP_SSL_KEY_STORE_FILE, CONF_HTTP_SSL_KEY_STORE_PASSWORD, CONF_HTTP_SSL_KEY_STORE_ALGORITHM)

					SslConfiguration(trustStore, keyStore)
				}),
			data = DataConfiguration(
				dataWriterClasses = toStringSeq(config.getString(CONF_DATA_WRITER_CLASS_NAMES)).map {
					case "console" => "com.excilys.ebi.gatling.core.result.writer.ConsoleDataWriter"
					case "file" => "com.excilys.ebi.gatling.core.result.writer.FileDataWriter"
					case "graphite" => "com.excilys.ebi.gatling.metrics.GraphiteDataWriter"
					case clazz => clazz
				},
				dataReaderClass = (config.getString(CONF_DATA_READER_CLASS_NAME)).trim match {
					case "file" => "com.excilys.ebi.gatling.charts.result.reader.FileDataReader"
					case clazz => clazz
				},
				console = ConsoleConfiguration(
					light = config.getBoolean(CONF_DATA_CONSOLE_LIGHT)),
				file = FileDataWriterConfiguration(
					bufferSize = config.getInt(CONF_DATA_FILE_BUFFER_SIZE)),
				graphite = GraphiteConfiguration(
					light = config.getBoolean(CONF_DATA_GRAPHITE_LIGHT),
					host = config.getString(CONF_DATA_GRAPHITE_HOST),
					port = config.getInt(CONF_DATA_GRAPHITE_PORT),
					rootPathPrefix = config.getString(CONF_DATA_GRAPHITE_ROOT_PATH_PREFIX),
					bucketWidth = config.getInt(CONF_DATA_GRAPHITE_BUCKET_WIDTH))),
			config)
	}
}

case class CoreConfiguration(
	outputDirectoryBaseName: Option[String],
	runDescription: Option[String],
	encoding: String,
	simulationClass: Option[String],
	extract: ExtractConfiguration,
	timeOut: TimeOutConfiguration,
	directory: DirectoryConfiguration)

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
	cache: Boolean)

case class CssConfiguration(
	engine: CssEngine)

case class TimeOutConfiguration(
	simulation: Int,
	actor: Int)

case class DirectoryConfiguration(
	data: String,
	requestBodies: String,
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
	percentile1: Int,
	percentile2: Int)

case class HttpConfiguration(
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
	warmUpUrl: Option[String],
	rfc6265CookieEncoding: Boolean,
	ssl: SslConfiguration)

case class SslConfiguration(
	trustStore: Option[StoreConfiguration],
	keyStore: Option[StoreConfiguration])

case class StoreConfiguration(
	storeType: String,
	file: String,
	password: String,
	algorithm: Option[String])

case class DataConfiguration(
	dataWriterClasses: Seq[String],
	dataReaderClass: String,
	file: FileDataWriterConfiguration,
	console: ConsoleConfiguration,
	graphite: GraphiteConfiguration)

case class FileDataWriterConfiguration(
	bufferSize: Int)

case class ConsoleConfiguration(
	light: Boolean)

case class GraphiteConfiguration(
	light: Boolean,
	host: String,
	port: Int,
	rootPathPrefix: String,
	bucketWidth: Int)

case class GatlingConfiguration(
	core: CoreConfiguration,
	charting: ChartingConfiguration,
	http: HttpConfiguration,
	data: DataConfiguration,
	config: Config)

sealed trait CssEngine
case object Jodd extends CssEngine
case object Jsoup extends CssEngine