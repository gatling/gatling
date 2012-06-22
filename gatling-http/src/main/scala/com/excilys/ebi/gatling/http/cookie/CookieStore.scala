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
import com.ning.http.client.Cookie

class CookieStore(store: Map[URI, Seq[Cookie]]) {

	private val MAX_AGE_UNSPECIFIED = -1l;

	private def equals(c1: Cookie, c2: Cookie) = {
		c1.getName.equalsIgnoreCase(c2.getName) &&
			c1.getDomain != null && c1.getDomain.equalsIgnoreCase(c2.getDomain) &&
			c1.getPath != null && c1.getPath == c2.getPath
	}

	private def contains(cookies: Seq[Cookie], c: Cookie) = cookies.exists(equals(_, c))

	private def hasExpired(c: Cookie): Boolean = {
		return c.getMaxAge != MAX_AGE_UNSPECIFIED && c.getMaxAge <= 0
	}

	private def getEffectiveUri(uri: URI) =
		new URI(uri.getScheme(),
			uri.getAuthority(),
			uri.getPath(),
			null, // query component
			null) // fragment component

	/**
	 * @param uri       the uri this cookie associated with.
	 *                  if <tt>null</tt>, this cookie will not be associated
	 *                  with an URI
	 * @param cookie    the cookie to store
	 */
	def add(rawURI: URI, newCookies: Seq[Cookie]) = {
		val uri = getEffectiveUri(rawURI)
		val cookiesWithDiffURI = store.filter(pair => pair._1 != uri)

		def replaceCookie(newCookie: Cookie, cookies: Seq[Cookie]) = cookies.map(c => if (equals(c, newCookie)) newCookie else c)

		@tailrec
		def addOrReplaceCookies(newCookies: Seq[Cookie], oldCookies: Seq[Cookie]): Seq[Cookie] =
			newCookies match {
				case newCookie :: moreNewCookies => addOrReplaceCookies(moreNewCookies, if (contains(oldCookies, newCookie)) replaceCookie(newCookie, oldCookies) else newCookie :: oldCookies.toList)
				case _ => oldCookies
			}

		val nonExpiredNewCookies = newCookies.filter(c => !hasExpired(c))
		val cookiesWithExactURI = store.get(uri) match {
			case Some(cookies) => addOrReplaceCookies(nonExpiredNewCookies, cookies)
			case _ => nonExpiredNewCookies
		}

		new CookieStore(cookiesWithDiffURI ++ Map(uri -> cookiesWithExactURI))
	}

	/**
	 * @return an immutable list of HttpCookie, an empty list if no cookies match the given URI
	 */
	def get(rawURI: URI) = {
		val uri = getEffectiveUri(rawURI)
		val cookiesWithExactURI = store.get(uri) match {
			case Some(cookies) => cookies
			case _ => Nil
		}

		def domainAndPathFilter(cookies: Seq[Cookie]) = {
			cookies.filter(c => !contains(cookiesWithExactURI, c) && java.net.HttpCookie.domainMatches(c.getDomain, uri.getHost) && (uri.getPath startsWith c.getPath))
		}
		val cookiesWithSubPath = store.foldLeft(List[Cookie]())((cookies, pair) => if (pair._1 != uri) cookies ++ domainAndPathFilter(pair._2) else Nil)

		cookiesWithExactURI ++ cookiesWithSubPath
	}
}