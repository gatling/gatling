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

import com.excilys.ebi.gatling.core.config.GatlingConfig.config

object GatlingHTTPConfig {
	val GATLING_HTTP_CONFIG_PROVIDER_CLASS = {
		val chosenProvider = config.getString("gatling.http.provider", "Netty")
		new StringBuilder("com.ning.http.client.providers.").append(chosenProvider.toLowerCase).append(".").append(chosenProvider).append("AsyncHttpProvider").toString
	}
	val GATLING_HTTP_CONFIG_CONNECTION_TIMEOUT = config.getInt("gatling.http.connectionTimeout", 60000)
	val GATLING_HTTP_CONFIG_COMPRESSION_ENABLED = config.getBoolean("gatling.http.compressionEnabled", true)
	val GATLING_HTTP_CONFIG_REQUEST_TIMEOUT = config.getInt("gatling.http.requestTimeout", 60000)
	val GATLING_HTTP_CONFIG_MAX_RETRY = config.getInt("gatling.http.maxRetry", 5)
	val GATLING_HTTP_CONFIG_ALLOW_POOLING_CONNECTION = config.getBoolean("gatling.http.allowPoolingConnection", true)
}