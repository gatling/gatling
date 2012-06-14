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
package com.excilys.ebi.gatling.http.config

import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration

object HttpConfig {
	val GATLING_HTTP_CONFIG_PROVIDER_CLASS = {
		val selectedProvider = configuration.fileConfiguration.getString("gatling.http.provider", "Netty")
		new StringBuilder("com.ning.http.client.providers.").append(selectedProvider.toLowerCase).append(".").append(selectedProvider).append("AsyncHttpProvider").toString
	}

	val GATLING_HTTP_CONFIG_ALLOW_POOLING_CONNECTION = configuration.fileConfiguration.getBoolean("gatling.http.allowPoolingConnection", true)
	val GATLING_HTTP_CONFIG_ALLOW_SSL_CONNECTION_POOL = configuration.fileConfiguration.getBoolean("gatling.http.allowSslConnectionPool", true)
	val GATLING_HTTP_CONFIG_COMPRESSION_ENABLED = configuration.fileConfiguration.getBoolean("gatling.http.compressionEnabled", true)
	val GATLING_HTTP_CONFIG_CONNECTION_TIMEOUT = configuration.fileConfiguration.getInt("gatling.http.connectionTimeout", 60 * 1000)
	val GATLING_HTTP_CONFIG_IDLE_CONNECTION_IN_POOL_TIMEOUT_IN_MS = configuration.fileConfiguration.getInt("gatling.http.idleConnectionInPoolTimeoutInMs", 60 * 1000)
	val GATLING_HTTP_CONFIG_IDLE_CONNECTION_TIMEOUT_IN_MS = configuration.fileConfiguration.getInt("gatling.http.idleConnectionTimeoutInMs", 60 * 1000)
	val GATLING_HTTP_CONFIG_IO_THREAD_MULTIPLIER = configuration.fileConfiguration.getInt("gatling.http.ioThreadMultiplier", 2)
	val GATLING_HTTP_MAXIMUM_CONNECTIONS_PER_HOST = configuration.fileConfiguration.getInt("gatling.http.maximumConnectionsPerHost", -1)
	val GATLING_HTTP_MAXIMUM_CONNECTIONS_TOTAL = configuration.fileConfiguration.getInt("gatling.http.maximumConnectionsTotal", -1)
	val GATLING_HTTP_CONFIG_MAX_RETRY = configuration.fileConfiguration.getInt("gatling.http.maxRetry", 5)
	val GATLING_HTTP_CONFIG_REQUEST_COMPRESSION_LEVEL = configuration.fileConfiguration.getInt("gatling.http.requestCompressionLevel", -1)
	val GATLING_HTTP_CONFIG_REQUEST_TIMEOUT_IN_MS = configuration.fileConfiguration.getInt("gatling.http.requestTimeoutInMs", 60 * 1000)
	val GATLING_HTTP_CONFIG_USE_PROXY_PROPERTIES = configuration.fileConfiguration.getBoolean("gatling.http.useProxyProperties", false)
	val GATLING_HTTP_CONFIG_USER_AGENT = configuration.fileConfiguration.getString("gatling.http.userAgent", "NING/1.0")
	val GATLING_HTTP_CONFIG_USE_RAW_URL = configuration.fileConfiguration.getBoolean("gatling.http.useRawUrl", false)
}