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

import com.ning.http.client.ProxyServer

object HttpProxyBuilder {
	implicit def toHttpProtocolConfigurationBuilder(hpb: HttpProxyBuilder) = {

		def getProxyServer(protocol: ProxyServer.Protocol, port: Int) = {
			val securedProxyServer = for {
				username <- hpb.username
				password <- hpb.password
			} yield new ProxyServer(protocol, hpb.host, port, username, password)

			securedProxyServer.getOrElse(new ProxyServer(protocol, hpb.host, port)).setNtlmDomain(null)
		}

		val httpProxy = getProxyServer(ProxyServer.Protocol.HTTP, hpb.port)

		val httpsProxy = hpb.sslPort.map(getProxyServer(ProxyServer.Protocol.HTTPS, _))

		hpb.configBuilder.addProxies(httpProxy, httpsProxy)
	}
}
class HttpProxyBuilder(val configBuilder: HttpProtocolConfigurationBuilder, val host: String, val port: Int, val sslPort: Option[Int], val username: Option[String], val password: Option[String]) {
	def this(configBuilder: HttpProtocolConfigurationBuilder, host: String, port: Int) = this(configBuilder, host, port, None, None, None)

	def httpsPort(sslPort: Int) = new HttpProxyBuilder(configBuilder, host, port, Some(sslPort), username, password)

	def credentials(username: String, password: String) = new HttpProxyBuilder(configBuilder, host, port, sslPort, Some(username), Some(password))
}