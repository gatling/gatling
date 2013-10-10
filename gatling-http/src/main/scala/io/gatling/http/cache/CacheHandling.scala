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
import io.gatling.core.util.NumberHelper.isPositiveDigit
import io.gatling.http.{ HeaderNames, HeaderValues }
import io.gatling.http.config.HttpProtocol
import io.gatling.http.response.Response

object CacheHandling extends Logging {

	val httpCacheAttributeName = SessionPrivateAttributes.privateAttributePrefix + "http.cache"
	val httpLastModifiedStoreAttributeName = SessionPrivateAttributes.privateAttributePrefix + "http.lastModifiedStore"
	val httpEtagStoreAttributeName = SessionPrivateAttributes.privateAttributePrefix + "http.etagStore"

	def isFutureExpire(timeString: String): Boolean = {
		val tryConvertExpiresField = Try(AsyncHttpProviderUtils.convertExpireField(timeString))
		val tryConvertToInt = Try(timeString.toInt)
		tryConvertExpiresField.orElse(tryConvertToInt).map(_ > 0).getOrElse(false)
	}

	private def getCache(session: Session): Set[String] = session(httpCacheAttributeName).asOption.getOrElse(Set.empty)
	private def getLastModifiedStore(session: Session): Map[String, String] = session(httpLastModifiedStoreAttributeName).asOption.getOrElse(Map.empty[String, String])
	private def getEtagStore(session: Session): Map[String, String] = session(httpEtagStoreAttributeName).asOption.getOrElse(Map.empty[String, String])

	def isCached(httpProtocol: HttpProtocol, session: Session, request: Request) = httpProtocol.cache && getCache(session).contains(request.getUrl)
	def getLastModified(httpProtocol: HttpProtocol, session: Session, url: String) = if (httpProtocol.cache) getLastModifiedStore(session).get(url) else None
	def getEtag(httpProtocol: HttpProtocol, session: Session, url: String) = if (httpProtocol.cache) getEtagStore(session).get(url) else None

	val maxAgePrefix = "max-age="
	val maxAgeZero = maxAgePrefix + "0"
	def hasPositiveMaxAge(s: String) = {
		val index = s.indexOf(maxAgePrefix)
		val start = maxAgePrefix.length + index
		index >= 0 && start <= s.length && isPositiveDigit(s.charAt(start))
	}

	def isResponseCacheable(httpProtocol: HttpProtocol, response: Response): Boolean = {
		def pragmaNoCache = Option(response.getHeader(HeaderNames.PRAGMA)).exists(_.contains(HeaderValues.NO_CACHE))
		def cacheControlNoCache = Option(response.getHeader(HeaderNames.CACHE_CONTROL))
			.exists(h => h.contains(HeaderValues.NO_CACHE) || h.contains(HeaderValues.NO_STORE) || h.contains(maxAgeZero))
		def cacheControlInFuture = Option(response.getHeader(HeaderNames.CACHE_CONTROL)).exists(hasPositiveMaxAge)
		def expiresInFuture = Option(response.getHeader(HeaderNames.EXPIRES)).exists(isFutureExpire)

		!pragmaNoCache && !cacheControlNoCache && (cacheControlInFuture || expiresInFuture)
	}

	def cache(httpProtocol: HttpProtocol, session: Session, request: Request, response: Response): Session = {

		val url = request.getUrl

		def updateCache(session: Session) = {
			val cache = getCache(session)
			if (cache.contains(url)) {
				logger.info(s"$url was already cached")
				session

			} else {
				logger.info(s"Caching url $url")
				session.set(httpCacheAttributeName, cache + url)
			}
		}

		def updateLastModified(session: Session) = Option(response.getHeader(HeaderNames.LAST_MODIFIED))
			.map { lastModified =>
				logger.info(s"Setting LastModified $lastModified for url $url")
				val lastModifiedStore = getLastModifiedStore(session)
				session.set(httpLastModifiedStoreAttributeName, lastModifiedStore + (url -> lastModified))
			}.getOrElse(session)

		def updateEtag(session: Session) = Option(response.getHeader(HeaderNames.ETAG))
			.map { etag =>
				logger.info(s"Setting Etag $etag for url $url")
				val etagStore = getEtagStore(session)
				session.set(httpEtagStoreAttributeName, etagStore + (url -> etag))
			}.getOrElse(session)

		if (httpProtocol.cache)
			if (isResponseCacheable(httpProtocol, response))
				updateCache(session)
			else
				updateEtag(updateLastModified(session))
		else
			session
	}
}
