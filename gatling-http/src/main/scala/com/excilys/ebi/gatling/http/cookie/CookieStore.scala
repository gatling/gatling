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

class CookieStore(store: Map[URI, Seq[HttpCookie]]) {

	def add(uri: URI, newCookies: Seq[HttpCookie]) = {
		val cookiesWithDiffURI = store.filter(pair => pair._1 != uri)

		@tailrec
		def addCookies(newCookies: Seq[HttpCookie], oldCookies: Seq[HttpCookie]): Seq[HttpCookie] =
			newCookies match {
				case newCookie :: moreNewCookies => addCookies(moreNewCookies, oldCookies.map(c => if (c == newCookie) newCookie else c))
				case _ => oldCookies
			}

		val cookiesWithExactURI = store.get(uri) match {
			case Some(cookies) => addCookies(newCookies, cookies)
			case _ => newCookies
		}

		new CookieStore(cookiesWithDiffURI ++ Map(uri -> cookiesWithExactURI))
	}

	def get(uri: URI) = {

		val cookiesWithExactURI = store.get(uri) match {
			case Some(cookies) => cookies.filter(c => !c.hasExpired)
			case _ => List()
		}

		cookiesWithExactURI.map(c => c.getName())

		def domainAndExpirityFilter(cookies: Seq[HttpCookie]) = {
			cookies.filter(c => !cookiesWithExactURI.contains(c) && java.net.HttpCookie.domainMatches(c.getDomain, uri.getHost) && (uri.getPath startsWith c.getPath) && !c.hasExpired)
		}
		val cookiesWithSubPath = store.filter(pair => pair._1 != uri).flatten(pair => domainAndExpirityFilter(pair._2))

		cookiesWithExactURI ++ cookiesWithSubPath
	}
}