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
package com.excilys.ebi.gatling.http.cache

import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.session.Session.GATLING_PRIVATE_ATTRIBUTE_PREFIX
import com.excilys.ebi.gatling.http.Headers
import com.excilys.ebi.gatling.http.config.HttpProtocolConfiguration
import com.ning.http.client.{ Request, Response }
import com.ning.http.util.AsyncHttpProviderUtils

import grizzled.slf4j.Logging

object CacheHandling extends Logging {

	val COOKIES_CONTEXT_KEY = GATLING_PRIVATE_ATTRIBUTE_PREFIX + "http.cache"

	def isFutureExpire(timeString: String): Boolean =
		try {
			val maxAge = try {
				AsyncHttpProviderUtils.convertExpireField(timeString)
			} catch {
				case _: Exception => timeString.toInt
			}

			maxAge > 0
		} catch {
			case _: Exception => false
		}

	def isCached(httpProtocolConfiguration: HttpProtocolConfiguration, session: Session, request: Request) = httpProtocolConfiguration.cachingEnabled && getCache(session).contains(request.getUrl)

	def cache(httpProtocolConfiguration: HttpProtocolConfiguration, session: Session, request: Request, response: Response): Session = {

		def pragmaNoCache = Option(response.getHeader(Headers.Names.PRAGMA)).map(_.contains(Headers.Values.NO_CACHE)).getOrElse(false)
		def cacheControlNoCache = Option(response.getHeader(Headers.Names.CACHE_CONTROL)).map(_.contains(Headers.Values.NO_CACHE)).getOrElse(false)
		def expiresInFuture = Option(response.getHeader(Headers.Names.EXPIRES))
			.map(isFutureExpire(_))
			.getOrElse(false)

		val isResponseCacheable = httpProtocolConfiguration.cachingEnabled && !pragmaNoCache && !cacheControlNoCache && expiresInFuture

		if (isResponseCacheable) {
			val cache = getCache(session)
			val url = request.getUrl

			if (cache.contains(url)) {
				info(url + " was already cached")
				session

			} else {
				info("Caching url " + url)
				session.setAttribute(COOKIES_CONTEXT_KEY, cache + url)
			}

		} else
			session
	}

	private def getCache(session: Session): Set[String] = session.getAttributeAsOption[Set[String]](COOKIES_CONTEXT_KEY).getOrElse(Set.empty)
}