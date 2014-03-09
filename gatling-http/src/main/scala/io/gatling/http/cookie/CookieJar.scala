/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.http.cookie

import java.net.URI

import scala.annotation.tailrec

import com.ning.http.client.cookie.Cookie

import io.gatling.core.util.TimeHelper.nowMillis

object CookieJar {

	// rfc6265#section-1 Cookies for a given host are shared  across all the ports on that host
	private def requestDomain(uri: URI) = uri.getHost.toLowerCase

	// rfc6265#section-5.1.3
	// check "The string is a host name (i.e., not an IP address)" ignored
	def domainMatches(string: String, domain: String) = domain == string || string.endsWith("." + domain)

	// rfc6265#section-5.1.4
	def cookieDefaultPath(requestURI: URI) = Option(requestURI.getPath) match {
		case Some(requestPath) if requestPath.count(_ == '/') > 1 => requestPath.substring(0, requestPath.lastIndexOf('/'))
		case _ => "/"
	}

	// rfc6265#section-5.1.4
	def pathMatches(cookiePath: String, requestPath: String) =
		cookiePath == requestPath ||
			(requestPath.startsWith(cookiePath) && (cookiePath.last == '/' || requestPath.charAt(cookiePath.length) == '/'))

	// rfc6265#section-5.2.3
	// Let cookie-domain be the attribute-value without the leading %x2E (".") character.
	def cookieDomain(cookieDomain: String, requestDomain: String) = Option(cookieDomain) match {
		case Some(dom) => (if (dom.charAt(0) == '.') dom.substring(1) else dom).toLowerCase
		case None => requestDomain
	}

	// rfc6265#section-5.2.4
	def cookiePath(rawCookiePath: String, requestURI: URI) = Option(rawCookiePath) match {
		case Some(path) if path.charAt(0) == '/' => path
		case _ => CookieJar.cookieDefaultPath(requestURI)
	}

	def apply(uri: URI, cookies: List[Cookie]): CookieJar = CookieJar(Map.empty).add(uri, cookies)
	def apply(domain: String, cookies: List[Cookie]): CookieJar = CookieJar(Map.empty).add(domain, cookies)
}

case class CookieJar(store: Map[String, List[Cookie]]) {

	private val MAX_AGE_UNSPECIFIED = -1
	private val EXPIRES_UNSPECIFIED = -1L

	def add(domain: String, cookies: List[Cookie]): CookieJar = {
		def cookiesEquals(c1: Cookie, c2: Cookie) = c1.getName.equalsIgnoreCase(c2.getName) && c1.getDomain == c2.getDomain && c1.getPath == c2.getPath

		@tailrec
		def addOrReplaceCookies(newCookies: List[Cookie], oldCookies: List[Cookie]): List[Cookie] = newCookies match {
			case Nil => oldCookies
			case newCookie :: moreNewCookies =>
				val updatedCookies = newCookie :: oldCookies.filterNot(cookiesEquals(_, newCookie))
				addOrReplaceCookies(moreNewCookies, updatedCookies)
		}

		def hasExpired(c: Cookie): Boolean = {
			val maxAge = c.getMaxAge
			val expires = c.getExpires
			(maxAge != MAX_AGE_UNSPECIFIED && maxAge <= 0) || (expires != EXPIRES_UNSPECIFIED && expires <= nowMillis)
		}

		val newCookies = addOrReplaceCookies(cookies, store.get(domain).getOrElse(Nil))
		val nonExpiredCookies = newCookies.filterNot(hasExpired)
		new CookieJar(store + (domain -> nonExpiredCookies))
	}

	/**
	 * @param requestURI       the uri used to deduce defaults for  optional domains and paths
	 * @param rawCookies    the cookies to store
	 */
	def add(requestURI: URI, rawCookies: List[Cookie]): CookieJar = {

		val requestDomain = CookieJar.requestDomain(requestURI)

		val fixedCookies = rawCookies.map { cookie =>
			val cookieDomain = CookieJar.cookieDomain(cookie.getDomain, requestDomain)
			val cookiePath = CookieJar.cookiePath(cookie.getPath, requestURI)

			if (cookieDomain != cookie.getDomain || cookiePath != cookie.getPath)
				new Cookie(
					cookie.getName,
					cookie.getValue,
					cookie.getRawValue,
					cookieDomain,
					cookiePath,
					cookie.getExpires,
					cookie.getMaxAge,
					cookie.isSecure,
					cookie.isHttpOnly)
			else
				cookie
		} filter {
			// rfc6265#section-4.1.2.3 Reject the cookies when the domains don't match
			cookie => CookieJar.domainMatches(requestDomain, cookie.getDomain)
		}

		add(requestDomain, fixedCookies)
	}

	def get(requestURI: URI): List[Cookie] = {

		val requestPath = requestURI.getPath match {
			case "" => "/"
			case p => p
		}
		val requestDomain = CookieJar.requestDomain(requestURI)

		val cookiesWithExactDomain = store.get(requestDomain).getOrElse(Nil).filter(cookie => CookieJar.pathMatches(cookie.getPath, requestPath))
		val cookiesWithExactDomainNames = cookiesWithExactDomain.map(_.getName.toLowerCase)

		// known limitation: might return duplicates if more than 1 cookie with a given name with non exact domain
		def matchSubDomain(cookie: Cookie) = !cookiesWithExactDomainNames.contains(cookie.getName.toLowerCase) &&
			CookieJar.domainMatches(requestDomain, cookie.getDomain) &&
			CookieJar.pathMatches(cookie.getPath, requestPath)

		val cookiesWithMatchingSubDomain = store
			.collect { case (key, cookies) if key != requestDomain => cookies.filter(matchSubDomain) }
			.flatten

		// known limitation: don't handle runtime expiration, intended for stress test
		cookiesWithExactDomain ++ cookiesWithMatchingSubDomain
	}
}
