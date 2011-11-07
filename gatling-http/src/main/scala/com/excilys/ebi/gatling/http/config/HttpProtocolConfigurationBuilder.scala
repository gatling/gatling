/**
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.http.config
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