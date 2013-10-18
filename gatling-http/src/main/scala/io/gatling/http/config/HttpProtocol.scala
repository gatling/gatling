/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import com.ning.http.client.{ ProxyServer, Realm, Request, RequestBuilder }
import com.typesafe.scalalogging.slf4j.Logging

import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.config.{ Protocol, Proxy }
import io.gatling.core.filter.FilterList
import io.gatling.core.result.message.Status
import io.gatling.core.session.{ Expression, ExpressionWrapper, Session }
import io.gatling.core.session.el.EL
import io.gatling.core.util.RoundRobin
import io.gatling.http.HeaderNames._
import io.gatling.http.ahc.{ HttpClient, ProxyConverter }
import io.gatling.http.check.HttpCheck
import io.gatling.http.request.builder.HttpRequestBaseBuilder
import io.gatling.http.response.{ Response, ResponseTransformer }
import io.gatling.http.util.HttpHelper.{ buildProxy, buildRealm }

/**
 * HttpProtocol class companion
 */
object HttpProtocol {
	val default = HttpProtocol(
		baseURLs = configuration.http.baseURLs,
		proxy = configuration.http.proxy.map(_.proxyServer),
		secureProxy = configuration.http.proxy.flatMap(_.secureProxyServer),
		proxyExceptions = Nil,
		followRedirect = configuration.http.followRedirect,
		autoReferer = configuration.http.autoReferer,
		cache = configuration.http.cache,
		discardResponseChunks = configuration.http.discardResponseChunks,
		shareClient = configuration.http.shareClient,
		shareConnections = configuration.http.shareConnections,
		basicAuth = configuration.http.basicAuth.map(credentials => buildRealm(credentials.username, credentials.password).expression),
		baseHeaders = Map.empty,
		virtualHost = None,
		localAddress = None,
		responseTransformer = None,
		checks = Nil,
		maxRedirects = None,
		warmUpUrl = configuration.http.warmUpUrl,
		fetchHtmlResources = false,
		fetchHtmlResourcesFilters = Nil,
		maxConnectionsPerHost = 6,
		extraInfoExtractor = None)
}

/**
 * Class containing the configuration for the HTTP protocol
 *
 * @param baseURL the radix of all the URLs that will be used (eg: http://mywebsite.tld)
 * @param proxy a proxy through which all the requests must pass to succeed
 */
case class HttpProtocol(
	baseURLs: Seq[String],
	proxy: Option[ProxyServer],
	secureProxy: Option[ProxyServer],
	proxyExceptions: Seq[String],
	followRedirect: Boolean,
	autoReferer: Boolean,
	cache: Boolean,
	discardResponseChunks: Boolean,
	shareClient: Boolean,
	shareConnections: Boolean,
	baseHeaders: Map[String, String],
	basicAuth: Option[Expression[Realm]],
	virtualHost: Option[Expression[String]],
	localAddress: Option[InetAddress],
	responseTransformer: Option[ResponseTransformer],
	checks: List[HttpCheck],
	maxRedirects: Option[Int],
	warmUpUrl: Option[String],
	fetchHtmlResources: Boolean,
	fetchHtmlResourcesFilters: List[FilterList],
	maxConnectionsPerHost: Int,
	extraInfoExtractor: Option[(String, Status, Session, Request, Response) => List[Any]]) extends Protocol with Logging {

	val roundRobinUrls = RoundRobin(baseURLs.toArray)

	def baseURL(): Option[String] = baseURLs match {
		case Nil => None
		case _ => Some(roundRobinUrls.next)
	}

	override def warmUp() {

		logger.info("Start warm up")

		warmUpUrl.map { url =>
			if (!HttpProtocolBuilder.warmUpUrls.contains(url)) {
				HttpProtocolBuilder.warmUpUrls += url
				val requestBuilder = new RequestBuilder().setUrl(url)
					.setHeader(ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
					.setHeader(ACCEPT_LANGUAGE, "en-US,en;q=0.5")
					.setHeader(ACCEPT_ENCODING, "gzip")
					.setHeader(CONNECTION, "keep-alive")
					.setHeader(USER_AGENT, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

				if (url.startsWith("http://"))
					proxy.foreach(requestBuilder.setProxyServer)
				else
					secureProxy.foreach(requestBuilder.setProxyServer)

				try {
					HttpClient.default.executeRequest(requestBuilder.build).get
				} catch {
					case e: Exception => logger.info(s"Couldn't execute warm up request $url", e)
				}
			}
		}

		if (HttpProtocolBuilder.warmUpUrls.isEmpty) {
			val expression = "foo".el[String]

			HttpRequestBaseBuilder.http(expression)
				.get(expression)
				.header("bar", expression)
				.queryParam(expression, expression)
				.build(Session("scenarioName", "0"), HttpProtocol.default)

			HttpRequestBaseBuilder.http(expression)
				.post(expression)
				.header("bar", expression)
				.param(expression, expression)
				.build(Session("scenarioName", "0"), HttpProtocol.default)
		}

		logger.info("Warm up done")
	}
}
