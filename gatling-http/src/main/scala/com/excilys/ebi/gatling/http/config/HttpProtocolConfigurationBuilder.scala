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
import com.excilys.ebi.gatling.http.action.HttpRequestAction.HTTP_CLIENT
import com.ning.http.client.{ Response, RequestBuilder, Request, ProxyServer }

import grizzled.slf4j.Logging

/**
 * HttpProtocolConfigurationBuilder class companion
 */
object HttpProtocolConfigurationBuilder {
	
	private[gatling] val BASE_HTTP_PROTOCOL_CONFIGURATION_BUILDER = new HttpProtocolConfigurationBuilder(None, None, None, true, true, true, true, Map.empty, None, None, None)
	
	def httpConfig = BASE_HTTP_PROTOCOL_CONFIGURATION_BUILDER.warmUp("http://gatling-tool.org")

	implicit def toHttpProtocolConfiguration(builder: HttpProtocolConfigurationBuilder) = builder.build
}

/**
 * Builder for HttpProtocolConfiguration used in DSL
 *
 * @param baseUrl the radix of all the URLs that will be used (eg: http://mywebsite.tld)
 * @param proxy a proxy through which all the requests must pass to succeed
 */
class HttpProtocolConfigurationBuilder(baseUrls: Option[Seq[String]],
		proxy: Option[ProxyServer],
		securedProxy: Option[ProxyServer],
		followRedirectEnabled: Boolean,
		automaticRefererEnabled: Boolean,
		cachingEnabled: Boolean,
		responseChunksDiscardingEnabled: Boolean,
		baseHeaders: Map[String, String],
		warmUpUrl: Option[String],
		extraRequestInfoExtractor: Option[(Request => List[String])],
		extraResponseInfoExtractor: Option[(Response => List[String])]) extends Logging {

	/**
	 * Sets the baseURL of the future HttpProtocolConfiguration
	 *
	 * @param baseUrl the base url that will be set
	 */
	def baseURL(baseUrl: String) = new HttpProtocolConfigurationBuilder(Some(List(baseUrl)), proxy, securedProxy, followRedirectEnabled, automaticRefererEnabled, cachingEnabled, responseChunksDiscardingEnabled, baseHeaders, warmUpUrl, extraRequestInfoExtractor, extraResponseInfoExtractor)

	def baseURLs(baseUrls: Seq[String]) = new HttpProtocolConfigurationBuilder(Some(baseUrls), proxy, securedProxy, followRedirectEnabled, automaticRefererEnabled, cachingEnabled, responseChunksDiscardingEnabled, baseHeaders, warmUpUrl, extraRequestInfoExtractor, extraResponseInfoExtractor)

	def disableFollowRedirect = new HttpProtocolConfigurationBuilder(baseUrls, proxy, securedProxy, false, automaticRefererEnabled, cachingEnabled, responseChunksDiscardingEnabled, baseHeaders, warmUpUrl, extraRequestInfoExtractor, extraResponseInfoExtractor)

	def disableAutomaticReferer = new HttpProtocolConfigurationBuilder(baseUrls, proxy, securedProxy, followRedirectEnabled, false, cachingEnabled, responseChunksDiscardingEnabled, baseHeaders, warmUpUrl, extraRequestInfoExtractor, extraResponseInfoExtractor)

	def disableCaching = new HttpProtocolConfigurationBuilder(baseUrls, proxy, securedProxy, followRedirectEnabled, automaticRefererEnabled, false, responseChunksDiscardingEnabled, baseHeaders, warmUpUrl, extraRequestInfoExtractor, extraResponseInfoExtractor)

	def disableResponseChunksDiscarding = new HttpProtocolConfigurationBuilder(baseUrls, proxy, securedProxy, followRedirectEnabled, automaticRefererEnabled, cachingEnabled, false, baseHeaders, warmUpUrl, extraRequestInfoExtractor, extraResponseInfoExtractor)

	def acceptHeader(value: String) = new HttpProtocolConfigurationBuilder(baseUrls, proxy, securedProxy, followRedirectEnabled, automaticRefererEnabled, cachingEnabled, responseChunksDiscardingEnabled, baseHeaders + (Headers.Names.ACCEPT -> value), warmUpUrl, extraRequestInfoExtractor, extraResponseInfoExtractor)

	def acceptCharsetHeader(value: String) = new HttpProtocolConfigurationBuilder(baseUrls, proxy, securedProxy, followRedirectEnabled, automaticRefererEnabled, cachingEnabled, responseChunksDiscardingEnabled, baseHeaders + (Headers.Names.ACCEPT_CHARSET -> value), warmUpUrl, extraRequestInfoExtractor, extraResponseInfoExtractor)

	def acceptEncodingHeader(value: String) = new HttpProtocolConfigurationBuilder(baseUrls, proxy, securedProxy, followRedirectEnabled, automaticRefererEnabled, cachingEnabled, responseChunksDiscardingEnabled, baseHeaders + (Headers.Names.ACCEPT_ENCODING -> value), warmUpUrl, extraRequestInfoExtractor, extraResponseInfoExtractor)

	def acceptLanguageHeader(value: String) = new HttpProtocolConfigurationBuilder(baseUrls, proxy, securedProxy, followRedirectEnabled, automaticRefererEnabled, cachingEnabled, responseChunksDiscardingEnabled, baseHeaders + (Headers.Names.ACCEPT_LANGUAGE -> value), warmUpUrl, extraRequestInfoExtractor, extraResponseInfoExtractor)

	def hostHeader(value: String) = new HttpProtocolConfigurationBuilder(baseUrls, proxy, securedProxy, followRedirectEnabled, automaticRefererEnabled, cachingEnabled, responseChunksDiscardingEnabled, baseHeaders + (Headers.Names.HOST -> value), warmUpUrl, extraRequestInfoExtractor, extraResponseInfoExtractor)

	def userAgentHeader(value: String) = new HttpProtocolConfigurationBuilder(baseUrls, proxy, securedProxy, followRedirectEnabled, automaticRefererEnabled, cachingEnabled, responseChunksDiscardingEnabled, baseHeaders + (Headers.Names.USER_AGENT -> value), warmUpUrl, extraRequestInfoExtractor, extraResponseInfoExtractor)

	def warmUp(warmUpUrl: String) = new HttpProtocolConfigurationBuilder(baseUrls, proxy, securedProxy, followRedirectEnabled, automaticRefererEnabled, cachingEnabled, responseChunksDiscardingEnabled, baseHeaders, Some(warmUpUrl), extraRequestInfoExtractor, extraResponseInfoExtractor)

	def disableWarmUp = new HttpProtocolConfigurationBuilder(baseUrls, proxy, securedProxy, followRedirectEnabled, automaticRefererEnabled, cachingEnabled, responseChunksDiscardingEnabled, baseHeaders, None, extraRequestInfoExtractor, extraResponseInfoExtractor)

	def requestInfoExtractor(value: (Request => List[String])) = new HttpProtocolConfigurationBuilder(baseUrls, proxy, securedProxy, followRedirectEnabled, automaticRefererEnabled, cachingEnabled, responseChunksDiscardingEnabled, baseHeaders, warmUpUrl, Some(value), extraResponseInfoExtractor)

	def responseInfoExtractor(value: (Response) => List[String]) = new HttpProtocolConfigurationBuilder(baseUrls, proxy, securedProxy, followRedirectEnabled, automaticRefererEnabled, cachingEnabled, responseChunksDiscardingEnabled, baseHeaders, warmUpUrl, extraRequestInfoExtractor, Some(value))

	/**
	 * Sets the proxy of the future HttpProtocolConfiguration
	 *
	 * @param host the host of the proxy
	 * @param port the port of the proxy
	 */
	def proxy(host: String, port: Int) = new HttpProxyBuilder(this, host, port)

	private[http] def addProxies(httpProxy: ProxyServer, httpsProxy: Option[ProxyServer]) = new HttpProtocolConfigurationBuilder(baseUrls, Some(httpProxy), httpsProxy, followRedirectEnabled, automaticRefererEnabled, cachingEnabled, responseChunksDiscardingEnabled, baseHeaders, warmUpUrl, extraRequestInfoExtractor, extraResponseInfoExtractor)

	private[http] def build = {
		warmUpUrl.map { url =>
			val requestBuilder = new RequestBuilder().setUrl(url)

			proxy.map { proxy => if (url.startsWith("http://")) requestBuilder.setProxyServer(proxy) }
			securedProxy.map { proxy => if (url.startsWith("https://")) requestBuilder.setProxyServer(proxy) }

			try {
				HTTP_CLIENT.executeRequest(requestBuilder.build).get
			} catch {
				case e => info("Couldn't execute warm up request " + url, e)
			}
		}

		HttpProtocolConfiguration(baseUrls, proxy, securedProxy, followRedirectEnabled, automaticRefererEnabled, cachingEnabled, responseChunksDiscardingEnabled, baseHeaders, extraRequestInfoExtractor, extraResponseInfoExtractor)
	}
}