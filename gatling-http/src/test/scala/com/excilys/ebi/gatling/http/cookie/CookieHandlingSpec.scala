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

import org.specs2.mutable.Specification
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import com.excilys.ebi.gatling.core.session.Session
import com.ning.http.client.Cookie
import java.net.URI
import com.ning.http.util.AsyncHttpProviderUtils

@RunWith(classOf[JUnitRunner])
class CookieHandlingSpec extends Specification {

	val originalCookie = AsyncHttpProviderUtils.parseCookie("Set-Cookie: NAME=VALUE1; Domain=docs.foo.com; Path=/accounts; Expires=Wed, 13-Jan-2021 22:23:01 GMT; Secure; HttpOnly")
	val originalURI = new URI("http://docs.foo.com/accounts")
	val originalCookieKey = CookieHandling.newCookieKey(originalCookie, originalURI)
	val originalCookieMap = Map(originalCookieKey -> originalCookie)
	val originalSession = new Session("scenarioName", 1, Map(CookieHandling.COOKIES_CONTEXT_KEY -> originalCookieMap))

	"storeCookies" should {

		"overwrite cookie when setting a new one with the same path" in {

			val newCookie = AsyncHttpProviderUtils.parseCookie("Set-Cookie: NAME=VALUE2; Domain=docs.foo.com; Path=/accounts; Expires=Wed, 13-Jan-2021 22:23:01 GMT; Secure; HttpOnly")
			val newSession = CookieHandling.storeCookies(originalSession, originalURI, List(newCookie))

			val newCookies = CookieHandling.getStoredCookies(newSession, "http://docs.foo.com/accounts").toList
			newCookies.length must beEqualTo(1)
			newCookies.head.getValue must beEqualTo("VALUE2")
		}

		"return original cookie when setting a new one with a sub path and requesting the original path" in {

			val newCookie = AsyncHttpProviderUtils.parseCookie("Set-Cookie: NAME=VALUE2; Domain=docs.foo.com; Path=/accounts/foo; Expires=Wed, 13-Jan-2021 22:23:01 GMT; Secure; HttpOnly")
			val newURI = new URI("http://docs.foo.com/accounts/foo")
			val newSession = CookieHandling.storeCookies(originalSession, newURI, List(newCookie))

			val newCookies = CookieHandling.getStoredCookies(newSession, "http://docs.foo.com/accounts").toList
			newCookies.length must beEqualTo(1)
			newCookies.head.getValue must beEqualTo("VALUE1")
		}

		"return updated cookie when setting a new one with a sub path and requesting the sub path" in {

			val newCookie = AsyncHttpProviderUtils.parseCookie("Set-Cookie: NAME=VALUE2; Domain=docs.foo.com; Path=/accounts/foo; Expires=Wed, 13-Jan-2021 22:23:01 GMT; Secure; HttpOnly")
			val newURI = new URI("http://docs.foo.com/accounts/foo")
			val newSession = CookieHandling.storeCookies(originalSession, newURI, List(newCookie))

			val newCookies = CookieHandling.getStoredCookies(newSession, "http://docs.foo.com/accounts/foo").toList
			newCookies.length must beEqualTo(1)
			newCookies.head.getValue must beEqualTo("VALUE2")
		}
	}
}