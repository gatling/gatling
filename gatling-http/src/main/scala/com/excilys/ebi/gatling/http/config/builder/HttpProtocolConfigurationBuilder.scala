package com.excilys.ebi.gatling.http.config.builder
import com.excilys.ebi.gatling.http.config.HttpProtocolConfiguration
import com.ning.http.client.ProxyServer

object HttpProtocolConfigurationBuilder {
	def httpConfig = new HttpProtocolConfigurationBuilder(None, None)

	implicit def toHttpProtocolConfiguration(builder: HttpProtocolConfigurationBuilder) = builder.build
}
class HttpProtocolConfigurationBuilder(baseUrl: Option[String], proxy: Option[ProxyServer]) {
	def baseURL(baseurl: String) = new HttpProtocolConfigurationBuilder(Some(baseurl), proxy)

	def proxy(host: String, port: Int): HttpProtocolConfigurationBuilder = proxy(host, port, null, null)

	def proxy(host: String, port: Int, username: String, password: String) = {
		val ps = new ProxyServer(ProxyServer.Protocol.HTTP, host, port, username, password)
		ps.setNtlmDomain(null)
		new HttpProtocolConfigurationBuilder(baseUrl, Some(ps))
	}

	def build = new HttpProtocolConfiguration(baseUrl, proxy)
}