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

	def isFutureExpire(timeString: String): Boolean = {
		val tryConvertExpiresField = Try(AsyncHttpProviderUtils.convertExpireField(timeString))
		val tryConvertToInt = Try(timeString.toInt)
		tryConvertExpiresField.orElse(tryConvertToInt).map(_ > 0).getOrElse(false)
	}

	val httpExpireStoreAttributeName = SessionPrivateAttributes.privateAttributePrefix + "http.cache.expireStore"
	def getExpireStore(session: Session): Map[String, Long] = session(httpExpireStoreAttributeName).asOption.getOrElse(Map.empty[String, Long])
	def getExpire(httpProtocol: HttpProtocol, session: Session, url: String): Option[Long] = if (httpProtocol.cache) getExpireStore(session).get(url) else None
	def clearExpire(session: Session, url: String) = session.set(httpExpireStoreAttributeName, getExpireStore(session) - url)

	val httpLastModifiedStoreAttributeName = SessionPrivateAttributes.privateAttributePrefix + "http.cache.lastModifiedStore"
	def getLastModifiedStore(session: Session): Map[String, String] = session(httpLastModifiedStoreAttributeName).asOption.getOrElse(Map.empty[String, String])
	def getLastModified(httpProtocol: HttpProtocol, session: Session, url: String): Option[String] = if (httpProtocol.cache) getLastModifiedStore(session).get(url) else None

	val httpEtagStoreAttributeName = SessionPrivateAttributes.privateAttributePrefix + "http.cache.etagStore"
	def getEtagStore(session: Session): Map[String, String] = session(httpEtagStoreAttributeName).asOption.getOrElse(Map.empty[String, String])
	def getEtag(httpProtocol: HttpProtocol, session: Session, url: String): Option[String] = if (httpProtocol.cache) getEtagStore(session).get(url) else None

	val maxAgePrefix = "max-age="
	val maxAgeZero = maxAgePrefix + "0"
	def hasPositiveMaxAge(s: String) = {
		val index = s.indexOf(maxAgePrefix)
		val start = maxAgePrefix.length + index
		index >= 0 && start <= s.length && isPositiveDigit(s.charAt(start))
	}

	def getResponseExpire(httpProtocol: HttpProtocol, response: Response): Option[Long] = {
		def pragmaNoCache = Option(response.getHeader(HeaderNames.PRAGMA)).exists(_.contains(HeaderValues.NO_CACHE))
		def cacheControlNoCache = Option(response.getHeader(HeaderNames.CACHE_CONTROL))
			.exists(h => h.contains(HeaderValues.NO_CACHE) || h.contains(HeaderValues.NO_STORE) || h.contains(maxAgeZero))
		def cacheControlInFuture = Option(response.getHeader(HeaderNames.CACHE_CONTROL)).exists(hasPositiveMaxAge)
		def expiresInFuture = Option(response.getHeader(HeaderNames.EXPIRES)).exists(isFutureExpire)

		// TODO are the first 2 tests worth it? 
		if (!pragmaNoCache && !cacheControlNoCache && (cacheControlInFuture || expiresInFuture))
			Some(Long.MaxValue) // FIXME set proper value
		else
			None
	}

	def cache(httpProtocol: HttpProtocol, session: Session, request: Request, response: Response): Session = {

		val url: String = request.getURI.toCacheKey

		val updateExpire = (session: Session) => getResponseExpire(httpProtocol, response)
			.map { expire =>
				logger.info(s"Setting expire for url $url")
				val expireStore = getExpireStore(session)
				session.set(httpExpireStoreAttributeName, expireStore + (url -> expire))
			}.getOrElse(session)

		val updateLastModified = (session: Session) => Option(response.getHeader(HeaderNames.LAST_MODIFIED))
			.map { lastModified =>
				logger.info(s"Setting LastModified $lastModified for url $url")
				val lastModifiedStore = getLastModifiedStore(session)
				session.set(httpLastModifiedStoreAttributeName, lastModifiedStore + (url -> lastModified))
			}.getOrElse(session)

		val updateEtag = (session: Session) => Option(response.getHeader(HeaderNames.ETAG))
			.map { etag =>
				logger.info(s"Setting Etag $etag for url $url")
				val etagStore = getEtagStore(session)
				session.set(httpEtagStoreAttributeName, etagStore + (url -> etag))
			}.getOrElse(session)

		if (httpProtocol.cache)
			(updateExpire andThen updateEtag andThen updateLastModified)(session)
		else
			session
	}
}
