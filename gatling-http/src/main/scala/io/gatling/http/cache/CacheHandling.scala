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
package io.gatling.http.cache

import scala.util.Try

import com.ning.http.client.Request
import com.ning.http.util.AsyncHttpProviderUtils
import com.typesafe.scalalogging.slf4j.Logging

import io.gatling.core.session.{ Session, SessionPrivateAttributes }
import io.gatling.http.Headers
import io.gatling.http.config.HttpProtocol
import io.gatling.http.response.Response

object CacheHandling extends Logging {

	val httpCacheAttributeName = SessionPrivateAttributes.privateAttributePrefix + "http.cache"

	def isFutureExpire(timeString: String): Boolean = {
		val tryConvertExpiresField = Try(AsyncHttpProviderUtils.convertExpireField(timeString))
		val tryConvertToInt = Try(timeString.toInt)
		tryConvertExpiresField.orElse(tryConvertToInt).map(_ > 0).getOrElse(false)
	}

	private def getCache(session: Session): Set[String] = session(httpCacheAttributeName).asOption.getOrElse(Set.empty)

	def isCached(httpProtocol: HttpProtocol, session: Session, request: Request) = httpProtocol.cache && getCache(session).contains(request.getUrl)

	val maxAgeRegex = """max-age=(\d+)""".r

	def isResponseCacheable(httpProtocol: HttpProtocol, response: Response): Boolean = {
		def pragmaNoCache = Option(response.getHeader(Headers.Names.PRAGMA)).exists(_.contains(Headers.Values.NO_CACHE))
		def cacheControlNoCache = Option(response.getHeader(Headers.Names.CACHE_CONTROL))
			.exists(h => h.contains(Headers.Values.NO_CACHE) || h.contains(Headers.Values.NO_STORE) || h.contains("max-age=0"))
		def cacheControlInFuture = Option(response.getHeader(Headers.Names.CACHE_CONTROL))
			.flatMap(h => for (maxAgeRegex(maxAge) <- maxAgeRegex.findFirstIn(h)) yield maxAge.toInt).exists(_ > 0)
		def expiresInFuture = Option(response.getHeader(Headers.Names.EXPIRES)).exists(isFutureExpire)

		httpProtocol.cache && !pragmaNoCache && !cacheControlNoCache && (cacheControlInFuture || expiresInFuture)
	}

	def cache(httpProtocol: HttpProtocol, session: Session, request: Request, response: Response): Session = {
		if (isResponseCacheable(httpProtocol, response)) {
			val cache = getCache(session)
			val url = request.getUrl

			if (cache.contains(url)) {
				logger.info(s"$url was already cached")
				session

			} else {
				logger.info(s"Caching url $url")
				session.set(httpCacheAttributeName, cache + url)
			}

		} else
			session
	}
}