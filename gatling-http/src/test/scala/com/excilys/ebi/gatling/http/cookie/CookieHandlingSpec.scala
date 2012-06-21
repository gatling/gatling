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

	val originalCookie = new HttpCookie(AsyncHttpProviderUtils.parseCookie("ALPHA=VALUE1; Domain=docs.foo.com; Path=/accounts; Expires=Wed, 13-Jan-2021 22:23:01 GMT; Secure; HttpOnly"))
	val expiredCookie = new HttpCookie(AsyncHttpProviderUtils.parseCookie("BETA=EXPIRED; Domain=docs.foo.com; Path=/home; Expires=Wed, 13-Jan-2008 22:23:01 GMT; Secure; HttpOnly"))
	val originalURI = new URI("https://docs.foo.com/accounts")
	val originalCookieStore = new CookieStore(Map(originalURI -> List(originalCookie)))
	val originalSession = new Session("scenarioName", 1, Map(CookieHandling.COOKIES_CONTEXT_KEY -> originalCookieStore))

	"storeCookies" should {

		"return the original cookie when requesting the original session" in {
			val newCookies = CookieHandling.getStoredCookies(originalSession, "https://docs.foo.com/accounts").toList
			newCookies.length must beEqualTo(1)
			newCookies.head.getValue must beEqualTo("VALUE1")
		}

		"return the original cookie when requesting the original session with a sub path" in {
			val newCookies = CookieHandling.getStoredCookies(originalSession, "https://docs.foo.com/accounts/baz").toList
			newCookies.length must beEqualTo(1)
			newCookies.head.getValue must beEqualTo("VALUE1")
		}

		"not return any expired cookies" in {
			val newCookies = CookieHandling.getStoredCookies(originalSession, "https://docs.foo.com/home").toList
			newCookies must beEmpty
		}

		"overwrite cookie when setting a new one with the same path" in {
			val newCookie = AsyncHttpProviderUtils.parseCookie("ALPHA=VALUE2; Domain=docs.foo.com; Path=/accounts; Expires=Wed, 13-Jan-2021 22:23:01 GMT; Secure; HttpOnly")
			val newSession = CookieHandling.storeCookies(originalSession, originalURI, List(newCookie))

			val newCookies = CookieHandling.getStoredCookies(newSession, "https://docs.foo.com/accounts").toList
			newCookies.length must beEqualTo(1)
			newCookies.head.getValue must beEqualTo("VALUE2")
		}

		"return original cookie when setting a new one with a sub path and requesting the original path" in {

			val newCookie = AsyncHttpProviderUtils.parseCookie("ALPHA=VALUE2; Domain=docs.foo.com; Path=/accounts/foo; Expires=Wed, 13-Jan-2021 22:23:01 GMT; Secure; HttpOnly")
			val newURI = new URI("https://docs.foo.com/accounts/foo")
			val newSession = CookieHandling.storeCookies(originalSession, newURI, List(newCookie))

			val newCookies = CookieHandling.getStoredCookies(newSession, "http://docs.foo.com/accounts").toList
			newCookies.length must beEqualTo(1)
			newCookies.head.getValue must beEqualTo("VALUE1")
		}

		"return updated cookie when setting a new one with a sub path and requesting the sub path" in {

			val newCookie = AsyncHttpProviderUtils.parseCookie("ALPHA=VALUE2; Domain=docs.foo.com; Path=/accounts/foo; Expires=Wed, 13-Jan-2021 22:23:01 GMT; Secure; HttpOnly")
			val newURI = new URI("https://docs.foo.com/accounts/foo")
			val newSession = CookieHandling.storeCookies(originalSession, newURI, List(newCookie))

			val newCookies = CookieHandling.getStoredCookies(newSession, "http://docs.foo.com/accounts/foo").toList
			newCookies.length must beEqualTo(2)
			newCookies.map(c => c.getValue()) must haveTheSameElementsAs(List("VALUE1", "VALUE2"))
		}

		"be able to store a bunch of cookies" in {

			val newCookie1 = AsyncHttpProviderUtils.parseCookie("ALPHA=VALUE2; Domain=docs.foo.com; Path=/accounts/foo; Expires=Wed, 13-Jan-2021 22:23:01 GMT; Secure; HttpOnly")
			val newCookie2 = AsyncHttpProviderUtils.parseCookie("BETA=VALUE3; Domain=docs.foo.com; Path=/accounts/foo; Expires=Wed, 13-Jan-2021 22:23:01 GMT; Secure; HttpOnly")
			val newURI = new URI("https://docs.foo.com/accounts/foo")
			val newSession = CookieHandling.storeCookies(originalSession, newURI, List(newCookie1, newCookie2))

			val newCookies = CookieHandling.getStoredCookies(newSession, "http://docs.foo.com/accounts/foo").toList
			newCookies.map(c => c.getValue()) must haveTheSameElementsAs(List("VALUE1", "VALUE2", "VALUE3"))
		}

		"handle the domain in a case-insensitive manner (RFC 2965 sec. 3.3.3)" in {
			val newCookies = CookieHandling.getStoredCookies(originalSession, "https://dOcs.fOO.cOm/accounts").toList
			newCookies.length must beEqualTo(1)
			newCookies.head.getValue must beEqualTo("VALUE1")
		}

		"handle the cookie name in a case-insensitive manner (RFC 2965 sec. 3.3.3)" in {
			val newCookie = AsyncHttpProviderUtils.parseCookie("aLpHa=VALUE2; Domain=docs.foo.com; Path=/accounts; Expires=Wed, 13-Jan-2021 22:23:01 GMT; Secure; HttpOnly")
			val newSession = CookieHandling.storeCookies(originalSession, originalURI, List(newCookie))

			val newCookies = CookieHandling.getStoredCookies(newSession, "https://docs.foo.com/accounts").toList
			newCookies.length must beEqualTo(1)
			newCookies.head.getValue must beEqualTo("VALUE2")
		}

		"handle the cookie path in a case-sensitive manner (RFC 2965 sec. 3.3.3)" in {
			val newCookies = CookieHandling.getStoredCookies(originalSession, "https://docs.foo.com/aCCounts").toList
			newCookies must beEmpty
		}

	}
}