package com.excilys.ebi.gatling.http.config.builder
import com.excilys.ebi.gatling.http.config.HttpProtocolConfiguration

object HttpProtocolConfigurationBuilder {
	def httpConfig = new HttpProtocolConfigurationBuilder(None)

	implicit def toHttpProtocolConfiguration(builder: HttpProtocolConfigurationBuilder) = builder.build
}
class HttpProtocolConfigurationBuilder(baseURL: Option[String]) {
	def baseURL(baseurl: String) = new HttpProtocolConfigurationBuilder(Some(baseurl))

	def build = new HttpProtocolConfiguration(baseURL)
}