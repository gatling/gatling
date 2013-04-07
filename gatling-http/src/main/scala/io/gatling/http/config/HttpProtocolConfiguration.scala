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

import io.gatling.core.config.ProtocolConfiguration
import io.gatling.core.result.message.RequestStatus
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.util.RoundRobin
import io.gatling.http.response.Response
import com.ning.http.client.{ ProxyServer, Realm, Request }

/**
 * HttpProtocolConfiguration class companion
 */
object HttpProtocolConfiguration {
	val default = HttpProtocolConfiguration(
		baseURLs = None,
		proxy = None,
		securedProxy = None,
		followRedirectEnabled = true,
		automaticRefererEnabled = true,
		cachingEnabled = true,
		responseChunksDiscardingEnabled = true,
		shareConnections = false,
		baseHeaders = Map.empty,
		basicAuth = None,
		extraInfoExtractor = None)
}

/**
 * Class containing the configuration for the HTTP protocol
 *
 * @param baseURL the radix of all the URLs that will be used (eg: http://mywebsite.tld)
 * @param proxy a proxy through which all the requests must pass to succeed
 */
case class HttpProtocolConfiguration(
	baseURLs: Option[Seq[String]],
	proxy: Option[ProxyServer],
	securedProxy: Option[ProxyServer],
	followRedirectEnabled: Boolean,
	automaticRefererEnabled: Boolean,
	cachingEnabled: Boolean,
	responseChunksDiscardingEnabled: Boolean,
	shareConnections: Boolean,
	baseHeaders: Map[String, String],
	basicAuth: Option[Expression[Realm]],
	extraInfoExtractor: Option[(RequestStatus, Session, Request, Response) => List[Any]]) extends ProtocolConfiguration {

	val roundRobinUrls = baseURLs.map(RoundRobin(_))

	def baseURL(): Option[String] = roundRobinUrls.map(_.next)
}