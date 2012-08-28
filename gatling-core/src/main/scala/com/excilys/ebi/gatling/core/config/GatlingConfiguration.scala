/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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

import java.util.Properties

import scala.collection.JavaConversions.asScalaBuffer

import com.excilys.ebi.gatling.core.ConfigurationConstants._
import com.typesafe.config.{ Config, ConfigFactory }

import grizzled.slf4j.Logging
import java.util.concurrent.TimeUnit

/**
 * Configuration loader of Gatling
 */
object GatlingConfiguration extends Logging {

	var configuration: GatlingConfiguration = _

	def setUp(props: Properties) {
		val classLoader = getClass.getClassLoader

		val defaultsConfig = ConfigFactory.parseResources(classLoader, "gatling-defaults.conf")
		val customConfig = ConfigFactory.parseResources(classLoader, "gatling.conf")
		val propertiesConfig = ConfigFactory.parseProperties(props)

		val config = propertiesConfig.withFallback(customConfig).withFallback(defaultsConfig)

		configuration = GatlingConfiguration(
			simulation = SimulationConfiguration(
				runName = config.getString(CONF_SIMULATION_RUN_NAME),
				runDescription = config.getString(CONF_SIMULATION_RUN_DESCRIPTION),
				encoding = config.getString(CONF_SIMULATION_ENCODING),
				classes = config.getStringList(CONF_SIMULATION_CLASSES).toList),
			timeOut = TimeOutConfiguration(
				simulation = config.getInt(CONF_TIME_OUT_SIMULATION),
				actor = config.getInt(CONF_TIME_OUT_ACTOR)),
			directory = DirectoryConfiguration(
				data = config.getString(CONF_DIRECTORY_DATA),
				requestBodies = config.getString(CONF_DIRECTORY_REQUEST_BODIES),
				sources = config.getString(CONF_DIRECTORY_SIMULATIONS),
				binaries = config.getString(CONF_DIRECTORY_BINARIES),
				reportsOnly = config.getString(CONF_DIRECTORY_REPORTS_ONLY),
				results = config.getString(CONF_DIRECTORY_RESULTS)),
			charting = ChartingConfiguration(
				noReports = config.getBoolean(CONF_CHARTING_NO_REPORTS),
				maxPlotsPerSeries = config.getInt(CONF_CHARTING_MAX_PLOTS_PER_SERIES),
				indicators = IndicatorsConfiguration(
					lowerBound = config.getInt(CONF_CHARTING_INDICATORS_LOWER_BOUND),
					higherBound = config.getInt(CONF_CHARTING_INDICATORS_HIGHER_BOUND),
					percentile1 = config.getInt(CONF_CHARTING_INDICATORS_PERCENTILE1),
					percentile2 = config.getInt(CONF_CHARTING_INDICATORS_PERCENTILE2))),
			http = HttpConfiguration(
				provider = config.getString(CONF_HTTP_PROVIDER),
				allowPoolingConnection = config.getBoolean(CONF_HTTP_ALLOW_POOLING_CONNECTION),
				allowSslConnectionPool = config.getBoolean(CONF_HTTP_ALLOW_SSL_CONNECTION_POOL),
				compressionEnabled = config.getBoolean(CONF_HTTP_COMPRESSION_ENABLED),
				connectionTimeOut = config.getInt(CONF_HTTP_CONNECTION_TIMEOUT),
				idleConnectionInPoolTimeOutInMs = config.getInt(CONF_HTTP_IDLE_CONNECTION_IN_POOL_TIMEOUT_IN_MS),
				idleConnectionTimeOutInMs = config.getInt(CONF_HTTP_IDLE_CONNECTION_TIMEOUT_IN_MS),
				ioThreadMultiplier = config.getInt(CONF_HTTP_IO_THREAD_MULTIPLIER),
				maximumConnectionsPerHost = config.getInt(CONF_HTTP_MAXIMUM_CONNECTIONS_PER_HOST),
				maximumConnectionsTotal = config.getInt(CONF_HTTP_MAXIMUM_CONNECTIONS_TOTAL),
				maxRetry = config.getInt(CONF_HTTP_MAX_RETRY),
				requestCompressionLevel = config.getInt(CONF_HTTP_REQUEST_COMPRESSION_LEVEL),
				requestTimeOutInMs = config.getInt(CONF_HTTP_REQUEST_TIMEOUT_IN_MS),
				useProxyProperties = config.getBoolean(CONF_HTTP_USE_PROXY_PROPERTIES),
				userAgent = config.getString(CONF_HTTP_USER_AGENT),
				userRawUrl = config.getBoolean(CONF_HTTP_USE_RAW_URL)),
			data = DataConfiguration(
				dataWriterClasses = config.getStringList(CONF_DATA_WRITER_CLASS_NAMES).toList,
				dataReaderClass = config.getString(CONF_DATA_READER_CLASS_NAME)),
			graphite = GraphiteConfiguration(
				host = config.getString(CONF_GRAPHITE_HOST),
				port = config.getInt(CONF_GRAPHITE_PORT),
				period = config.getLong(CONF_GRAPHITE_PERIOD),
				timeUnit = TimeUnit.valueOf(config.getString(CONF_GRAPHITE_TIMEUNIT).toUpperCase)),
			config)
	}
}

case class SimulationConfiguration(
	runName: String,
	runDescription: String,
	encoding: String,
	classes: List[String])

case class TimeOutConfiguration(
	simulation: Int,
	actor: Int)

case class DirectoryConfiguration(
	data: String,
	requestBodies: String,
	sources: String,
	binaries: String,
	reportsOnly: String,
	results: String)

case class ChartingConfiguration(
	noReports: Boolean,
	maxPlotsPerSeries: Int,
	indicators: IndicatorsConfiguration)

case class IndicatorsConfiguration(
	lowerBound: Int,
	higherBound: Int,
	percentile1: Int,
	percentile2: Int)

case class HttpConfiguration(
	provider: String,
	allowPoolingConnection: Boolean,
	allowSslConnectionPool: Boolean,
	compressionEnabled: Boolean,
	connectionTimeOut: Int,
	idleConnectionInPoolTimeOutInMs: Int,
	idleConnectionTimeOutInMs: Int,
	ioThreadMultiplier: Int,
	maximumConnectionsPerHost: Int,
	maximumConnectionsTotal: Int,
	maxRetry: Int,
	requestCompressionLevel: Int,
	requestTimeOutInMs: Int,
	useProxyProperties: Boolean,
	userAgent: String,
	userRawUrl: Boolean)

case class DataConfiguration(
	dataWriterClasses: List[String],
	dataReaderClass: String)

case class GraphiteConfiguration(
	host : String,
	port : Int,
	period : Long,
	timeUnit : TimeUnit)

case class GatlingConfiguration(
	simulation: SimulationConfiguration,
	timeOut: TimeOutConfiguration,
	directory: DirectoryConfiguration,
	charting: ChartingConfiguration,
	http: HttpConfiguration,
	data: DataConfiguration,
	graphite : GraphiteConfiguration,
	config: Config)
