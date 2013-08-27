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

import com.ning.http.client.{ ProxyServer, Realm, Request }

import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.config.Protocol
import io.gatling.core.result.message.Status
import io.gatling.core.session.{ ELWrapper, Expression, Session }
import io.gatling.core.util.RoundRobin
import io.gatling.http.check.HttpCheck
import io.gatling.http.response.{ Response, ResponseTransformer }
import io.gatling.http.util.HttpHelper.{ buildProxy, buildRealm }

/**
 * HttpProtocol class companion
 */
object HttpProtocol {
	val default = HttpProtocol(
		baseURLs = configuration.http.baseURLs,
		proxy = configuration.http.proxy.map(proxy => buildProxy(proxy.host, proxy.port, proxy.credentials, false)),
		securedProxy = configuration.http.proxy.flatMap(proxy => proxy.securePort.map(port => buildProxy(proxy.host, port, proxy.credentials, true))),
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
	securedProxy: Option[ProxyServer],
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
	extraInfoExtractor: Option[(Status, Session, Request, Response) => List[Any]]) extends Protocol {

	val roundRobinUrls = RoundRobin(baseURLs.toArray)

	def baseURL(): Option[String] = baseURLs match {
		case Nil => None
		case _ => Some(roundRobinUrls.next)
	}
}