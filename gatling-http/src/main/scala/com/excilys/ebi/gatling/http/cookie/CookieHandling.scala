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
package com.excilys.ebi.gatling.http.cookie

import java.net.URI

import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.session.Session.GATLING_PRIVATE_ATTRIBUTE_PREFIX
import com.ning.http.client.Cookie

object CookieHandling {

	val COOKIES_CONTEXT_KEY = GATLING_PRIVATE_ATTRIBUTE_PREFIX + "http.cookies"

	def getStoredCookies(session: Session, url: String): List[Cookie] = getStoredCookies(session, URI.create(url))

	def getStoredCookies(session: Session, uri: URI): List[Cookie] = {
		session.getAttributeAsOption[CookieJar](COOKIES_CONTEXT_KEY) match {
			case Some(cookieJar) => cookieJar.get(uri)
			case _ => Nil
		}
	}

	def storeCookies(session: Session, uri: URI, cookies: List[Cookie]): Session = {
		if (!cookies.isEmpty) {
			session.getAttributeAsOption[CookieJar](COOKIES_CONTEXT_KEY) match {
				case Some(cookieJar) => session.setAttribute(COOKIES_CONTEXT_KEY, cookieJar.add(uri, cookies))
				case _ => session.setAttribute(COOKIES_CONTEXT_KEY, CookieJar(uri, cookies))
			}
		} else
			session
	}
}
