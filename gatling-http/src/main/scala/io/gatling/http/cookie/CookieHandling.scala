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

import com.ning.http.client.Cookie

import io.gatling.core.session.{ Session, SessionPrivateAttributes }
import io.gatling.core.validation.Success

object CookieHandling {

	val cookieJarAttributeName = SessionPrivateAttributes.privateAttributePrefix + "http.cookies"

	def getStoredCookies(session: Session, url: String): List[Cookie] = getStoredCookies(session, URI.create(url))

	def getStoredCookies(session: Session, uri: URI): List[Cookie] =
		session(cookieJarAttributeName).asOption[CookieJar]
			.map(_.get(uri))
			.getOrElse(Nil)

	def storeCookies(session: Session, uri: URI, cookies: List[Cookie]): Session =
		if (!cookies.isEmpty)
			session(cookieJarAttributeName).asOption[CookieJar]
				.map(cookieJar => session.set(cookieJarAttributeName, cookieJar.add(uri, cookies)))
				.getOrElse(session.set(cookieJarAttributeName, CookieJar(uri, cookies)))
		else
			session
}
