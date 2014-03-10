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
package com.excilys.ebi.gatling.http.cache

import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.session.Session.GATLING_PRIVATE_ATTRIBUTE_PREFIX
import com.excilys.ebi.gatling.core.util.TimeHelper.nowMillis
import com.excilys.ebi.gatling.http.Headers
import com.excilys.ebi.gatling.http.ahc.JodaTimeConverter
import com.excilys.ebi.gatling.http.config.HttpProtocolConfiguration
import com.ning.http.client.{ Request, Response }
import com.ning.http.client.date.RFC2616DateParser

import grizzled.slf4j.Logging

object CacheHandling extends Logging {

	val CACHE_CONTEXT_KEY = GATLING_PRIVATE_ATTRIBUTE_PREFIX + "http.cache"
	val LAST_MODIFIED_CONTEXT_KEY = GATLING_PRIVATE_ATTRIBUTE_PREFIX + "http.lastModified"
	val ETAG_CONTEXT_KEY = GATLING_PRIVATE_ATTRIBUTE_PREFIX + "http.etag"

	private def getCache(session: Session): Set[String] = session.getAttributeAsOption[Set[String]](CACHE_CONTEXT_KEY).getOrElse(Set.empty)
	private def getLastModifiedStore(session: Session): Map[String, String] = session.getAttributeAsOption[Map[String, String]](LAST_MODIFIED_CONTEXT_KEY).getOrElse(Map.empty[String, String])
	private def getEtagStore(session: Session): Map[String, String] = session.getAttributeAsOption[Map[String, String]](ETAG_CONTEXT_KEY).getOrElse(Map.empty[String, String])

	def isFutureExpire(timeString: String): Boolean = {

		def removeQuote(s: String) =
			if (!s.isEmpty) {
				var changed = false
				var start = 0
				var end = s.length

				if (s.charAt(0) == '"')
					start += 1

				if (s.charAt(s.length() - 1) == '"')
					end -= 1

				if (changed)
					s.substring(start, end)
				else
					s
			} else
				s

		// FIXME use offset instead of 2 substrings
		val trimmedTimeString = removeQuote(timeString.trim)

		Option(new RFC2616DateParser(trimmedTimeString).parse).map(JodaTimeConverter.toTime) match {
			case Some(millis) => millis > nowMillis
			case None => false
		}
	}

	def isCached(httpProtocolConfiguration: HttpProtocolConfiguration, session: Session, request: Request) = httpProtocolConfiguration.cachingEnabled && getCache(session).contains(request.getUrl)
	def getLastModified(httpProtocol: HttpProtocolConfiguration, session: Session, url: String) = if (httpProtocol.cachingEnabled) getLastModifiedStore(session).get(url) else None
	def getEtag(httpProtocol: HttpProtocolConfiguration, session: Session, url: String) = if (httpProtocol.cachingEnabled) getEtagStore(session).get(url) else None

	def cache(httpProtocolConfiguration: HttpProtocolConfiguration, session: Session, request: Request, response: Response): Session = {

		def pragmaNoCache = Option(response.getHeader(Headers.Names.PRAGMA)).map(_.contains(Headers.Values.NO_CACHE)).getOrElse(false)
		def cacheControlNoCache = Option(response.getHeader(Headers.Names.CACHE_CONTROL)).map(_.contains(Headers.Values.NO_CACHE)).getOrElse(false)
		def expiresInFuture = Option(response.getHeader(Headers.Names.EXPIRES))
			.map(isFutureExpire)
			.getOrElse(false)

		def isResponseCacheable = httpProtocolConfiguration.cachingEnabled && !pragmaNoCache && !cacheControlNoCache && expiresInFuture

		val url = request.getUrl

		def updateCache(session: Session) = {
			val cache = getCache(session)
			if (cache.contains(url)) {
				logger.info(url + " was already cached")
				session

			} else {
				logger.info("Caching url " + url)
				session.setAttribute(CACHE_CONTEXT_KEY, cache + url)
			}
		}

		def updateLastModified(session: Session) = Option(response.getHeader(Headers.Names.LAST_MODIFIED))
			.map { lastModified =>
				logger.info("Setting LastModified for url " + url)
				val lastModifiedStore = getLastModifiedStore(session)
				session.setAttribute(LAST_MODIFIED_CONTEXT_KEY, lastModifiedStore + (url -> lastModified))
			}.getOrElse(session)

		def updateEtag(session: Session) = Option(response.getHeader(Headers.Names.ETAG))
			.map { etag =>
				logger.info("Setting Etag for url " + url)
				val etagStore = getEtagStore(session)
				session.setAttribute(ETAG_CONTEXT_KEY, etagStore + (url -> etag))
			}.getOrElse(session)

		if (httpProtocolConfiguration.cachingEnabled)
			if (isResponseCacheable)
				updateCache(session)
			else
				updateEtag(updateLastModified(session))
		else
			session
	}
}
