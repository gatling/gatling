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
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.session.Session.GATLING_PRIVATE_ATTRIBUTE_PREFIX
import com.ning.http.client.Cookie
import scala.collection.immutable.ListMap

case class CookieKey(domain: String, path: String, name: String)

trait CookieHandling {

	val COOKIES_CONTEXT_KEY = GATLING_PRIVATE_ATTRIBUTE_PREFIX + "http.cookies"

	def getStoredCookies(session: Session, url: String): List[Cookie] = {
		session.getAttributeAsOption[Map[CookieKey, Cookie]](COOKIES_CONTEXT_KEY) match {
			case Some(storedCookies) => {
				if (!storedCookies.isEmpty) {
					val uri = URI.create(url)
					val uriHost = uri.getHost
					val uriPath = uri.getPath
					storedCookies
						.filter { case (key, _) => uriHost.endsWith(key.domain) && uriPath.startsWith(key.path) }
						.map { case (_, cookie) => cookie }
						.toList
				} else {
					Nil
				}
			}
			case None => Nil
		}
	}

	def storeCookies(session: Session, url: String, cookies: Seq[Cookie]) = {
		if (!cookies.isEmpty) {
			val storedCookies = session.getAttributeAsOption[Map[CookieKey, Cookie]](COOKIES_CONTEXT_KEY).getOrElse(new ListMap[CookieKey, Cookie])
			val uri = URI.create(url)
			val uriHost = uri.getHost
			val uriPath = uri.getPath
			val newCookies = storedCookies ++ cookies.map { cookie =>
				val cookieDomain = Option(cookie.getDomain).getOrElse(uriHost)
				val cookiePath = Option(cookie.getPath).getOrElse(uriPath)
				CookieKey(cookieDomain, cookiePath, cookie.getName) -> cookie
			}

			session.setAttribute(COOKIES_CONTEXT_KEY, newCookies)
		} else {
			session
		}
	}
}
