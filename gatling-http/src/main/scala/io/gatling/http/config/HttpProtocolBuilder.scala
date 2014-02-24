/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import java.net.InetAddress

import com.ning.http.client.{ ProxyServer, Realm, Request }
import com.typesafe.scalalogging.slf4j.StrictLogging

import io.gatling.core.filter.{ BlackList, Filter, Filters, WhiteList }
import io.gatling.core.result.message.Status
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.session.el.EL
import io.gatling.http.HeaderNames._
import io.gatling.http.ahc.ProxyConverter
import io.gatling.http.check.HttpCheck
import io.gatling.http.request.ExtraInfoExtractor
import io.gatling.http.response.{ Response, ResponseTransformer }
import io.gatling.http.util.HttpHelper

/**
 * HttpProtocolBuilder class companion
 */
object HttpProtocolBuilder {

	val default = new HttpProtocolBuilder(HttpProtocol.default)

	implicit def toHttpProtocol(builder: HttpProtocolBuilder): HttpProtocol = builder.build
}

/**
 * Builder for HttpProtocol used in DSL
 *
 * @param protocol the protocol being built
 * @param warmUpUrl a URL to be pinged in order to warm up the HTTP engine
 */
case class HttpProtocolBuilder(protocol: HttpProtocol) extends StrictLogging {

	def baseURL(url: String) = copy(protocol = protocol.copy(baseURLs = List(url)))
	def baseURLs(urls: String*): HttpProtocolBuilder = baseURLs(urls.toList)
	def baseURLs(urls: List[String]): HttpProtocolBuilder = copy(protocol = protocol.copy(baseURLs = urls))
	def warmUp(url: String): HttpProtocolBuilder = copy(protocol = copy(protocol.copy(warmUpUrl = Some(url))))
	def disableWarmUp: HttpProtocolBuilder = copy(protocol = protocol.copy(warmUpUrl = None))

	// enginePart
	private def newEnginePart(enginePart: HttpProtocolEnginePart) = copy(protocol = copy(protocol.copy(enginePart = enginePart)))
	def disableClientSharing = newEnginePart(protocol.enginePart.copy(shareClient = false))
	def shareConnections = newEnginePart(protocol.enginePart.copy(shareConnections = true))
	def virtualHost(virtualHost: Expression[String]) = newEnginePart(protocol.enginePart.copy(virtualHost = Some(virtualHost)))
	def localAddress(localAddress: InetAddress) = newEnginePart(protocol.enginePart.copy(localAddress = Some(localAddress)))
	def maxConnectionsPerHostLikeFirefoxOld = maxConnectionsPerHost(2)
	def maxConnectionsPerHostLikeFirefox = maxConnectionsPerHost(6)
	def maxConnectionsPerHostLikeOperaOld = maxConnectionsPerHost(4)
	def maxConnectionsPerHostLikeOpera = maxConnectionsPerHost(6)
	def maxConnectionsPerHostLikeSafariOld = maxConnectionsPerHost(4)
	def maxConnectionsPerHostLikeSafari = maxConnectionsPerHost(6)
	def maxConnectionsPerHostLikeIE7 = maxConnectionsPerHost(2)
	def maxConnectionsPerHostLikeIE8 = maxConnectionsPerHost(6)
	def maxConnectionsPerHostLikeIE10 = maxConnectionsPerHost(8)
	def maxConnectionsPerHostLikeChrome = maxConnectionsPerHost(6)
	def maxConnectionsPerHost(max: Int): HttpProtocolBuilder = newEnginePart(protocol.enginePart.copy(maxConnectionsPerHost = max))

	// requestPart
	private def newRequestPart(requestPart: HttpProtocolRequestPart) = copy(protocol = copy(protocol.copy(requestPart = requestPart)))
	def disableAutoReferer = newRequestPart(protocol.requestPart.copy(autoReferer = false))
	def disableCaching = newRequestPart(protocol.requestPart.copy(cache = false))
	def baseHeaders(headers: Map[String, String]) = newRequestPart(protocol.requestPart.copy(baseHeaders = protocol.requestPart.baseHeaders ++ headers.mapValues(_.el[String])))
	def acceptHeader(value: Expression[String]) = newRequestPart(protocol.requestPart.copy(baseHeaders = protocol.requestPart.baseHeaders + (ACCEPT -> value)))
	def acceptCharsetHeader(value: Expression[String]) = newRequestPart(protocol.requestPart.copy(baseHeaders = protocol.requestPart.baseHeaders + (ACCEPT_CHARSET -> value)))
	def acceptEncodingHeader(value: Expression[String]) = newRequestPart(protocol.requestPart.copy(baseHeaders = protocol.requestPart.baseHeaders + (ACCEPT_ENCODING -> value)))
	def acceptLanguageHeader(value: Expression[String]) = newRequestPart(protocol.requestPart.copy(baseHeaders = protocol.requestPart.baseHeaders + (ACCEPT_LANGUAGE -> value)))
	def authorizationHeader(value: Expression[String]) = newRequestPart(protocol.requestPart.copy(baseHeaders = protocol.requestPart.baseHeaders + (AUTHORIZATION -> value)))
	def connection(value: Expression[String]) = newRequestPart(protocol.requestPart.copy(baseHeaders = protocol.requestPart.baseHeaders + (CONNECTION -> value)))
	def doNotTrackHeader(value: Expression[String]) = newRequestPart(protocol.requestPart.copy(baseHeaders = protocol.requestPart.baseHeaders + (DO_NOT_TRACK -> value)))
	def userAgentHeader(value: Expression[String]) = newRequestPart(protocol.requestPart.copy(baseHeaders = protocol.requestPart.baseHeaders + (USER_AGENT -> value)))
	def basicAuth(username: Expression[String], password: Expression[String]) = authRealm(HttpHelper.buildBasicAuthRealm(username, password))
	def digestAuth(username: Expression[String], password: Expression[String]) = authRealm(HttpHelper.buildDigestAuthRealm(username, password))
	def authRealm(realm: Expression[Realm]) = newRequestPart(protocol.requestPart.copy(realm = Some(realm)))

	// responsePart
	private def newResponsePart(responsePart: HttpProtocolResponsePart) = copy(protocol = copy(protocol.copy(responsePart = responsePart)))
	def disableFollowRedirect = newResponsePart(protocol.responsePart.copy(followRedirect = false))
	def maxRedirects(max: Int) = newResponsePart(protocol.responsePart.copy(maxRedirects = Some(max)))
	def disableResponseChunksDiscarding = newResponsePart(protocol.responsePart.copy(discardResponseChunks = false))
	def extraInfoExtractor(f: ExtraInfoExtractor) = newResponsePart(protocol.responsePart.copy(extraInfoExtractor = Some(f)))
	def transformResponse(responseTransformer: ResponseTransformer) = newResponsePart(protocol.responsePart.copy(responseTransformer = Some(responseTransformer)))
	def check(checks: HttpCheck*) = newResponsePart(protocol.responsePart.copy(checks = protocol.responsePart.checks ::: checks.toList))
	def fetchHtmlResources(): HttpProtocolBuilder = fetchHtmlResources(None)
	def fetchHtmlResources(white: WhiteList): HttpProtocolBuilder = fetchHtmlResources(Some(Filters(white, BlackList())))
	def fetchHtmlResources(white: WhiteList, black: BlackList): HttpProtocolBuilder = fetchHtmlResources(Some(Filters(white, black)))
	def fetchHtmlResources(black: BlackList, white: WhiteList = WhiteList(Nil)): HttpProtocolBuilder = fetchHtmlResources(Some(Filters(black, white)))
	private def fetchHtmlResources(filters: Option[Filters]) = newResponsePart(protocol.responsePart.copy(fetchHtmlResources = true, htmlResourcesFetchingFilters = filters))

	// wsPart
	private def newWsPart(wsPart: HttpProtocolWsPart) = copy(protocol = copy(protocol.copy(wsPart = wsPart)))
	def wsBaseURL(url: String) = newWsPart(protocol.wsPart.copy(wsBaseURLs = List(url)))
	def wsBaseURLs(urls: String*) = newWsPart(protocol.wsPart.copy(wsBaseURLs = urls.toList))
	def wsBaseURLs(urls: List[String]) = newWsPart(protocol.wsPart.copy(wsBaseURLs = urls))
	def wsReconnect = newWsPart(protocol.wsPart.copy(reconnect = true))
	def wsMaxReconnects(max: Int) = newWsPart(protocol.wsPart.copy(maxReconnects = Some(max)))

	// proxyPart
	private def newProxyPart(proxyPart: HttpProtocolProxyPart) = copy(protocol = copy(protocol.copy(proxyPart = proxyPart)))
	def noProxyFor(hosts: String*): HttpProtocolBuilder = newProxyPart(protocol.proxyPart.copy(proxyExceptions = hosts))
	def proxy(httpProxy: Proxy): HttpProtocolBuilder = newProxyPart(protocol.proxyPart.copy(proxy = Some(httpProxy.proxyServer), secureProxy = httpProxy.secureProxyServer))

	def build = {
		require(protocol.enginePart.shareClient || !protocol.enginePart.shareConnections, "Invalid protocol configuration: if you stop sharing the HTTP client, you can't share connections!")
		protocol
	}
}
