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
package com.excilys.ebi.gatling.core;

public class ConfigurationConstants {

	public static final String CONF_SIMULATION_RUN_NAME = "gatling.simulation.runName";
	public static final String CONF_SIMULATION_RUN_DESCRIPTION = "gatling.simulation.runDescription";
	public static final String CONF_SIMULATION_ENCODING = "gatling.simulation.encoding";
	public static final String CONF_SIMULATION_CLASSES = "gatling.simulation.classes";

	public static final String CONF_TIME_OUT_SIMULATION = "gatling.timeOut.simulation";
	public static final String CONF_TIME_OUT_ACTOR = "gatling.timeOut.actor";

	public static final String CONF_DIRECTORY_DATA = "gatling.directory.data";
	public static final String CONF_DIRECTORY_REQUEST_BODIES = "gatling.directory.requestBodies";
	public static final String CONF_DIRECTORY_SIMULATIONS = "gatling.directory.simulations";
	public static final String CONF_DIRECTORY_BINARIES = "gatling.directory.binaries";
	public static final String CONF_DIRECTORY_REPORTS_ONLY = "gatling.directory.reportsOnly";
	public static final String CONF_DIRECTORY_RESULTS = "gatling.directory.results";

	public static final String CONF_CHARTING_NO_REPORTS = "gatling.charting.noReports";
	public static final String CONF_CHARTING_MAX_PLOTS_PER_SERIES = "gatling.charting.maxPlotPerSeries";
	public static final String CONF_CHARTING_INDICATORS_LOWER_BOUND = "gatling.charting.indicators.lowerBound";
	public static final String CONF_CHARTING_INDICATORS_HIGHER_BOUND = "gatling.charting.indicators.higherBound";
	public static final String CONF_CHARTING_INDICATORS_PERCENTILE1 = "gatling.charting.indicators.percentile1";
	public static final String CONF_CHARTING_INDICATORS_PERCENTILE2 = "gatling.charting.indicators.percentile2";

	public static final String CONF_HTTP_PROVIDER = "gatling.http.provider";
	public static final String CONF_HTTP_ALLOW_POOLING_CONNECTION = "gatling.http.allowPoolingConnection";
	public static final String CONF_HTTP_ALLOW_SSL_CONNECTION_POOL = "gatling.http.allowSslConnectionPool";
	public static final String CONF_HTTP_COMPRESSION_ENABLED = "gatling.http.compressionEnabled";
	public static final String CONF_HTTP_CONNECTION_TIMEOUT = "gatling.http.connectionTimeout";
	public static final String CONF_HTTP_IDLE_CONNECTION_IN_POOL_TIMEOUT_IN_MS = "gatling.http.idleConnectionInPoolTimeoutInMs";
	public static final String CONF_HTTP_IDLE_CONNECTION_TIMEOUT_IN_MS = "gatling.http.idleConnectionTimeoutInMs";
	public static final String CONF_HTTP_IO_THREAD_MULTIPLIER = "gatling.http.ioThreadMultiplier";
	public static final String CONF_HTTP_MAXIMUM_CONNECTIONS_PER_HOST = "gatling.http.maximumConnectionsPerHost";
	public static final String CONF_HTTP_MAXIMUM_CONNECTIONS_TOTAL = "gatling.http.maximumConnectionsTotal";
	public static final String CONF_HTTP_MAX_RETRY = "gatling.http.maxRetry";
	public static final String CONF_HTTP_REQUEST_COMPRESSION_LEVEL = "gatling.http.requestCompressionLevel";
	public static final String CONF_HTTP_REQUEST_TIMEOUT_IN_MS = "gatling.http.requestTimeoutInMs";
	public static final String CONF_HTTP_USE_PROXY_PROPERTIES = "gatling.http.useProxyProperties";
	public static final String CONF_HTTP_USER_AGENT = "gatling.http.userAgent";
	public static final String CONF_HTTP_USE_RAW_URL = "gatling.http.useRawUrl";

	public static final String CONF_DATA_WRITER_CLASS_NAMES = "gatling.data.writers";
	public static final String CONF_DATA_READER_CLASS_NAME = "gatling.data.reader";

	public static final String CONF_GRAPHITE_HOST = "gatling.graphite.host";
	public static final String CONF_GRAPHITE_PORT = "gatling.graphite.port";
	public static final String CONF_GRAPHITE_PERIOD = "gatling.graphite.period";
	public static final String CONF_GRAPHITE_TIMEUNIT = "gatling.graphite.timeUnit";
}
