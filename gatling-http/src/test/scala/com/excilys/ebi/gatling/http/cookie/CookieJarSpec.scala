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

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import com.ning.http.util.AsyncHttpProviderUtils.parseCookie

@RunWith(classOf[JUnitRunner])
class CookieJarSpec extends Specification {

	"storeCookies" should {

		"return nothing when it's empty" in {
			new CookieJar(Map.empty).get(new URI("http://docs.foo.com")) must beEmpty
		}

		"not return cookie when it was set on another domain" in {
			val cookie = parseCookie("ALPHA; Domain=www.foo.com")
			val cookieStore = CookieJar(new URI("http://www.foo.com"), List(cookie))

			cookieStore.get(new URI("http://www.bar.com")) must beEmpty
		}

		"return the cookie when domain and path exactly match" in {
			val cookie = parseCookie("ALPHA; Domain=www.foo.com; path=/bar")
			val cookieStore = CookieJar(new URI("http://www.foo.com/bar"), List(cookie))

			cookieStore.get(new URI("http://www.foo.com/bar")).length must beEqualTo(1)
		}

		"not return the cookie when domain matches but path is different" in {
			val cookie = parseCookie("ALPHA; Domain=www.foo.com; path=/bar")
			val cookieStore = CookieJar(new URI("http://www.foo.com/bar"), List(cookie))

			cookieStore.get(new URI("http://www.foo.com/baz")) must beEmpty
		}

		"not return the cookie when domain matches but path is a parent" in {
			val cookie = parseCookie("ALPHA; Domain=www.foo.com; path=/bar")
			val cookieStore = CookieJar(new URI("http://www.foo.com/bar"), List(cookie))

			cookieStore.get(new URI("http://www.foo.com")) must beEmpty
		}

		"return the cookie when domain matches and path is a child" in {
			val cookie = parseCookie("ALPHA; Domain=www.foo.com; path=/bar")
			val cookieStore = CookieJar(new URI("http://www.foo.com/bar"), List(cookie))

			cookieStore.get(new URI("http://www.foo.com/bar/baz")).length must beEqualTo(1)
		}

		"return cookie when it was set on a sub domain" in {
			val cookie = parseCookie("ALPHA; Domain=.foo.com")
			val cookieStore = CookieJar(new URI("http://www.foo.com"), List(cookie))

			cookieStore.get(new URI("http://bar.foo.com")).length must beEqualTo(1)
		}

		"replace cookie when set on the same domain and path" in {
			val cookie = parseCookie("ALPHA=VALUE1; Domain=www.foo.com; path=/bar")
			val uri = new URI("http://www.foo.com/bar")
			val cookieStore = CookieJar(uri, List(cookie))

			val cookies = cookieStore.add(uri, List(parseCookie("ALPHA=VALUE2; Domain=www.foo.com; path=/bar"))).get(uri)
			cookies.length must beEqualTo(1)
			cookies.head.getValue must beEqualTo("VALUE2")
		}

		"not replace cookies when they don't have the same name" in {
			val cookie = parseCookie("BETA=VALUE1; Domain=www.foo.com; path=/bar")
			val uri = new URI("http://www.foo.com/bar")
			val cookieStore = CookieJar(uri, List(cookie))

			val cookies = cookieStore.add(uri, List(parseCookie("ALPHA=VALUE2; Domain=www.foo.com; path=/bar"))).get(uri)
			cookies.length must beEqualTo(2)
		}

		"expire cookie when set with a date in the past" in {
			val cookie = parseCookie("ALPHA=VALUE1; Domain=www.foo.com; path=/bar")
			val uri = new URI("http://www.foo.com/bar")
			val cookieStore = CookieJar(uri, List(cookie))

			val cookies = cookieStore.add(uri, List(parseCookie("ALPHA=EXPIRED; Domain=www.foo.com; Path=/bar; Expires=Wed, 24-Jan-1982 22:23:01 GMT"))).get(uri)
			cookies must beEmpty
		}

		"have cookie of the same name co-exist if not set on the same domain" in {
			val cookie1 = parseCookie("ALPHA=VALUE1; Domain=www.foo.com")
			val uri1 = new URI("http://www.foo.com")
			val cookie2 = parseCookie("ALPHA=VALUE2; Domain=www.bar.com")
			val uri2 = new URI("http://www.bar.com")
			val cookieStore = CookieJar(uri1, List(cookie1)).add(uri2, List(cookie2))

			val cookies1 = cookieStore.get(uri1)
			cookies1.length must beEqualTo(1)
			cookies1.head.getValue must beEqualTo("VALUE1")
			val cookies2 = cookieStore.get(uri2)
			cookies2.length must beEqualTo(1)
			cookies2.head.getValue must beEqualTo("VALUE2")
		}

		"handle missing domain as request host" in {
			val cookie = parseCookie("ALPHA=VALUE1; Path=/")
			val uri = new URI("http://www.foo.com")
			val cookieStore = CookieJar(uri, List(cookie))

			cookieStore.get(new URI("http://www.foo.com")).length must beEqualTo(1)
		}

		"return the cookie when it's issued from a request with a subpath" in {
			val cookie = parseCookie("ALPHA; path=/")
			val cookieStore = CookieJar(new URI("http://www.foo.com/bar"), List(cookie))

			cookieStore.get(new URI("http://www.foo.com")).length must beEqualTo(1)
		}

		"handle missing path as request path when from root dir" in {
			val cookie = parseCookie("ALPHA=VALUE1")
			val uri = new URI("http://www.foo.com")
			val cookieStore = CookieJar(uri, List(cookie))

			cookieStore.get(new URI("http://www.foo.com")).length must beEqualTo(1)
		}

		"handle missing path as request path when path is not empty" in {
			val cookie = parseCookie("ALPHA=VALUE1")
			val uri = new URI("http://www.foo.com/bar")
			val cookieStore = CookieJar(uri, List(cookie))

			cookieStore.get(new URI("http://www.foo.com/bar")).length must beEqualTo(1)
		}

		"handle the domain in a case-insensitive manner (RFC 2965 sec. 3.3.3)" in {
			val cookie = parseCookie("ALPHA=VALUE1")
			val uri = new URI("http://www.foo.com/bar")
			val cookieStore = CookieJar(uri, List(cookie))

			cookieStore.get(new URI("http://www.FoO.com/bar")).length must beEqualTo(1)
		}

		"handle the cookie name in a case-insensitive manner (RFC 2965 sec. 3.3.3)" in {
			val cookie = parseCookie("ALPHA=VALUE1; Domain=www.foo.com; path=/bar")
			val uri = new URI("http://www.foo.com/bar")
			val cookieStore = CookieJar(uri, List(cookie))

			val cookies = cookieStore.add(uri, List(parseCookie("alpha=VALUE2; Domain=www.foo.com; path=/bar"))).get(uri)
			cookies.length must beEqualTo(1)
			cookies.head.getValue must beEqualTo("VALUE2")
		}

		"handle the cookie path in a case-sensitive manner (RFC 2965 sec. 3.3.3)" in {
			val cookie = parseCookie("ALPHA=VALUE1")
			val uri = new URI("http://www.foo.com/bar")
			val cookieStore = CookieJar(uri, List(cookie))

			cookieStore.get(new URI("http://www.FoO.com/bAr")) must beEmpty
		}

		"not take into account the query parameter in the URI" in {
			val cookie = parseCookie("ALPHA; Domain=www.foo.com; path=/bar")
			val cookieStore = CookieJar(new URI("http://www.foo.com/bar?query1"), List(cookie))

			cookieStore.get(new URI("http://www.foo.com/bar?query2")).length must beEqualTo(1)
		}

		"should serve the cookies on a subdomain when the domains match" in {
			val cookie = parseCookie("cookie1=VALUE1; Path=/; Domain=foo.org;")
			val cookieStore = CookieJar(new URI("https://x.foo.org/"), List(cookie))

			// RFC 6265, 5.1.3.  Domain Matching
			cookieStore.get(new URI("https://y.x.foo.org/")).length must beEqualTo(1)
		}

		"should serve the last cookie when they are definied twice" in {
			val cookie1 = parseCookie("cookie1=VALUE1; Path=/")
			val cookie2 = parseCookie("cookie1=VALUE2; Path=/")
			val cookie3 = parseCookie("cookie1=VALUE3; Path=/")
			val cookieStore = CookieJar(new URI("https://foo.org/"), List(cookie1, cookie2, cookie3))

			val cookies = cookieStore.get(new URI("https://foo.org/"))
			cookies.length must beEqualTo(1)
			cookies.head.getValue must beEqualTo("VALUE3")
		}
	}
}