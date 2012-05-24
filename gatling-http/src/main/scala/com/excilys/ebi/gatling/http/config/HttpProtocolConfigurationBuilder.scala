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

import com.excilys.ebi.gatling.http.Headers
import com.ning.http.client.ProxyServer

/**
 * HttpProtocolConfigurationBuilder class companion
 */
object HttpProtocolConfigurationBuilder {
	def httpConfig = new HttpProtocolConfigurationBuilder(None, None, None, true, true, Map.empty)

	implicit def toHttpProtocolConfiguration(builder: HttpProtocolConfigurationBuilder) = builder.build
}

/**
 * Builder for HttpProtocolConfiguration used in DSL
 *
 * @param baseUrl the radix of all the URLs that will be used (eg: http://mywebsite.tld)
 * @param proxy a proxy through which all the requests must pass to succeed
 */
class HttpProtocolConfigurationBuilder(baseUrl: Option[String], proxy: Option[ProxyServer], securedProxy: Option[ProxyServer], followRedirectParam: Boolean, automaticRefererParam: Boolean, baseHeaders: Map[String, String]) {

	/**
	 * Sets the baseURL of the future HttpProtocolConfiguration
	 *
	 * @param baseurl the base url that will be set
	 */
	def baseURL(baseUrl: String) = new HttpProtocolConfigurationBuilder(Some(baseUrl), proxy, securedProxy, followRedirectParam, automaticRefererParam, baseHeaders)

	def disableFollowRedirect = new HttpProtocolConfigurationBuilder(baseUrl, proxy, securedProxy, false, automaticRefererParam, baseHeaders)

	def disableAutomaticReferer = new HttpProtocolConfigurationBuilder(baseUrl, proxy, securedProxy, followRedirectParam, false, baseHeaders)

	def acceptHeader(value: String) = new HttpProtocolConfigurationBuilder(baseUrl, proxy, securedProxy, followRedirectParam, automaticRefererParam, baseHeaders + (Headers.Names.ACCEPT -> value))

	def acceptCharsetHeader(value: String) = new HttpProtocolConfigurationBuilder(baseUrl, proxy, securedProxy, followRedirectParam, automaticRefererParam, baseHeaders + (Headers.Names.ACCEPT_CHARSET -> value))

	def acceptEncodingHeader(value: String) = new HttpProtocolConfigurationBuilder(baseUrl, proxy, securedProxy, followRedirectParam, automaticRefererParam, baseHeaders + (Headers.Names.ACCEPT_ENCODING -> value))

	def acceptLanguageHeader(value: String) = new HttpProtocolConfigurationBuilder(baseUrl, proxy, securedProxy, followRedirectParam, automaticRefererParam, baseHeaders + (Headers.Names.ACCEPT_LANGUAGE -> value))

	def hostHeader(value: String) = new HttpProtocolConfigurationBuilder(baseUrl, proxy, securedProxy, followRedirectParam, automaticRefererParam, baseHeaders + (Headers.Names.HOST -> value))

	def userAgentHeader(value: String) = new HttpProtocolConfigurationBuilder(baseUrl, proxy, securedProxy, followRedirectParam, automaticRefererParam, baseHeaders + (Headers.Names.USER_AGENT -> value))

	/**
	 * Sets the proxy of the future HttpProtocolConfiguration
	 *
	 * @param host the host of the proxy
	 * @param port the port of the proxy
	 */
	def proxy(host: String, port: Int) = new HttpProxyBuilder(this, host, port)

	private[http] def addProxies(httpProxy: ProxyServer, httpsProxy: Option[ProxyServer]) = new HttpProtocolConfigurationBuilder(baseUrl, Some(httpProxy), httpsProxy, followRedirectParam, automaticRefererParam, baseHeaders)

	private[http] def build = HttpProtocolConfiguration(baseUrl, proxy, securedProxy, followRedirectParam, automaticRefererParam, baseHeaders)
}