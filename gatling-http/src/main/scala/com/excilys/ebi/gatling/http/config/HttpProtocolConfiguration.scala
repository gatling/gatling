package com.excilys.ebi.gatling.http.config

import com.excilys.ebi.gatling.core.config.ProtocolConfiguration

object HttpProtocolConfiguration {
	val HTTP_PROTOCOL_TYPE = "httpProtocol"
}
class HttpProtocolConfiguration(baseURL: Option[String]) extends ProtocolConfiguration {
	import HttpProtocolConfiguration._

	def getBaseUrl = baseURL

	def getProtocolType = HTTP_PROTOCOL_TYPE
}