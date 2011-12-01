package com.excilys.ebi.gatling.http.config
import com.ning.http.client.ProxyServer

object HttpProxyBuilder {
	implicit def toHttpProtocolConfiguration(hpb: HttpProxyBuilder) = {

		def getProxyServer(builder: HttpProxyBuilder, protocol: ProxyServer.Protocol, port: Int) =
			if (builder.username.isDefined && builder.password.isDefined)
				new ProxyServer(protocol, builder.host, port, builder.username.get, builder.password.get)
			else
				new ProxyServer(protocol, builder.host, port)

		val httpProxy = getProxyServer(hpb, ProxyServer.Protocol.HTTP, hpb.port)
		httpProxy.setNtlmDomain(null)

		val httpsProxy =
			hpb.sslPort.map { sslPort =>
				getProxyServer(hpb, ProxyServer.Protocol.HTTPS, sslPort)
			}.map(_.setNtlmDomain(null))

		hpb.configBuilder.addProxies(httpProxy, httpsProxy).build
	}
}
class HttpProxyBuilder(val configBuilder: HttpProtocolConfigurationBuilder, val host: String, val port: Int, val sslPort: Option[Int], val username: Option[String], val password: Option[String]) {
	def this(configBuilder: HttpProtocolConfigurationBuilder, host: String, port: Int) = this(configBuilder, host, port, None, None, None)

	def httpsPort(sslPort: Int) = new HttpProxyBuilder(configBuilder, host, port, Some(sslPort), username, password)

	def credentials(username: String, password: String) = new HttpProxyBuilder(configBuilder, host, port, sslPort, Some(username), Some(password))
}