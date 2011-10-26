package com.excilys.ebi.gatling.http.config

import com.excilys.ebi.gatling.core.config.ProtocolConfiguration
import com.ning.http.client.ProxyServer

object HttpProtocolConfiguration {
	val HTTP_PROTOCOL_TYPE = "httpProtocol"
}
class HttpProtocolConfiguration(baseURL: Option[String], proxy: Option[ProxyServer]) extends ProtocolConfiguration {
	import HttpProtocolConfiguration._

	def getBaseUrl = baseURL

	def getProxy = proxy

	def getProtocolType = HTTP_PROTOCOL_TYPE
}