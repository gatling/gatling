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

import java.text.{ ParseException, SimpleDateFormat }

import scala.annotation.tailrec

import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.session.Session.GATLING_PRIVATE_ATTRIBUTE_PREFIX
import com.excilys.ebi.gatling.http.Headers
import com.excilys.ebi.gatling.http.config.HttpProtocolConfiguration
import com.ning.http.client.{ Request, Response }
import com.ning.http.util.AsyncHttpProviderUtils

import grizzled.slf4j.Logging

object CacheHandling extends Logging {

	val COOKIES_CONTEXT_KEY = GATLING_PRIVATE_ATTRIBUTE_PREFIX + "http.cache"

	val QUOTED_REGEX = "\"(.+)\"".r

	private val sdfs = AsyncHttpProviderUtils.get.toList

	// damn AsyncHttpProviderUtils.convertExpireField is not public
	def convertExpireField(timestring: String): Long = {

		@tailrec
		def parse(timestring: String, sdfs: List[SimpleDateFormat]): Long = {

			val valueOrOtherSdfs = sdfs match {
				case Nil => throw new IllegalArgumentException("Cannot parse into a date: " + timestring)
				case sdf :: others =>
					try {
						val expire = sdf.parse(timestring).getTime
						Left(expire)
					} catch {
						case e: ParseException => Right(others)
						case e: NumberFormatException => Right(others)
					}
			}

			valueOrOtherSdfs match {
				case Left(value) => value
				case Right(others) => parse(timestring, others)
			}
		}

		val cleanedString = timestring.trim match {
			case QUOTED_REGEX(content) => content
			case trimmed => trimmed
		}

		parse(cleanedString, sdfs)
	}

	def isCached(httpProtocolConfiguration: HttpProtocolConfiguration, session: Session, request: Request) = httpProtocolConfiguration.cachingEnabled && getCache(session).contains(request.getUrl)

	def cache(httpProtocolConfiguration: HttpProtocolConfiguration, session: Session, request: Request, response: Response): Session = {

		def isResponseCacheable(response: Response) = httpProtocolConfiguration.cachingEnabled &&
			Option(response.getHeader(Headers.Names.CACHE_CONTROL))
			.map(!_.contains(Headers.Values.NO_CACHE)) // simplification: consider value != no-cache as cache forever
			.getOrElse {
				// if no Cache-Control defined, look for Expires header
				Option(response.getHeader(Headers.Names.EXPIRES))
					.map(convertExpireField(_) > System.currentTimeMillis) // simplification: consider future expiring date as cache forever
					.getOrElse(false) // if neither CC nor Expires, don't cache
			}

		if (isResponseCacheable(response)) {

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