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
package com.excilys.ebi.gatling.http.cookie

import java.net.URI

import scala.annotation.tailrec

import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY
import com.ning.http.client.Cookie

object CookieStore {

	def apply(uri: URI, cookies: List[Cookie]) = new CookieStore(Map.empty).add(uri, cookies)
}

private[cookie] class CookieStore(store: Map[URI, List[Cookie]]) {

	private val MAX_AGE_UNSPECIFIED = -1L

	private def getEffectiveUri(uri: URI) =
		new URI(uri.getScheme,
			uri.getAuthority,
			null, // path component
			null, // query component
			null) // fragment component

	/**
	 * @param uri       the uri this cookie associated with.
	 *                  if <tt>null</tt>, this cookie will not be associated
	 *                  with an URI
	 * @param cookie    the cookie to store
	 */
	def add(rawURI: URI, rawCookies: List[Cookie]): CookieStore = {
		val newCookies = rawCookies.map { cookie =>

			val fixedDomain = Option(cookie.getDomain).getOrElse(rawURI.getHost)
			val fixedPath = Option(cookie.getPath).getOrElse(rawURI.getPath)

			if (fixedDomain != cookie.getDomain || fixedPath != cookie.getPath) {
				val newCookie = new Cookie(fixedDomain, cookie.getName, cookie.getValue, fixedPath, cookie.getMaxAge, cookie.isSecure, cookie.getVersion)
				newCookie.setPorts(cookie.getPorts)
				newCookie
			} else
				cookie
		}

		def cookiesEquals(c1: Cookie, c2: Cookie) = c1.getName.equalsIgnoreCase(c2.getName) && c1.getDomain.equalsIgnoreCase(c2.getDomain) && c1.getPath == c2.getPath

		def hasExpired(c: Cookie): Boolean = c.getMaxAge != MAX_AGE_UNSPECIFIED && c.getMaxAge <= 0

		@tailrec
		def addOrReplaceCookies(newCookies: List[Cookie], oldCookies: List[Cookie]): List[Cookie] = newCookies match {
			case Nil => oldCookies
			case newCookie :: moreNewCookies =>
				val updatedCookies = newCookie :: oldCookies.filterNot(cookiesEquals(_, newCookie))
				addOrReplaceCookies(moreNewCookies, updatedCookies)
		}

		val uri = getEffectiveUri(rawURI)

		val cookiesWithExactURI = store.get(uri) match {
			case Some(cookies) => addOrReplaceCookies(newCookies, cookies)
			case _ => newCookies
		}
		val nonExpiredCookies = cookiesWithExactURI.filterNot(hasExpired(_))
		new CookieStore(store + (uri -> nonExpiredCookies))
	}

	def get(rawURI: URI): List[Cookie] = {

		val fixedPath = if (rawURI.getPath == EMPTY) "/" else rawURI.getPath
		val uri = getEffectiveUri(rawURI)
		
		def domainMatches(cookie: Cookie) = java.net.HttpCookie.domainMatches(cookie.getDomain, rawURI.getHost)
		def pathMatches(cookie: Cookie) = fixedPath.startsWith(cookie.getPath)

		val cookiesWithExactDomain = store.get(uri).getOrElse(Nil).filter(pathMatches)
		val cookiesWithExactDomainNames = cookiesWithExactDomain.map(_.getName.toLowerCase)

		// known limitation: might return duplicates if more than 1 cookie with a given name with non exact domain
		val cookiesWithMatchingDomain = store
			.filterKeys(_ != uri)
			.values
			.flatten
			.filter(cookie => !cookiesWithExactDomainNames.contains(cookie.getName.toLowerCase) && domainMatches(cookie) && pathMatches(cookie))

		// known limitation: don't handle runtime expiration, intended for stress test
		cookiesWithExactDomain ++ cookiesWithMatchingDomain
	}

	override def toString = "CookieStore=" + store.toString
}