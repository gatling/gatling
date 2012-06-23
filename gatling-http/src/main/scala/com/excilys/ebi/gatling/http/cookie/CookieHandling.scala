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
import com.excilys.ebi.gatling.core.session.Session.GATLING_PRIVATE_ATTRIBUTE_PREFIX
import com.excilys.ebi.gatling.core.session.Session
import com.ning.http.client.Cookie

object CookieHandling {

	val COOKIES_CONTEXT_KEY = GATLING_PRIVATE_ATTRIBUTE_PREFIX + "http.cookies"

	def getStoredCookies(session: Session, url: String): Iterable[Cookie] = {

		session.getAttributeAsOption[CookieStore](COOKIES_CONTEXT_KEY) match {
			case Some(cookieStore) => {
				val uri = URI.create(url)
				cookieStore.get(uri)
			}
			case _ => Nil
		}
	}

	def storeCookies(session: Session, uri: URI, cookies: Iterable[Cookie]): Session = {
		if (!cookies.isEmpty) {
			session.getAttributeAsOption[CookieStore](COOKIES_CONTEXT_KEY) match {
				case Some(cookieStore) => session.setAttribute(COOKIES_CONTEXT_KEY, cookieStore.add(uri, cookies))
				case _ => session.setAttribute(COOKIES_CONTEXT_KEY, new CookieStore(Map(uri -> cookies.toList)))
			}
		} else
			session
	}
}
