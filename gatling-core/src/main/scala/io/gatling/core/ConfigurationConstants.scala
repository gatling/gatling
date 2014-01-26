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
package io.gatling.core

object ConfigurationConstants {

	val CONF_CORE_OUTPUT_DIRECTORY_BASE_NAME = "gatling.core.outputDirectoryBaseName"
	val CONF_CORE_RUN_DESCRIPTION = "gatling.core.runDescription"
	val CONF_CORE_ENCODING = "gatling.core.encoding"
	val CONF_CORE_SIMULATION_CLASS = "gatling.core.simulationClass"
	val CONF_CORE_DISABLE_COMPILER = "gatling.core.disableCompiler"
	val CONF_CORE_MUTE = "gatling.core.mute"

	val CONF_CORE_EXTRACT_REGEXP_CACHE = "gatling.core.extract.regex.cache"
	val CONF_CORE_EXTRACT_XPATH_CACHE = "gatling.core.extract.xpath.cache"
	val CONF_CORE_EXTRACT_JSONPATH_CACHE = "gatling.core.extract.jsonPath.cache"
	val CONF_CORE_EXTRACT_JSONPATH_JACKSON_ALLOW_COMMENTS = "gatling.core.extract.jsonPath.jackson.allowComments"
	val CONF_CORE_EXTRACT_JSONPATH_JACKSON_ALLOW_UNQUOTED_FIELD_NAMES = "gatling.core.extract.jsonPath.jackson.allowUnquotedFieldNames"
	val CONF_CORE_EXTRACT_JSONPATH_JACKSON_ALLOW_SINGLE_QUOTES = "gatling.core.extract.jsonPath.jackson.allowSingleQuotes"
	val CONF_CORE_EXTRACT_CSS_CACHE = "gatling.core.extract.css.cache"

	val CONF_CORE_TIMEOUT_SIMULATION = "gatling.core.timeOut.simulation"
	val CONF_CORE_TIMEOUT_ACTOR = "gatling.core.timeOut.actor"

	val CONF_CORE_DIRECTORY_DATA = "gatling.core.directory.data"
	val CONF_CORE_DIRECTORY_REQUEST_BODIES = "gatling.core.directory.requestBodies"
	val CONF_CORE_DIRECTORY_SIMULATIONS = "gatling.core.directory.simulations"
	val CONF_CORE_DIRECTORY_BINARIES = "gatling.core.directory.binaries"
	val CONF_CORE_DIRECTORY_REPORTS_ONLY = "gatling.core.directory.reportsOnly"
	val CONF_CORE_DIRECTORY_RESULTS = "gatling.core.directory.results"

	val CONF_CORE_ZINC_JVM_ARGS = "gatling.core.zinc.jvmArgs"

	val CONF_CHARTING_NO_REPORTS = "gatling.charting.noReports"
	val CONF_CHARTING_STATS_TSV_SEPARATOR = "gatling.charting.statsTsvSeparator"
	val CONF_CHARTING_MAX_PLOTS_PER_SERIES = "gatling.charting.maxPlotPerSeries"
	val CONF_CHARTING_ACCURACY = "gatling.charting.accuracy"
	val CONF_CHARTING_INDICATORS_LOWER_BOUND = "gatling.charting.indicators.lowerBound"
	val CONF_CHARTING_INDICATORS_HIGHER_BOUND = "gatling.charting.indicators.higherBound"
	val CONF_CHARTING_INDICATORS_PERCENTILE1 = "gatling.charting.indicators.percentile1"
	val CONF_CHARTING_INDICATORS_PERCENTILE2 = "gatling.charting.indicators.percentile2"

	val CONF_HTTP_BASE_URLS = "gatling.http.baseUrls"
	val CONF_HTTP_PROXY_HOST = "gatling.http.proxy.host"
	val CONF_HTTP_PROXY_PORT = "gatling.http.proxy.port"
	val CONF_HTTP_PROXY_SECURED_PORT = "gatling.http.proxy.securedPort"
	val CONF_HTTP_PROXY_USERNAME = "gatling.http.proxy.username"
	val CONF_HTTP_PROXY_PASSWORD = "gatling.http.proxy.password"
	val CONF_HTTP_FOLLOW_REDIRECT = "gatling.http.followRedirect"
	val CONF_HTTP_AUTO_REFERER = "gatling.http.autoReferer"
	val CONF_HTTP_CACHE = "gatling.http.cache"
	val CONF_HTTP_CACHE_EL_FILE_BODIES = "gatling.http.cacheELFileBodies"
	val CONF_HTTP_CACHE_RAW_FILE_BODIES = "gatling.http.cacheRawFileBodies"
	val CONF_HTTP_DISCARD_RESPONSE_CHUNKS = "gatling.http.discardResponseChunks"
	val CONF_HTTP_SHARE_CLIENT = "gatling.http.shareClient"
	val CONF_HTTP_SHARE_CONNECTIONS = "gatling.http.shareConnections"
	val CONF_HTTP_BASIC_AUTH_USERNAME = "gatling.http.basicAuth.username"
	val CONF_HTTP_BASIC_AUTH_PASSWORD = "gatling.http.basicAuth.password"
	val CONF_HTTP_WARM_UP_URL = "gatling.http.warmUpUrl"
	val CONF_HTTP_SSL_TRUST_STORE_TYPE = "gatling.http.ssl.trustStore.type"
	val CONF_HTTP_SSL_TRUST_STORE_FILE = "gatling.http.ssl.trustStore.file"
	val CONF_HTTP_SSL_TRUST_STORE_PASSWORD = "gatling.http.ssl.trustStore.password"
	val CONF_HTTP_SSL_TRUST_STORE_ALGORITHM = "gatling.http.ssl.trustStore.algorithm"
	val CONF_HTTP_SSL_KEY_STORE_TYPE = "gatling.http.ssl.keyStore.type"
	val CONF_HTTP_SSL_KEY_STORE_FILE = "gatling.http.ssl.keyStore.file"
	val CONF_HTTP_SSL_KEY_STORE_PASSWORD = "gatling.http.ssl.keyStore.password"
	val CONF_HTTP_SSL_KEY_STORE_ALGORITHM = "gatling.http.ssl.keyStore.algorithm"
	val CONF_HTTP_AHC_ALLOW_POOLING_CONNECTION = "gatling.http.ahc.allowPoolingConnection"
	val CONF_HTTP_AHC_ALLOW_SSL_CONNECTION_POOL = "gatling.http.ahc.allowSslConnectionPool"
	val CONF_HTTP_AHC_COMPRESSION_ENABLED = "gatling.http.ahc.compressionEnabled"
	val CONF_HTTP_AHC_CONNECTION_TIMEOUT = "gatling.http.ahc.connectionTimeout"
	val CONF_HTTP_AHC_IDLE_CONNECTION_IN_POOL_TIMEOUT_IN_MS = "gatling.http.ahc.idleConnectionInPoolTimeoutInMs"
	val CONF_HTTP_AHC_IDLE_CONNECTION_TIMEOUT_IN_MS = "gatling.http.ahc.idleConnectionTimeoutInMs"
	val CONF_HTTP_AHC_MAX_CONNECTION_LIFETIME_IN_MS = "gatling.http.ahc.maxConnectionLifeTimeInMs"
	val CONF_HTTP_AHC_IO_THREAD_MULTIPLIER = "gatling.http.ahc.ioThreadMultiplier"
	val CONF_HTTP_AHC_MAXIMUM_CONNECTIONS_PER_HOST = "gatling.http.ahc.maximumConnectionsPerHost"
	val CONF_HTTP_AHC_MAXIMUM_CONNECTIONS_TOTAL = "gatling.http.ahc.maximumConnectionsTotal"
	val CONF_HTTP_AHC_MAX_RETRY = "gatling.http.ahc.maxRetry"
	val CONF_HTTP_AHC_REQUEST_COMPRESSION_LEVEL = "gatling.http.ahc.requestCompressionLevel"
	val CONF_HTTP_AHC_REQUEST_TIMEOUT_IN_MS = "gatling.http.ahc.requestTimeoutInMs"
	val CONF_HTTP_AHC_USE_PROXY_PROPERTIES = "gatling.http.ahc.useProxyProperties"
	val CONF_HTTP_AHC_USER_AGENT = "gatling.http.ahc.userAgent"
	val CONF_HTTP_AHC_USE_RAW_URL = "gatling.http.ahc.useRawUrl"
	val CONF_HTTP_AHC_RFC6265_COOKIE_ENCODING = "gatling.http.ahc.rfc6265CookieEncoding"
	val CONF_HTTP_AHC_WEBSOCKET_IDLE_TIMEOUT_IN_MS = "gatling.http.ahc.webSocketIdleTimeoutInMs"
	val CONF_HTTP_AHC_USE_RELATIVE_URIS_WITH_SSL_PROXIES = "gatling.http.ahc.useRelativeURIsWithSSLProxies"

	val CONF_DATA_WRITER_CLASS_NAMES = "gatling.data.writers"
	val CONF_DATA_READER_CLASS_NAME = "gatling.data.reader"

	val CONF_DATA_FILE_BUFFER_SIZE = "gatling.data.file.bufferSize"

	val CONF_DATA_CONSOLE_LIGHT = "gatling.data.console.light"

	val CONF_DATA_GRAPHITE_LIGHT = "gatling.data.graphite.light"
	val CONF_DATA_GRAPHITE_HOST = "gatling.data.graphite.host"
	val CONF_DATA_GRAPHITE_PORT = "gatling.data.graphite.port"
	val CONF_DATA_GRAPHITE_PROTOCOL = "gatling.data.graphite.protocol"
	val CONF_DATA_GRAPHITE_ROOT_PATH_PREFIX = "gatling.data.graphite.rootPathPrefix"
	val CONF_DATA_GRAPHITE_BUCKET_WIDTH = "gatling.data.graphite.bucketWidth"
	val CONF_DATA_GRAPHITE_BUFFER_SIZE = "gatling.data.graphite.bufferSize"

	val CONF_DATA_JDBC_DB_URL = "gatling.data.jdbc.db.url"
	val CONF_DATA_JDBC_DB_USERNAME = "gatling.data.jdbc.db.username"
	val CONF_DATA_JDBC_DB_PASSWORD = "gatling.data.jdbc.db.password"
	val CONF_DATA_JDBC_BUFFER_SIZE = "gatling.data.jdbc.bufferSize"
	val CONF_DATA_JDBC_CREATE_RUN_RECORD_TABLE = "gatling.data.jdbc.create.createRunRecordTable"
	val CONF_DATA_JDBC_CREATE_REQUEST_RECORD_TABLE = "gatling.data.jdbc.create.createRequestRecordTable"
	val CONF_DATA_JDBC_CREATE_SCENARIO_RECORD_TABLE = "gatling.data.jdbc.create.createScenarioRecordTable"
	val CONF_DATA_JDBC_CREATE_GROUP_RECORD_TABLE = "gatling.data.jdbc.create.createGroupRecordTable"
	val CONF_DATA_JDBC_INSERT_RUN_RECORD = "gatling.data.jdbc.insert.insertRunRecord"
	val CONF_DATA_JDBC_INSERT_REQUEST_RECORD = "gatling.data.jdbc.insert.insertRequestRecord"
	val CONF_DATA_JDBC_INSERT_SCENARIO_RECORD = "gatling.data.jdbc.insert.insertScenarioRecord"
	val CONF_DATA_JDBC_INSERT_GROUP_RECORD = "gatling.data.jdbc.insert.insertGroupRecord"
}
