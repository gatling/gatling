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

import scala.collection.JavaConversions.asScalaSet

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import com.ning.org.jboss.netty.handler.codec.http.CookieDecoder.decode

class CookieJarSpec extends Specification {

	"storeCookies" should {

		"return nothing when it's empty" in {
			new CookieJar(Map.empty).get(new URI("http://docs.foo.com")) must beEmpty
		}

		"not return cookie when it was set on another domain" in {
			val cookies = decode("ALPHA; Domain=www.foo.com").toList
			val cookieStore = CookieJar(new URI("http://www.foo.com"), cookies)

			cookieStore.get(new URI("http://www.bar.com")) must beEmpty
		}

		"return the cookie when it was set on a parent path" in {
			val cookies = decode("ALPHA; Domain=www.foo.com; path=/bar").toList
			val cookieStore = CookieJar(new URI("http://www.foo.com"), cookies)

			cookieStore.get(new URI("http://www.foo.com/bar/baz")).length must beEqualTo(1)
		}

		"not return the cookie when domain matches but path is different" in {
			val cookies = decode("ALPHA; Domain=www.foo.com; path=/bar").toList
			val cookieStore = CookieJar(new URI("http://www.foo.com/bar"), cookies)

			cookieStore.get(new URI("http://www.foo.com/baz")) must beEmpty
		}

		"not return the cookie when domain matches but path is a parent" in {
			val cookies = decode("ALPHA; Domain=www.foo.com; path=/bar").toList
			val cookieStore = CookieJar(new URI("http://www.foo.com/bar"), cookies)

			cookieStore.get(new URI("http://www.foo.com")) must beEmpty
		}

		"return the cookie when domain matches and path is a child" in {
			val cookies = decode("ALPHA; Domain=www.foo.com; path=/bar").toList
			val cookieStore = CookieJar(new URI("http://www.foo.com/bar"), cookies)

			cookieStore.get(new URI("http://www.foo.com/bar/baz")).length must beEqualTo(1)
		}

		"return cookie when it was set on a sub domain" in {
			val cookies = decode("ALPHA; Domain=.foo.com").toList
			val cookieStore = CookieJar(new URI("http://www.foo.com"), cookies)

			cookieStore.get(new URI("http://bar.foo.com")).length must beEqualTo(1)
		}

		"replace cookie when set on the same domain and path" in {
			val cookies = decode("ALPHA=VALUE1; Domain=www.foo.com; path=/bar").toList
			val uri = new URI("http://www.foo.com/bar/baz")
			val cookieStore = CookieJar(uri, cookies)

			val storedCookies = cookieStore.add(uri, decode("ALPHA=VALUE2; Domain=www.foo.com; path=/bar").toList).get(uri)
			storedCookies.length must beEqualTo(1)
			storedCookies.head.getValue must beEqualTo("VALUE2")
		}

		"not replace cookies when they don't have the same name" in {
			val cookies = decode("BETA=VALUE1; Domain=www.foo.com; path=/bar").toList
			val uri = new URI("http://www.foo.com/bar/baz")
			val cookieStore = CookieJar(uri, cookies)

			val storedCookies = cookieStore.add(uri, decode("ALPHA=VALUE2; Domain=www.foo.com; path=/bar").toList).get(uri)
			storedCookies.length must beEqualTo(2)
		}

		"expire cookie when set with a date in the past" in {
			val cookies = decode("ALPHA=VALUE1; Domain=www.foo.com; path=/bar").toList
			val uri = new URI("http://www.foo.com/bar")
			val cookieStore = CookieJar(uri, cookies)

			val storedCookies = cookieStore.add(uri, decode("ALPHA=EXPIRED; Domain=www.foo.com; Path=/bar; Expires=Wed, 24-Jan-1982 22:23:01 GMT").toList).get(uri)
			storedCookies must beEmpty
		}

		"have cookie of the same name co-exist if not set on the same domain" in {
			val cookies1 = decode("ALPHA=VALUE1; Domain=www.foo.com").toList
			val uri1 = new URI("http://www.foo.com")
			val cookies2 = decode("ALPHA=VALUE2; Domain=www.bar.com").toList
			val uri2 = new URI("http://www.bar.com")
			val cookieStore = CookieJar(uri1, cookies1).add(uri2, cookies2)

			val storedCookies1 = cookieStore.get(uri1)
			storedCookies1.length must beEqualTo(1)
			storedCookies1.head.getValue must beEqualTo("VALUE1")
			val storedCookies2 = cookieStore.get(uri2)
			storedCookies2.length must beEqualTo(1)
			storedCookies2.head.getValue must beEqualTo("VALUE2")
		}

		"handle missing domain as request host" in {
			val cookies = decode("ALPHA=VALUE1; Path=/").toList
			val uri = new URI("http://www.foo.com")
			val cookieStore = CookieJar(uri, cookies)

			cookieStore.get(new URI("http://www.foo.com")).length must beEqualTo(1)
		}

		"handle missing path as /" in {
			val cookies = decode("tooe_token=0b1d81dd02d207491a6e9b0a2af9470da9eb1dad").toList
			val uri = new URI("http://www.foo.com")
			val cookieStore = CookieJar(uri, cookies)

			cookieStore.get(new URI("http://www.foo.com")).length must beEqualTo(1)
		}

		"return the cookie when it's issued from a request with a subpath" in {
			val cookies = decode("ALPHA; path=/").toList
			val cookieStore = CookieJar(new URI("http://www.foo.com/bar"), cookies)

			cookieStore.get(new URI("http://www.foo.com")).length must beEqualTo(1)
		}

		"handle missing path as request path when from root dir" in {
			val cookies = decode("ALPHA=VALUE1").toList
			val uri = new URI("http://www.foo.com")
			val cookieStore = CookieJar(uri, cookies)

			cookieStore.get(new URI("http://www.foo.com")).length must beEqualTo(1)
		}

		"handle missing path as request path when path is not empty" in {
			val cookies = decode("ALPHA=VALUE1").toList
			val uri = new URI("http://www.foo.com/bar")
			val cookieStore = CookieJar(uri, cookies)

			cookieStore.get(new URI("http://www.foo.com/bar")).length must beEqualTo(1)
		}

		"handle the domain in a case-insensitive manner (RFC 2965 sec. 3.3.3)" in {
			val cookies = decode("ALPHA=VALUE1").toList
			val uri = new URI("http://www.foo.com/bar")
			val cookieStore = CookieJar(uri, cookies)

			cookieStore.get(new URI("http://www.FoO.com/bar")).length must beEqualTo(1)
		}

		"handle the cookie name in a case-insensitive manner (RFC 2965 sec. 3.3.3)" in {
			val cookies = decode("ALPHA=VALUE1; Domain=www.foo.com; path=/bar").toList
			val uri = new URI("http://www.foo.com/bar/baz")
			val cookieStore = CookieJar(uri, cookies)

			val storedCookies = cookieStore.add(uri, decode("alpha=VALUE2; Domain=www.foo.com; path=/bar").toList).get(uri)
			storedCookies.length must beEqualTo(1)
			storedCookies.head.getValue must beEqualTo("VALUE2")
		}

		"handle the cookie path in a case-sensitive manner (RFC 2965 sec. 3.3.3)" in {
			val cookies = decode("ALPHA=VALUE1").toList
			val uri = new URI("http://www.foo.com/foo/bar")
			val cookieStore = CookieJar(uri, cookies)

			cookieStore.get(new URI("http://www.FoO.com/Foo/bAr")) must beEmpty
		}

		"not take into account the query parameter in the URI" in {
			val cookies = decode("ALPHA; Domain=www.foo.com; path=/").toList
			val cookieStore = CookieJar(new URI("http://www.foo.com/bar?query1"), cookies)

			cookieStore.get(new URI("http://www.foo.com/bar?query2")).length must beEqualTo(1)
		}

		"should serve the cookies on a subdomain when the domains match" in {
			val cookies = decode("cookie1=VALUE1; Path=/; Domain=foo.org;").toList
			val cookieStore = CookieJar(new URI("https://x.foo.org/"), cookies)

			// RFC 6265, 5.1.3.  Domain Matching
			cookieStore.get(new URI("https://y.x.foo.org/")).length must beEqualTo(1)
		}

		"should serve the last cookie when they are definied twice" in {
			val cookies1 = decode("cookie1=VALUE1; Path=/").toList
			val cookies2 = decode("cookie1=VALUE2; Path=/").toList
			val cookies3 = decode("cookie1=VALUE3; Path=/").toList
			val cookieStore = CookieJar(new URI("https://foo.org/"), cookies1 ::: cookies2 ::: cookies3)

			val cookies = cookieStore.get(new URI("https://foo.org/"))
			cookies.length must beEqualTo(1)
			cookies.head.getValue must beEqualTo("VALUE3")
		}

		"should serve cookies based on the host and independently of the port" in {
			// rfc6265#section-1 Cookies for a given host are shared  across all the ports on that host
			val cookies1 = decode("cookie1=VALUE1; Path=/").toList
			val cookieStore = CookieJar(new URI("http://foo.org/moodle/"), cookies1)

			val cookies2 = decode("cookie1=VALUE2; Path=/").toList
			val cookieStore2 = cookieStore.add(new URI("https://foo.org:443/moodle/login"), cookies2)
			
			val cookies = cookieStore2.get(new URI("http://foo.org/moodle/login"))
			cookies.length must beEqualTo(1)
			cookies.head.getValue must beEqualTo("VALUE2")
		}
	}
}
