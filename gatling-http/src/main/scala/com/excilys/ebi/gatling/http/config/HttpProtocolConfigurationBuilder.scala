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

/**
 * HttpProtocolConfigurationBuilder class companion
 */
object HttpProtocolConfigurationBuilder {
	def httpConfig = new HttpProtocolConfigurationBuilder(None, None, None, false)

	implicit def toHttpProtocolConfiguration(builder: HttpProtocolConfigurationBuilder) = builder.build
}

/**
 * Builder for HttpProtocolConfiguration used in DSL
 *
 * @param baseUrl the radix of all the URLs that will be used (eg: http://mywebsite.tld)
 * @param proxy a proxy through which all the requests must pass to succeed
 */
class HttpProtocolConfigurationBuilder(baseUrl: Option[String], proxy: Option[ProxyServer], securedProxy: Option[ProxyServer], followRedirect: Boolean) {

	/**
	 * Sets the baseURL of the future HttpProtocolConfiguration
	 *
	 * @param baseurl the base url that will be set
	 */
	def baseURL(baseUrl: String) = new HttpProtocolConfigurationBuilder(Some(baseUrl), proxy, securedProxy, followRedirect)
	
	def followRedirect = new HttpProtocolConfigurationBuilder(baseUrl, proxy, securedProxy, true)

	/**
	 * Sets the proxy of the future HttpProtocolConfiguration
	 *
	 * @param host the host of the proxy
	 * @param port the port of the proxy
	 */
	def proxy(host: String, port: Int) = new HttpProxyBuilder(this, host, port)
	
	private[http] def addProxies(httpProxy: ProxyServer, httpsProxy: Option[ProxyServer]) = new HttpProtocolConfigurationBuilder(baseUrl, Some(httpProxy), httpsProxy, followRedirect)

	private[http] def build = new HttpProtocolConfiguration(baseUrl, proxy, securedProxy, followRedirect)
}