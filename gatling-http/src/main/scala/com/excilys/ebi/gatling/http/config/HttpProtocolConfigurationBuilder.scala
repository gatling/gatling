/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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

import com.excilys.ebi.gatling.core.result.message.RequestStatus
import com.excilys.ebi.gatling.core.session.{ EL, Expression, Session }
import com.excilys.ebi.gatling.http.Headers
import com.excilys.ebi.gatling.http.ahc.GatlingHttpClient
import com.excilys.ebi.gatling.http.request.builder.{ GetHttpRequestBuilder, PostHttpRequestBuilder }
import com.excilys.ebi.gatling.http.response.ExtendedResponse
import com.excilys.ebi.gatling.http.util.HttpHelper
import com.ning.http.client.{ ProxyServer, Realm, Request, RequestBuilder }

import grizzled.slf4j.Logging

/**
 * HttpProtocolConfigurationBuilder class companion
 */
object HttpProtocolConfigurationBuilder {

	private[gatling] val BASE_HTTP_PROTOCOL_CONFIGURATION_BUILDER = new HttpProtocolConfigurationBuilder(Attributes(
		baseUrls = None,
		proxy = None,
		securedProxy = None,
		followRedirectEnabled = true,
		automaticRefererEnabled = true,
		cachingEnabled = true,
		responseChunksDiscardingEnabled = true,
		shareConnections = false,
		baseHeaders = Map.empty,
		warmUpUrl = None,
		basicAuth = None,
		extraInfoExtractor = None))

	def httpConfig = BASE_HTTP_PROTOCOL_CONFIGURATION_BUILDER.warmUp("http://gatling-tool.org")
}

private case class Attributes(baseUrls: Option[List[String]],
	proxy: Option[ProxyServer],
	securedProxy: Option[ProxyServer],
	followRedirectEnabled: Boolean,
	automaticRefererEnabled: Boolean,
	cachingEnabled: Boolean,
	responseChunksDiscardingEnabled: Boolean,
	shareConnections: Boolean,
	baseHeaders: Map[String, String],
	warmUpUrl: Option[String],
	basicAuth: Option[Expression[Realm]],
	extraInfoExtractor: Option[(RequestStatus, Session, Request, ExtendedResponse) => List[Any]])

/**
 * Builder for HttpProtocolConfiguration used in DSL
 *
 * @param baseUrl the radix of all the URLs that will be used (eg: http://mywebsite.tld)
 * @param proxy a proxy through which all the requests must pass to succeed
 */
class HttpProtocolConfigurationBuilder(attributes: Attributes) extends Logging {

	/**
	 * Sets the baseURL of the future HttpProtocolConfiguration
	 *
	 * @param baseUrl the base url that will be set
	 */
	def baseURL(baseUrl: String) = new HttpProtocolConfigurationBuilder(attributes.copy(baseUrls = Some(List(baseUrl))))

	def baseURLs(baseUrl1: String, baseUrl2: String, baseUrls: String*) = new HttpProtocolConfigurationBuilder(attributes.copy(baseUrls = Some(baseUrl1 :: baseUrl2 :: baseUrls.toList)))

	def disableFollowRedirect = new HttpProtocolConfigurationBuilder(attributes.copy(followRedirectEnabled = false))

	def disableAutomaticReferer = new HttpProtocolConfigurationBuilder(attributes.copy(automaticRefererEnabled = false))

	def disableCaching = new HttpProtocolConfigurationBuilder(attributes.copy(cachingEnabled = false))

	def disableResponseChunksDiscarding = new HttpProtocolConfigurationBuilder(attributes.copy(responseChunksDiscardingEnabled = false))

	def shareConnections = new HttpProtocolConfigurationBuilder(attributes.copy(shareConnections = true))

	def acceptHeader(value: String) = new HttpProtocolConfigurationBuilder(attributes.copy(baseHeaders = attributes.baseHeaders + (Headers.Names.ACCEPT -> value)))

	def acceptCharsetHeader(value: String) = new HttpProtocolConfigurationBuilder(attributes.copy(baseHeaders = attributes.baseHeaders + (Headers.Names.ACCEPT_CHARSET -> value)))

	def acceptEncodingHeader(value: String) = new HttpProtocolConfigurationBuilder(attributes.copy(baseHeaders = attributes.baseHeaders + (Headers.Names.ACCEPT_ENCODING -> value)))

	def acceptLanguageHeader(value: String) = new HttpProtocolConfigurationBuilder(attributes.copy(baseHeaders = attributes.baseHeaders + (Headers.Names.ACCEPT_LANGUAGE -> value)))

	def authorizationHeader(value: String) = new HttpProtocolConfigurationBuilder(attributes.copy(baseHeaders = attributes.baseHeaders + (Headers.Names.AUTHORIZATION -> value)))

	def connection(value: String) = new HttpProtocolConfigurationBuilder(attributes.copy(baseHeaders = attributes.baseHeaders + (Headers.Names.CONNECTION -> value)))

	def doNotTrackHeader(value: String) = new HttpProtocolConfigurationBuilder(attributes.copy(baseHeaders = attributes.baseHeaders + (Headers.Names.DO_NOT_TRACK -> value)))

	def userAgentHeader(value: String) = new HttpProtocolConfigurationBuilder(attributes.copy(baseHeaders = attributes.baseHeaders + (Headers.Names.USER_AGENT -> value)))

	def warmUp(warmUpUrl: String) = new HttpProtocolConfigurationBuilder(attributes.copy(warmUpUrl = Some(warmUpUrl)))

	def disableWarmUp = new HttpProtocolConfigurationBuilder(attributes.copy(warmUpUrl = None))

	def basicAuth(username: Expression[String], password: Expression[String]) = new HttpProtocolConfigurationBuilder(attributes.copy(basicAuth = Some(HttpHelper.buildRealm(username, password))))

	def extraInfoExtractor(f: (RequestStatus, Session, Request, ExtendedResponse) => List[Any]) = new HttpProtocolConfigurationBuilder(attributes.copy(extraInfoExtractor = Some(f)))

	/**
	 * Sets the proxy of the future HttpProtocolConfiguration
	 *
	 * @param host the host of the proxy
	 * @param port the port of the proxy
	 */
	def proxy(host: String, port: Int) = new HttpProxyBuilder(this, host, port)

	private[http] def addProxies(httpProxy: ProxyServer, httpsProxy: Option[ProxyServer]) = new HttpProtocolConfigurationBuilder(attributes.copy(proxy = Some(httpProxy), securedProxy = httpsProxy))

	private[http] def build = {

		val config = HttpProtocolConfiguration(
			attributes.baseUrls,
			attributes.proxy,
			attributes.securedProxy,
			attributes.followRedirectEnabled,
			attributes.automaticRefererEnabled,
			attributes.cachingEnabled,
			attributes.responseChunksDiscardingEnabled,
			attributes.shareConnections,
			attributes.baseHeaders,
			attributes.basicAuth,
			attributes.extraInfoExtractor)

		def doWarmUp() {
			attributes.warmUpUrl.map { url =>
				val requestBuilder = new RequestBuilder().setUrl(url)

				attributes.proxy.map { proxy => if (url.startsWith("http://")) requestBuilder.setProxyServer(proxy) }
				attributes.securedProxy.map { proxy => if (url.startsWith("https://")) requestBuilder.setProxyServer(proxy) }

				try {
					GatlingHttpClient.client.executeRequest(requestBuilder.build).get
				} catch {
					case e: Exception => info(s"Couldn't execute warm up request $url", e)
				}
			}

			val expression = EL.compile[String]("foo")
			GetHttpRequestBuilder(expression, expression)
				.header("bar", expression)
				.queryParam(expression, expression)
				.build(Session("scenarioName", 0), config)

			PostHttpRequestBuilder(expression, expression)
				.header("bar", expression)
				.param(expression, expression)
				.build(Session("scenarioName", 0), config)
		}

		doWarmUp()
		config
	}
}