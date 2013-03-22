/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.core

object ConfigurationConstants {

	val CONF_SIMULATION_OUTPUT_DIRECTORY_BASE_NAME = "gatling.simulation.outputDirectoryBaseName"
	val CONF_SIMULATION_RUN_DESCRIPTION = "gatling.simulation.runDescription"
	val CONF_SIMULATION_ENCODING = "gatling.simulation.encoding"
	val CONF_SIMULATION_CLASS = "gatling.simulation.class"

	val CONF_TIME_OUT_SIMULATION = "gatling.timeOut.simulation"
	val CONF_TIME_OUT_ACTOR = "gatling.timeOut.actor"

	val CONF_DIRECTORY_DATA = "gatling.directory.data"
	val CONF_DIRECTORY_REQUEST_BODIES = "gatling.directory.requestBodies"
	val CONF_DIRECTORY_SIMULATIONS = "gatling.directory.simulations"
	val CONF_DIRECTORY_BINARIES = "gatling.directory.binaries"
	val CONF_DIRECTORY_REPORTS_ONLY = "gatling.directory.reportsOnly"
	val CONF_DIRECTORY_RESULTS = "gatling.directory.results"

	val CONF_CHARTING_NO_REPORTS = "gatling.charting.noReports"
	val CONF_CHARTING_STATS_TSV_SEPARATOR = "gatling.charting.statsTsvSeparator"
	val CONF_CHARTING_MAX_PLOTS_PER_SERIES = "gatling.charting.maxPlotPerSeries"
	val CONF_CHARTING_ACCURACY = "gatling.charting.accuracy"
	val CONF_CHARTING_INDICATORS_LOWER_BOUND = "gatling.charting.indicators.lowerBound"
	val CONF_CHARTING_INDICATORS_HIGHER_BOUND = "gatling.charting.indicators.higherBound"
	val CONF_CHARTING_INDICATORS_PERCENTILE1 = "gatling.charting.indicators.percentile1"
	val CONF_CHARTING_INDICATORS_PERCENTILE2 = "gatling.charting.indicators.percentile2"

	val CONF_HTTP_PROVIDER = "gatling.http.provider"
	val CONF_HTTP_ALLOW_POOLING_CONNECTION = "gatling.http.allowPoolingConnection"
	val CONF_HTTP_ALLOW_SSL_CONNECTION_POOL = "gatling.http.allowSslConnectionPool"
	val CONF_HTTP_COMPRESSION_ENABLED = "gatling.http.compressionEnabled"
	val CONF_HTTP_CONNECTION_TIMEOUT = "gatling.http.connectionTimeout"
	val CONF_HTTP_IDLE_CONNECTION_IN_POOL_TIMEOUT_IN_MS = "gatling.http.idleConnectionInPoolTimeoutInMs"
	val CONF_HTTP_IDLE_CONNECTION_TIMEOUT_IN_MS = "gatling.http.idleConnectionTimeoutInMs"
	val CONF_HTTP_IO_THREAD_MULTIPLIER = "gatling.http.ioThreadMultiplier"
	val CONF_HTTP_MAXIMUM_CONNECTIONS_PER_HOST = "gatling.http.maximumConnectionsPerHost"
	val CONF_HTTP_MAXIMUM_CONNECTIONS_TOTAL = "gatling.http.maximumConnectionsTotal"
	val CONF_HTTP_MAX_RETRY = "gatling.http.maxRetry"
	val CONF_HTTP_REQUEST_COMPRESSION_LEVEL = "gatling.http.requestCompressionLevel"
	val CONF_HTTP_REQUEST_TIMEOUT_IN_MS = "gatling.http.requestTimeoutInMs"
	val CONF_HTTP_USE_PROXY_PROPERTIES = "gatling.http.useProxyProperties"
	val CONF_HTTP_USER_AGENT = "gatling.http.userAgent"
	val CONF_HTTP_USE_RAW_URL = "gatling.http.useRawUrl"
	val CONF_HTTP_JSON_FEATURES = "gatling.http.nonStandardJsonSupport"
	val CONF_HTTP_WARM_UP_URL = "gatling.http.warmUpUrl"
	val CONF_HTTP_SSS_TRUST_STORE_TYPE = "gatling.http.ssl.trustStore.type"
	val CONF_HTTP_SSS_TRUST_STORE_FILE = "gatling.http.ssl.trustStore.file"
	val CONF_HTTP_SSS_TRUST_STORE_PASSWORD = "gatling.http.ssl.trustStore.password"
	val CONF_HTTP_SSS_TRUST_STORE_ALGORITHM = "gatling.http.ssl.trustStore.algorithm"
	val CONF_HTTP_SSS_KEY_STORE_TYPE = "gatling.http.ssl.keyStore.type"
	val CONF_HTTP_SSS_KEY_STORE_FILE = "gatling.http.ssl.keyStore.file"
	val CONF_HTTP_SSS_KEY_STORE_PASSWORD = "gatling.http.ssl.keyStore.password"
	val CONF_HTTP_SSS_KEY_STORE_ALGORITHM = "gatling.http.ssl.keyStore.algorithm"

	val CONF_DATA_WRITER_CLASS_NAMES = "gatling.data.writers"
	val CONF_DATA_READER_CLASS_NAME = "gatling.data.reader"

	val CONF_GRAPHITE_HOST = "gatling.graphite.host"
	val CONF_GRAPHITE_PORT = "gatling.graphite.port"
	val CONF_ROOT_PATH_PREFIX = "gatling.graphite.rootPathPrefix"
	val CONF_GRAPHITE_BUCKET_WIDTH = "gatling.graphite.bucketWidth"
}