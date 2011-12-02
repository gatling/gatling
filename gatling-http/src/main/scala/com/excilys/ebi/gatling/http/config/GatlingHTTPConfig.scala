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
}