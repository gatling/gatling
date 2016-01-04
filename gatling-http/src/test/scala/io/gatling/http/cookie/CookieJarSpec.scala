/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.cookie

import io.gatling.BaseSpec

import org.asynchttpclient.cookie.CookieDecoder.decode
import org.asynchttpclient.uri.Uri

class CookieJarSpec extends BaseSpec {

  "storeCookies" should "return nothing when it's empty" in {
    new CookieJar(Map.empty).get(Uri.create("http://docs.foo.com")) shouldBe empty
  }

  it should "support adding cookie with empty path" in {
    val cookie = decode("ALPHA=VALUE1; path=")
    val uri = Uri.create("http://www.foo.com")
    new CookieJar(Map.empty).add(uri, List(cookie)).get(uri) should not be empty
  }

  it should "not return cookie when it was set on another domain" in {
    val cookie = decode("ALPHA=VALUE1; Domain=www.foo.com")
    val cookieStore = CookieJar(Uri.create("http://www.foo.com"), List(cookie))

    cookieStore.get(Uri.create("http://www.bar.com")) shouldBe empty
  }

  it should "return the cookie when it was set on the same path" in {
    val cookie = decode("ALPHA=VALUE1; path=/bar/")
    val cookieStore = CookieJar(Uri.create("http://www.foo.com"), List(cookie))

    cookieStore.get(Uri.create("http://www.foo.com/bar/")) should have size 1
  }

  it should "return the cookie when it was set on a parent path" in {
    val cookie = decode("ALPHA=VALUE1; Domain=www.foo.com; path=/bar")
    val cookieStore = CookieJar(Uri.create("http://www.foo.com"), List(cookie))

    cookieStore.get(Uri.create("http://www.foo.com/bar/baz")) should have size 1
  }

  it should "not return the cookie when domain matches but path is different" in {
    val cookie = decode("ALPHA=VALUE1; Domain=www.foo.com; path=/bar")
    val cookieStore = CookieJar(Uri.create("http://www.foo.com/bar"), List(cookie))

    cookieStore.get(Uri.create("http://www.foo.com/baz")) shouldBe empty
  }

  it should "not return the cookie when domain matches but path is a parent" in {
    val cookie = decode("ALPHA=VALUE1; Domain=www.foo.com; path=/bar")
    val cookieStore = CookieJar(Uri.create("http://www.foo.com/bar"), List(cookie))

    cookieStore.get(Uri.create("http://www.foo.com")) shouldBe empty
  }

  it should "return the cookie when domain matches and path is a child" in {
    val cookie = decode("ALPHA=VALUE1; Domain=www.foo.com; path=/bar")
    val cookieStore = CookieJar(Uri.create("http://www.foo.com/bar"), List(cookie))

    cookieStore.get(Uri.create("http://www.foo.com/bar/baz")) should have size 1
  }

  it should "return cookie when it was set on a sub domain" in {
    val cookie = decode("ALPHA=VALUE1; Domain=.foo.com")
    val cookieStore = CookieJar(Uri.create("http://www.foo.com"), List(cookie))

    cookieStore.get(Uri.create("http://bar.foo.com")) should have size 1
  }

  it should "replace cookie when set on the same domain and path" in {
    val cookie = decode("ALPHA=VALUE1; Domain=www.foo.com; path=/bar")
    val uri = Uri.create("http://www.foo.com/bar/baz")
    val cookieStore = CookieJar(uri, List(cookie))

    val storedCookies = cookieStore.add(uri, List(decode("ALPHA=VALUE2; Domain=www.foo.com; path=/bar"))).get(uri)
    storedCookies should have size 1
    storedCookies.head.getValue shouldBe "VALUE2"
  }

  it should "not replace cookies when they don't have the same name" in {
    val cookie = decode("BETA=VALUE1; Domain=www.foo.com; path=/bar")
    val uri = Uri.create("http://www.foo.com/bar/baz")
    val cookieStore = CookieJar(uri, List(cookie))

    val storedCookies = cookieStore.add(uri, List(decode("ALPHA=VALUE2; Domain=www.foo.com; path=/bar"))).get(uri)
    storedCookies should have size 2
  }

  it should "expire cookie when set with a date in the past" in {
    val cookie = decode("ALPHA=VALUE1; Domain=www.foo.com; path=/bar")
    val uri = Uri.create("http://www.foo.com/bar")
    val cookieStore = CookieJar(uri, List(cookie))

    val storedCookies = cookieStore.add(uri, List(decode("ALPHA=EXPIRED; Domain=www.foo.com; Path=/bar; Expires=Sun, 06 Nov 1994 08:49:37 GMT"))).get(uri)
    storedCookies shouldBe empty
  }

  it should "have cookie of the same name co-exist if not set on the same domain" in {
    val cookie1 = decode("ALPHA=VALUE1; Domain=www.foo.com")
    val uri1 = Uri.create("http://www.foo.com")
    val cookie2 = decode("ALPHA=VALUE2; Domain=www.bar.com")
    val uri2 = Uri.create("http://www.bar.com")
    val cookieStore = CookieJar(uri1, List(cookie1)).add(uri2, List(cookie2))

    val storedCookies1 = cookieStore.get(uri1)
    storedCookies1 should have size 1
    storedCookies1.head.getValue shouldBe "VALUE1"
    val storedCookies2 = cookieStore.get(uri2)
    storedCookies2 should have size 1
    storedCookies2.head.getValue shouldBe "VALUE2"
  }

  it should "handle missing domain as request host" in {
    val cookie = decode("ALPHA=VALUE1; Path=/")
    val uri = Uri.create("http://www.foo.com")
    val cookieStore = CookieJar(uri, List(cookie))

    cookieStore.get(Uri.create("http://www.foo.com")) should have size 1
  }

  it should "handle missing path as /" in {
    val cookie = decode("tooe_token=0b1d81dd02d207491a6e9b0a2af9470da9eb1dad")
    val uri = Uri.create("http://www.foo.com")
    val cookieStore = CookieJar(uri, List(cookie))

    cookieStore.get(Uri.create("http://www.foo.com")) should have size 1
  }

  it should "return the cookie when it's issued from a request with a subpath" in {
    val cookie = decode("ALPHA=VALUE; path=/")
    val cookieStore = CookieJar(Uri.create("http://www.foo.com/bar"), List(cookie))

    cookieStore.get(Uri.create("http://www.foo.com")) should have size 1
  }

  it should "handle missing path as request path when from root dir" in {
    val cookie = decode("ALPHA=VALUE1")
    val uri = Uri.create("http://www.foo.com")
    val cookieStore = CookieJar(uri, List(cookie))

    cookieStore.get(Uri.create("http://www.foo.com")) should have size 1
  }

  it should "handle missing path as request path when path is not empty" in {
    val cookie = decode("ALPHA=VALUE1")
    val uri = Uri.create("http://www.foo.com/bar")
    val cookieStore = CookieJar(uri, List(cookie))

    cookieStore.get(Uri.create("http://www.foo.com/bar")) should have size 1
  }

  it should "handle the domain in a case-insensitive manner (RFC 2965 sec. 3.3.3)" in {
    val cookie = decode("ALPHA=VALUE1")
    val uri = Uri.create("http://www.foo.com/bar")
    val cookieStore = CookieJar(uri, List(cookie))

    cookieStore.get(Uri.create("http://www.FoO.com/bar")) should have size 1
  }

  it should "handle the cookie name in a case-insensitive manner (RFC 2965 sec. 3.3.3)" in {
    val cookie = decode("ALPHA=VALUE1; Domain=www.foo.com; path=/bar")
    val uri = Uri.create("http://www.foo.com/bar/baz")
    val cookieStore = CookieJar(uri, List(cookie))

    val storedCookies = cookieStore.add(uri, List(decode("alpha=VALUE2; Domain=www.foo.com; path=/bar"))).get(uri)
    storedCookies should have size 1
    storedCookies.head.getValue shouldBe "VALUE2"
  }

  it should "handle the cookie path in a case-sensitive manner (RFC 2965 sec. 3.3.3)" in {
    val cookie = decode("ALPHA=VALUE1")
    val uri = Uri.create("http://www.foo.com/foo/bar")
    val cookieStore = CookieJar(uri, List(cookie))

    cookieStore.get(Uri.create("http://www.FoO.com/Foo/bAr")) shouldBe empty
  }

  it should "not take into account the query parameter in the URI" in {
    val cookie = decode("ALPHA=VALUE1; Domain=www.foo.com; path=/")
    val cookieStore = CookieJar(Uri.create("http://www.foo.com/bar?query1"), List(cookie))

    cookieStore.get(Uri.create("http://www.foo.com/bar?query2")) should have size 1
  }

  it should "should serve the cookies on a subdomain when the domains match" in {
    val cookie = decode("cookie1=VALUE1; Path=/; Domain=foo.org;")
    val cookieStore = CookieJar(Uri.create("https://x.foo.org/"), List(cookie))

    // RFC 6265, 5.1.3.  Domain Matching
    cookieStore.get(Uri.create("https://y.x.foo.org/")) should have size 1
  }

  it should "should serve the last cookie when they are definied twice" in {
    val cookie1 = decode("cookie1=VALUE1; Path=/")
    val cookie2 = decode("cookie1=VALUE2; Path=/")
    val cookie3 = decode("cookie1=VALUE3; Path=/")
    val cookieStore = CookieJar(Uri.create("https://foo.org/"), List(cookie1, cookie2, cookie3))

    val cookies = cookieStore.get(Uri.create("https://foo.org/"))
    cookies should have size 1
    cookies.head.getValue shouldBe "VALUE3"
  }

  it should "should serve cookies based on the host and independently of the port" in {
    // rfc6265#section-1 Cookies for a given host are shared  across all the ports on that host
    val cookie1 = decode("cookie1=VALUE1; Path=/")
    val cookieStore = CookieJar(Uri.create("http://foo.org/moodle/"), List(cookie1))

    val cookie2 = decode("cookie1=VALUE2; Path=/")
    val cookieStore2 = cookieStore.add(Uri.create("https://foo.org:443/moodle/login"), List(cookie2))

    val cookies = cookieStore2.get(Uri.create("http://foo.org/moodle/login"))
    cookies should have size 1
    cookies.head.getValue shouldBe "VALUE2"
  }

  it should "properly deal with same name cookies" in {
    val cookie0 = decode("cookie=VALUE0; path=/")
    val cookieStore0 = CookieJar(Uri.create("http://www.foo.com"), List(cookie0))

    val cookie1 = decode("cookie=VALUE1; path=/foo/bar/")
    val cookieStore1 = cookieStore0.add(Uri.create("http://www.foo.com/foo/bar"), List(cookie1))

    val cookie2 = decode("cookie=VALUE2; path=/foo/baz/")
    val cookieStore2 = cookieStore1.add(Uri.create("http://www.foo.com/foo/baz"), List(cookie2))

    val barCookies = cookieStore2.get(Uri.create("http://www.foo.com/foo/bar/"))
    barCookies should have size 2
    barCookies(0).getValue shouldBe "VALUE1"
    barCookies(1).getValue shouldBe "VALUE0"

    val bazCookies = cookieStore2.get(Uri.create("http://www.foo.com/foo/baz/"))
    bazCookies should have size 2
    bazCookies(0).getValue shouldBe "VALUE2"
    bazCookies(1).getValue shouldBe "VALUE0"
  }

  it should "properly deal with trailing slashes in paths" in {

    val cookie = decode("JSESSIONID=211D17F016132BCBD31D9ABB31D90960; Path=/app/consumer/; HttpOnly")
    val uri = Uri.create("https://vagrant.moolb.com/app/consumer/j_spring_cas_security_check?ticket=ST-5-Q7gzqPpvG3N3Bb02bm3q-llinder-vagrantmgr.moolb.com")
    val cookieStore = CookieJar(uri, List(cookie))

    val cookies = cookieStore.get(Uri.create("https://vagrant.moolb.com/app/consumer/"))
    cookies should have size 1
    cookies(0).getValue shouldBe "211D17F016132BCBD31D9ABB31D90960"
  }
}
