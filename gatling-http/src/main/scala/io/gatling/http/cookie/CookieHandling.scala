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
import com.ning.http.client.Cookie
import io.gatling.core.session.{ Session, SessionPrivateAttributes }
import io.gatling.core.validation.{ Success, SuccessWrapper }
import io.gatling.core.session.Expression

object CookieHandling {

	val cookieJarAttributeName = SessionPrivateAttributes.privateAttributePrefix + "http.cookies"

	def cookieJar(session: Session): Option[CookieJar] = session(cookieJarAttributeName).asOption[CookieJar]

	def getStoredCookies(session: Session, url: String): List[Cookie] = getStoredCookies(session, URI.create(url))

	def getStoredCookies(session: Session, uri: URI): List[Cookie] =
		session(cookieJarAttributeName).asOption[CookieJar] match {
			case Some(cookieJar) => cookieJar.get(uri)
			case _ => Nil
		}

	def storeCookies(session: Session, uri: URI, cookies: List[Cookie]): Session =
		if (!cookies.isEmpty)
			session(cookieJarAttributeName).asOption[CookieJar] match {
				case Some(cookieJar) => session.set(cookieJarAttributeName, cookieJar.add(uri, cookies))
				case _ => session.set(cookieJarAttributeName, CookieJar(uri, cookies))
			}
		else
			session

	def storeCookie(session: Session, cookie: Cookie): Session = {

		val domain = cookie.getDomain
		val cookies = List(cookie)

		session(cookieJarAttributeName).asOption[CookieJar] match {
			case Some(cookieJar) => session.set(cookieJarAttributeName, cookieJar.add(domain, cookies))
			case _ => session.set(cookieJarAttributeName, CookieJar(domain, cookies))
		}
	}

	val flushSessionCookies: Expression[Session] = session => {

		(cookieJar(session) match {
			case Some(cookieJar) =>
				val cookieJarWithoutSessionCookies = CookieJar(cookieJar.store.mapValues(_.filter(_.getMaxAge != -1)))
				session.set(cookieJarAttributeName, cookieJarWithoutSessionCookies)
			case _ => session
		}).success
	}

	val flushCookieJar: Expression[Session] = _.remove(cookieJarAttributeName).success
}
