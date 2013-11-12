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
package io.gatling.http.cookie

import java.net.URI

import scala.annotation.tailrec

import com.ning.http.client.Cookie

object CookieJar {

	// rfc6265#section-1 Cookies for a given host are shared  across all the ports on that host
	private def requestDomain(uri: URI) = uri.getHost.toLowerCase

	// rfc6265#section-5.1.3
	// check "The string is a host name (i.e., not an IP address)" ignored
	def domainMatches(string: String, domain: String) = domain == string || string.endsWith("." + domain)

	// rfc6265#section-5.1.4
	def requestPath(requestURI: URI) = Option(requestURI.getPath) match {
		case Some(requestPath) if requestPath.count(_ == '/') > 1 => requestPath.substring(0, requestPath.lastIndexOf('/'))
		case _ => "/"
	}

	// rfc6265#section-5.1.4
	def pathMatches(cookiePath: String, requestPath: String) =
		cookiePath == requestPath ||
			requestPath.startsWith(cookiePath) &&
			(cookiePath.last == '/' || requestPath.charAt(cookiePath.length) == '/')

	// rfc6265#section-5.2.3
	// Let cookie-domain be the attribute-value without the leading %x2E (".") character.
	def cookieDomain(cookieDomain: String, requestDomain: String) = Option(cookieDomain) match {
		case Some(dom) => (if (dom.charAt(0) == '.') dom.substring(1) else dom).toLowerCase
		case None => requestDomain
	}

	// rfc6265#section-5.2.4
	def cookiePath(rawCookiePath: String, defaultPath: String) = Option(rawCookiePath) match {
		case Some(path) if path.startsWith("/") => path
		case _ => defaultPath
	}

	def apply(uri: URI, cookies: List[Cookie]) = (new CookieJar(Map.empty)).add(uri, cookies)
	def apply(domain: String, cookies: List[Cookie]) = (new CookieJar(Map.empty)).add(domain, cookies)
}

case class CookieJar(store: Map[String, List[Cookie]]) {

	private val MAX_AGE_UNSPECIFIED = -1L

	def add(domain: String, cookies: List[Cookie]) = {
		def cookiesEquals(c1: Cookie, c2: Cookie) = c1.getName.equalsIgnoreCase(c2.getName) && c1.getDomain == c2.getDomain && c1.getPath == c2.getPath

		@tailrec
		def addOrReplaceCookies(newCookies: List[Cookie], oldCookies: List[Cookie]): List[Cookie] = newCookies match {
			case Nil => oldCookies
			case newCookie :: moreNewCookies =>
				val updatedCookies = newCookie :: oldCookies.filterNot(cookiesEquals(_, newCookie))
				addOrReplaceCookies(moreNewCookies, updatedCookies)
		}

		def hasExpired(c: Cookie): Boolean = c.getMaxAge != MAX_AGE_UNSPECIFIED && c.getMaxAge <= 0

		val newCookies = addOrReplaceCookies(cookies, store.get(domain).getOrElse(Nil))
		val nonExpiredCookies = newCookies.filterNot(hasExpired)
		new CookieJar(store + (domain -> nonExpiredCookies))
	}

	/**
	 * @param uri       the uri used to deduce defaults for  optional domains and paths
	 * @param cookies    the cookies to store
	 */
	def add(requestURI: URI, rawCookies: List[Cookie]): CookieJar = {

		val requestDomain = CookieJar.requestDomain(requestURI)
		val requestPath = CookieJar.requestPath(requestURI)

		val fixedCookies = rawCookies.map { cookie =>
			val cookieDomain = CookieJar.cookieDomain(cookie.getDomain, requestDomain)
			val cookiePath = CookieJar.cookiePath(cookie.getPath, requestPath)

			if (cookieDomain != cookie.getDomain || cookiePath != cookie.getPath)
				new Cookie(
					cookieDomain,
					cookie.getName,
					cookie.getValue,
					cookie.getRawValue,
					cookiePath,
					cookie.getMaxAge,
					cookie.isSecure,
					cookie.getVersion,
					cookie.isHttpOnly,
					cookie.isDiscard,
					cookie.getComment,
					cookie.getCommentUrl,
					cookie.getPorts)
			else
				cookie
		} filter {
			// rfc6265#section-4.1.2.3 Reject the cookies when the domains don't match
			cookie => CookieJar.domainMatches(requestDomain, cookie.getDomain)
		}

		add(requestDomain, fixedCookies)
	}

	def get(requestURI: URI): List[Cookie] = {

		val requestDomain = CookieJar.requestDomain(requestURI)
		val requestPath = CookieJar.requestPath(requestURI)

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
