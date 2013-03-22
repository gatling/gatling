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
import com.excilys.ebi.gatling.core.session.{ ELCompiler, Expression, Session }
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

	val default = new HttpProtocolConfigurationBuilder(HttpProtocolConfiguration.default, Some("http://gatling-tool.org"))
}

/**
 * Builder for HttpProtocolConfiguration used in DSL
 *
 * @param baseUrl the radix of all the URLs that will be used (eg: http://mywebsite.tld)
 * @param proxy a proxy through which all the requests must pass to succeed
 */
class HttpProtocolConfigurationBuilder(config: HttpProtocolConfiguration, warmUpUrl: Option[String]) extends Logging {

	private def withNewConfig(config: HttpProtocolConfiguration) = new HttpProtocolConfigurationBuilder(config, warmUpUrl)

	def baseURL(baseUrl: String) = withNewConfig(config.copy(baseURLs = Some(List(baseUrl))))

	def baseURLs(baseUrl1: String, baseUrl2: String, baseUrls: String*) = withNewConfig(config.copy(baseURLs = Some(baseUrl1 :: baseUrl2 :: baseUrls.toList)))

	def disableFollowRedirect = withNewConfig(config.copy(followRedirectEnabled = false))

	def disableAutomaticReferer = withNewConfig(config.copy(automaticRefererEnabled = false))

	def disableCaching = withNewConfig(config.copy(cachingEnabled = false))

	def disableResponseChunksDiscarding = withNewConfig(config.copy(responseChunksDiscardingEnabled = false))

	def shareConnections = withNewConfig(config.copy(shareConnections = true))

	def acceptHeader(value: String) = withNewConfig(config.copy(baseHeaders = config.baseHeaders + (Headers.Names.ACCEPT -> value)))

	def acceptCharsetHeader(value: String) = withNewConfig(config.copy(baseHeaders = config.baseHeaders + (Headers.Names.ACCEPT_CHARSET -> value)))

	def acceptEncodingHeader(value: String) = withNewConfig(config.copy(baseHeaders = config.baseHeaders + (Headers.Names.ACCEPT_ENCODING -> value)))

	def acceptLanguageHeader(value: String) = withNewConfig(config.copy(baseHeaders = config.baseHeaders + (Headers.Names.ACCEPT_LANGUAGE -> value)))

	def authorizationHeader(value: String) = withNewConfig(config.copy(baseHeaders = config.baseHeaders + (Headers.Names.AUTHORIZATION -> value)))

	def connection(value: String) = withNewConfig(config.copy(baseHeaders = config.baseHeaders + (Headers.Names.CONNECTION -> value)))

	def doNotTrackHeader(value: String) = withNewConfig(config.copy(baseHeaders = config.baseHeaders + (Headers.Names.DO_NOT_TRACK -> value)))

	def userAgentHeader(value: String) = withNewConfig(config.copy(baseHeaders = config.baseHeaders + (Headers.Names.USER_AGENT -> value)))

	def warmUp(warmUpUrl: String) = new HttpProtocolConfigurationBuilder(config, Some(warmUpUrl))

	def disableWarmUp = new HttpProtocolConfigurationBuilder(config, None)

	def basicAuth(username: Expression[String], password: Expression[String]) = withNewConfig(config.copy(basicAuth = Some(HttpHelper.buildRealm(username, password))))

	def extraInfoExtractor(f: (RequestStatus, Session, Request, ExtendedResponse) => List[Any]) = withNewConfig(config.copy(extraInfoExtractor = Some(f)))

	/**
	 * Sets the proxy of the future HttpProtocolConfiguration
	 *
	 * @param host the host of the proxy
	 * @param port the port of the proxy
	 */
	def proxy(host: String, port: Int) = new HttpProxyBuilder(this, host, port)

	private[http] def addProxies(httpProxy: ProxyServer, httpsProxy: Option[ProxyServer]) = withNewConfig(config.copy(proxy = Some(httpProxy), securedProxy = httpsProxy))

	private[http] def build = {

		def warmUp() {
			warmUpUrl.map { url =>
				val requestBuilder = new RequestBuilder().setUrl(url)

				config.proxy.map { proxy => if (url.startsWith("http://")) requestBuilder.setProxyServer(proxy) }
				config.securedProxy.map { proxy => if (url.startsWith("https://")) requestBuilder.setProxyServer(proxy) }

				try {
					GatlingHttpClient.client.executeRequest(requestBuilder.build).get
				} catch {
					case e: Exception => info(s"Couldn't execute warm up request $url", e)
				}
			}

			val expression = "foo".el[String]
			GetHttpRequestBuilder(expression, expression)
				.header("bar", expression)
				.queryParam(expression, expression)
				.build(Session("scenarioName", 0), config)

			PostHttpRequestBuilder(expression, expression)
				.header("bar", expression)
				.param(expression, expression)
				.build(Session("scenarioName", 0), config)
		}

		warmUp()
		config
	}
}