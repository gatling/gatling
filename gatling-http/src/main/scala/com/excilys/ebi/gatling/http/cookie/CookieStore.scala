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
package com.excilys.ebi.gatling.http.cookie

import java.net.URI

import scala.annotation.tailrec

import com.ning.http.client.Cookie

object CookieStore {

	def apply(uri: URI, cookies: List[Cookie]) = new CookieStore(Map.empty).add(uri, cookies)
}

/*
 * http://www.ietf.org/rfc/rfc2109.txt
 * http://www.ietf.org/rfc/rfc2965.txt
 */
private[cookie] class CookieStore(store: Map[URI, List[Cookie]]) {

	private val MAX_AGE_UNSPECIFIED = -1L

	private def getEffectiveUri(uri: URI) =
		new URI(null, // scheme
			uri.getAuthority,
			null, // path component
			null, // query component
			null) // fragment component

	private def extractDomain(rawURI: URI, cookie: Cookie) = Option(cookie.getDomain).getOrElse {
		rawURI.getScheme match {
			case "http" if (rawURI.getPort == 80) => rawURI.getHost
			case "https" if (rawURI.getPort == 443) => rawURI.getHost
			case _ if (rawURI.getPort < 0) => rawURI.getHost
			case _ => rawURI.getHost + ":" + rawURI.getPort
		}
	}

	/**
	 * @param uri       the uri this cookie associated with.
	 *                  if <tt>null</tt>, this cookie will not be associated
	 *                  with an URI
	 * @param cookie    the cookie to store
	 */
	def add(rawURI: URI, rawCookies: List[Cookie]): CookieStore = {
		val newCookies = rawCookies.map { cookie =>
			val fixedDomain = extractDomain(rawURI, cookie)
			val fixedPath = Option(cookie.getPath).getOrElse(rawURI.getPath)

			if (fixedDomain != cookie.getDomain || fixedPath != cookie.getPath) {
				val newCookie = new Cookie(fixedDomain, cookie.getName, cookie.getValue, fixedPath, cookie.getMaxAge, cookie.isSecure, cookie.getVersion)
				newCookie.setPorts(cookie.getPorts)
				newCookie
			} else
				cookie
		} filter {
			// Reject the cookies when the domains don't match, cf: RFC 2965 sec. 3.3.2
			cookie => java.net.HttpCookie.domainMatches(cookie.getDomain, rawURI.getHost)
		}

		def cookiesEquals(c1: Cookie, c2: Cookie) = c1.getName.equalsIgnoreCase(c2.getName) && c1.getDomain.equalsIgnoreCase(c2.getDomain) && c1.getPath == c2.getPath

		def hasExpired(c: Cookie): Boolean = c.getMaxAge != MAX_AGE_UNSPECIFIED && c.getMaxAge <= 0

		val uri = getEffectiveUri(rawURI)

		val cookiesWithExactURI = store.get(uri) match {
			case Some(cookies) =>
				@tailrec
				def addOrReplaceCookies(newCookies: List[Cookie], oldCookies: List[Cookie]): List[Cookie] = newCookies match {
					case Nil => oldCookies
					case newCookie :: moreNewCookies =>
						val updatedCookies = newCookie :: oldCookies.filterNot(cookiesEquals(_, newCookie))
						addOrReplaceCookies(moreNewCookies, updatedCookies)
				}

				addOrReplaceCookies(newCookies, cookies)
			case _ => newCookies
		}
		val nonExpiredCookies = cookiesWithExactURI.filterNot(hasExpired)
		new CookieStore(store + (uri -> nonExpiredCookies))
	}

	def get(rawURI: URI): List[Cookie] = {

		val fixedPath = if (rawURI.getPath.isEmpty) "/" else rawURI.getPath
		val uri = getEffectiveUri(rawURI)

		// RFC 6265, 5.1.3.  Domain Matching
		def domainMatches(host: String, domain: String) =
			rawURI.getHost.equals(domain) || (domain.startsWith(".") && rawURI.getHost.endsWith(domain))

		def pathMatches(cookie: Cookie) = fixedPath.startsWith(cookie.getPath)

		val cookiesWithExactDomain = store.get(uri).getOrElse(Nil).filter(pathMatches)
		val cookiesWithExactDomainNames = cookiesWithExactDomain.map(_.getName.toLowerCase)

		// known limitation: might return duplicates if more than 1 cookie with a given name with non exact domain
		val cookiesWithMatchingDomain = store
			.filterKeys(_ != uri)
			.values
			.flatten
			.filter(cookie => !cookiesWithExactDomainNames.contains(cookie.getName.toLowerCase) && domainMatches(rawURI.getHost, cookie.getDomain) && pathMatches(cookie))

		// known limitation: don't handle runtime expiration, intended for stress test
		cookiesWithExactDomain ++ cookiesWithMatchingDomain
	}

	override def toString = "CookieStore=" + store.toString
}