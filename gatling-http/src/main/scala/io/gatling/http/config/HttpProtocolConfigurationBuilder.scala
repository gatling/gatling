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
package io.gatling.http.config

import com.ning.http.client.{ ProxyServer, Request, RequestBuilder }
import com.typesafe.scalalogging.slf4j.Logging

import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.result.message.Status
import io.gatling.core.session.{ Expression, Session }
import io.gatling.http.Headers
import io.gatling.http.ahc.GatlingHttpClient
import io.gatling.http.request.builder.{ GetHttpRequestBuilder, PostHttpRequestBuilder }
import io.gatling.http.response.Response
import io.gatling.http.util.HttpHelper

/**
 * HttpProtocolConfigurationBuilder class companion
 */
object HttpProtocolConfigurationBuilder {

	val default = new HttpProtocolConfigurationBuilder(HttpProtocolConfiguration.default, configuration.http.warmUpUrl)
}

/**
 * Builder for HttpProtocolConfiguration used in DSL
 *
 * @param config the config being built
 * @param warmUpUrl a URL to be pinged in order to warm up the HTTP engine
 */
case class HttpProtocolConfigurationBuilder(config: HttpProtocolConfiguration, warmUpUrl: Option[String]) extends Logging {

	def baseURL(baseUrl: String) = copy(config = config.copy(baseURLs = Some(List(baseUrl))))

	def baseURLs(baseUrl1: String, baseUrl2: String, baseUrls: String*) = copy(config = config.copy(baseURLs = Some(baseUrl1 :: baseUrl2 :: baseUrls.toList)))

	def disableFollowRedirect = copy(config = config.copy(followRedirectEnabled = false))

	def disableAutomaticReferer = copy(config = config.copy(automaticRefererEnabled = false))

	def disableCaching = copy(config = config.copy(cachingEnabled = false))

	def disableResponseChunksDiscarding = copy(config = config.copy(responseChunksDiscardingEnabled = false))

	def shareConnections = copy(config = config.copy(shareConnections = true))

	def acceptHeader(value: String) = copy(config = config.copy(baseHeaders = config.baseHeaders + (Headers.Names.ACCEPT -> value)))

	def acceptCharsetHeader(value: String) = copy(config = config.copy(baseHeaders = config.baseHeaders + (Headers.Names.ACCEPT_CHARSET -> value)))

	def acceptEncodingHeader(value: String) = copy(config = config.copy(baseHeaders = config.baseHeaders + (Headers.Names.ACCEPT_ENCODING -> value)))

	def acceptLanguageHeader(value: String) = copy(config = config.copy(baseHeaders = config.baseHeaders + (Headers.Names.ACCEPT_LANGUAGE -> value)))

	def authorizationHeader(value: String) = copy(config = config.copy(baseHeaders = config.baseHeaders + (Headers.Names.AUTHORIZATION -> value)))

	def connection(value: String) = copy(config = config.copy(baseHeaders = config.baseHeaders + (Headers.Names.CONNECTION -> value)))

	def doNotTrackHeader(value: String) = copy(config = config.copy(baseHeaders = config.baseHeaders + (Headers.Names.DO_NOT_TRACK -> value)))

	def userAgentHeader(value: String) = copy(config = config.copy(baseHeaders = config.baseHeaders + (Headers.Names.USER_AGENT -> value)))

	def warmUp(url: String) = copy(warmUpUrl = Some(url))

	def disableWarmUp = copy(warmUpUrl = None)

	def basicAuth(username: Expression[String], password: Expression[String]) = copy(config = config.copy(basicAuth = Some(HttpHelper.buildRealm(username, password))))

	def extraInfoExtractor(f: (Status, Session, Request, Response) => List[Any]) = copy(config = config.copy(extraInfoExtractor = Some(f)))

	def proxy(host: String, port: Int) = new HttpProxyBuilder(this, host, port)

	def addProxies(httpProxy: ProxyServer, httpsProxy: Option[ProxyServer]) = copy(config = config.copy(proxy = Some(httpProxy), securedProxy = httpsProxy))

	def build = {

		warmUpUrl.map { url =>
			val requestBuilder = new RequestBuilder().setUrl(url)

			config.proxy.map { proxy => if (url.startsWith("http://")) requestBuilder.setProxyServer(proxy) }
			config.securedProxy.map { proxy => if (url.startsWith("https://")) requestBuilder.setProxyServer(proxy) }

			try {
				GatlingHttpClient.client.executeRequest(requestBuilder.build).get
			} catch {
				case e: Exception => logger.info(s"Couldn't execute warm up request $url", e)
			}
		}

		GetHttpRequestBuilder.warmUp
		PostHttpRequestBuilder.warmUp

		config
	}
}