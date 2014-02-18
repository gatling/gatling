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

import scala.collection.mutable

import com.ning.http.client.{ ProxyServer, Realm, Request, RequestBuilder }
import com.typesafe.scalalogging.slf4j.StrictLogging

import io.gatling.core.akka.GatlingActorSystem
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.config.Protocol
import io.gatling.core.filter.Filters
import io.gatling.core.result.message.Status
import io.gatling.core.session.{ Expression, ExpressionWrapper, Session }
import io.gatling.core.session.el.EL
import io.gatling.core.util.RoundRobin
import io.gatling.http.HeaderNames.{ ACCEPT, ACCEPT_ENCODING, ACCEPT_LANGUAGE, CONNECTION, USER_AGENT }
import io.gatling.http.ahc.{ AsyncHandlerActor, HttpEngine, ProxyConverter }
import io.gatling.http.check.HttpCheck
import io.gatling.http.request.ExtraInfoExtractor
import io.gatling.http.request.builder.Http
import io.gatling.http.response.{ Response, ResponseTransformer }
import io.gatling.http.util.HttpHelper.buildBasicAuthRealm

/**
 * HttpProtocol class companion
 */
object HttpProtocol {
	val default = HttpProtocol(
		baseURLs = configuration.http.baseURLs,
		warmUpUrl = configuration.http.warmUpUrl,
		enginePart = HttpProtocolEnginePart(
			shareClient = configuration.http.shareClient,
			shareConnections = configuration.http.shareConnections,
			maxConnectionsPerHost = 6,
			virtualHost = None,
			localAddress = None),
		requestPart = HttpProtocolRequestPart(
			baseHeaders = Map.empty,
			realm = configuration.http.basicAuth.map(credentials => buildBasicAuthRealm(credentials.username, credentials.password).expression),
			autoReferer = configuration.http.autoReferer,
			cache = configuration.http.cache),
		responsePart = HttpProtocolResponsePart(
			followRedirect = configuration.http.followRedirect,
			maxRedirects = None,
			discardResponseChunks = configuration.http.discardResponseChunks,
			responseTransformer = None,
			checks = Nil,
			extraInfoExtractor = None,
			fetchHtmlResources = false,
			htmlResourcesFetchingFilters = None),
		wsPart = HttpProtocolWsPart(
			wsBaseURLs = Nil,
			reconnect = false,
			maxReconnects = None),
		proxyPart = HttpProtocolProxyPart(
			proxy = configuration.http.proxy.map(_.proxyServer),
			secureProxy = configuration.http.proxy.flatMap(_.secureProxyServer),
			proxyExceptions = Nil))

	val warmUpUrls = mutable.Set.empty[String]

	GatlingActorSystem.instanceOpt.foreach(_.registerOnTermination(warmUpUrls.clear))

	def nextBaseUrlF(urls: List[String]): () => Option[String] = {
		val roundRobinUrls = RoundRobin(urls.toArray)
		urls match {
			case Nil => () => None
			case url :: Nil => () => Some(url)
			case _ => () => Some(roundRobinUrls.next)
		}
	}
}

/**
 * Class containing the configuration for the HTTP protocol
 *
 * @param baseURL the radix of all the URLs that will be used (eg: http://mywebsite.tld)
 * @param proxy a proxy through which all the requests must pass to succeed
 */
case class HttpProtocol(
	baseURLs: List[String],
	warmUpUrl: Option[String],
	enginePart: HttpProtocolEnginePart,
	requestPart: HttpProtocolRequestPart,
	responsePart: HttpProtocolResponsePart,
	wsPart: HttpProtocolWsPart,
	proxyPart: HttpProtocolProxyPart) extends Protocol with StrictLogging {

	private val baseURLF = HttpProtocol.nextBaseUrlF(baseURLs)
	def baseURL(): Option[String] = baseURLF()

	override def warmUp() {

		logger.info("Start warm up")

		HttpEngine.start()
		AsyncHandlerActor.start()

		warmUpUrl.map { url =>
			if (!HttpProtocol.warmUpUrls.contains(url)) {
				HttpProtocol.warmUpUrls += url
				val requestBuilder = new RequestBuilder().setUrl(url)
					.setHeader(ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
					.setHeader(ACCEPT_LANGUAGE, "en-US,en;q=0.5")
					.setHeader(ACCEPT_ENCODING, "gzip")
					.setHeader(CONNECTION, "keep-alive")
					.setHeader(USER_AGENT, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

				if (url.startsWith("http://"))
					proxyPart.proxy.foreach(requestBuilder.setProxyServer)
				else
					proxyPart.secureProxy.foreach(requestBuilder.setProxyServer)

				try {
					HttpEngine.instance.defaultAHC.executeRequest(requestBuilder.build).get
				} catch {
					case e: Exception => logger.info(s"Couldn't execute warm up request $url", e)
				}
			}
		}

		if (HttpProtocol.warmUpUrls.isEmpty) {
			val expression = "foo".el[String]

			new Http(expression)
				.get(expression)
				.header("bar", expression)
				.queryParam(expression, expression)
				.build(HttpProtocol.default, false)

			new Http(expression)
				.post(expression)
				.header("bar", expression)
				.param(expression, expression)
				.build(HttpProtocol.default, false)
		}

		logger.info("Warm up done")
	}
}

case class HttpProtocolEnginePart(
	shareClient: Boolean,
	shareConnections: Boolean,
	maxConnectionsPerHost: Int,
	virtualHost: Option[Expression[String]],
	localAddress: Option[InetAddress])

case class HttpProtocolRequestPart(
	baseHeaders: Map[String, Expression[String]],
	realm: Option[Expression[Realm]],
	autoReferer: Boolean,
	cache: Boolean)

case class HttpProtocolResponsePart(
	followRedirect: Boolean,
	maxRedirects: Option[Int],
	discardResponseChunks: Boolean,
	responseTransformer: Option[ResponseTransformer],
	checks: List[HttpCheck],
	extraInfoExtractor: Option[ExtraInfoExtractor],
	fetchHtmlResources: Boolean,
	htmlResourcesFetchingFilters: Option[Filters])

case class HttpProtocolWsPart(
	wsBaseURLs: List[String],
	reconnect: Boolean,
	maxReconnects: Option[Int]) {

	private val wsBaseURLF = HttpProtocol.nextBaseUrlF(wsBaseURLs)
	def wsBaseURL(): Option[String] = wsBaseURLF()
}

case class HttpProtocolProxyPart(
	proxy: Option[ProxyServer],
	secureProxy: Option[ProxyServer],
	proxyExceptions: Seq[String])
