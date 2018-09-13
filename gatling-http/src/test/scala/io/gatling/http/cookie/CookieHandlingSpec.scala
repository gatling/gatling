/*
 * Copyright 2011-2018 GatlingCorp (https://gatling.io)
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
import io.gatling.core.session.Session
import io.gatling.http.client.ahc.uri.Uri

import io.netty.handler.codec.http.cookie.ClientCookieDecoder.LAX.decode

class CookieHandlingSpec extends BaseSpec {

  val emptySession = Session("scenarioName", 0, System.currentTimeMillis())

  "getStoredCookies" should "be able to get a cookie from session" in {
    val originalCookie = decode("ALPHA=VALUE1; Domain=docs.foo.com; Path=/; Expires=Wed, 13-Jan-2021 22:23:01 GMT; Secure; HttpOnly")
    val originalDomain = "docs.foo.com"
    val originalCookieJar = new CookieJar(Map(CookieKey("ALPHA", originalDomain, "/") -> StoredCookie(originalCookie, hostOnly = true, persistent = true, 0L)))
    val originalSession = Session("scenarioName", 0, System.currentTimeMillis(), Map(CookieSupport.CookieJarAttributeName -> originalCookieJar))
    CookieSupport.getStoredCookies(originalSession, "https://docs.foo.com/accounts").map(x => x.value) shouldBe List("VALUE1")
  }

  it should "be called with an empty session" in {
    CookieSupport.getStoredCookies(emptySession, "https://docs.foo.com/accounts") shouldBe empty
  }

  "storeCookies" should "be able to store a cookie in an empty session" in {
    val newCookie = decode("ALPHA=VALUE1; Domain=docs.foo.com; Path=/accounts; Expires=Wed, 13-Jan-2021 22:23:01 GMT; Secure; HttpOnly")
    CookieSupport.storeCookies(emptySession, Uri.create("https://docs.foo.com/accounts"), List(newCookie), System.currentTimeMillis())

    CookieSupport.getStoredCookies(emptySession, "https://docs.foo.com/accounts") shouldBe empty
  }

  "getSecureStoredCookies" should "be able to get a secure cookie from session" in {
    val originalCookie = decode("ALPHA=VALUE1; Domain=docs.foo.com; Path=/; Expires=Wed, 13-Jan-2021 22:23:01 GMT; Secure; HttpOnly")
    val originalDomain = "docs.foo.com"
    val originalCookieJar = new CookieJar(Map(CookieKey("ALPHA", originalDomain, "/") -> StoredCookie(originalCookie, hostOnly = true, persistent = true, 0L)))
    val originalSession = Session("scenarioName", 0, System.currentTimeMillis(), Map(CookieSupport.CookieJarAttributeName -> originalCookieJar))
    CookieSupport.getStoredCookies(originalSession, "https://docs.foo.com/accounts").map(x => x.value) shouldBe List("VALUE1")
    CookieSupport.getStoredCookies(originalSession, "https://docs.foo.com/accounts").map(x => x.isSecure) shouldBe List(true)
  }

  "getNonSecureStoredCookies" should "be able to get a nonsecure cookie from session" in {
    val originalCookie = decode("ALPHA=VALUE6; Domain=docs.foo.com; Path=/; Expires=Wed, 13-Jan-2021 22:23:01 GMT; HttpOnly")
    val originalDomain = "docs.foo.com"
    val originalCookieJar = new CookieJar(Map(CookieKey("ALPHA", originalDomain, "/") -> StoredCookie(originalCookie, hostOnly = true, persistent = true, 0L)))
    val originalSession = Session("scenarioName", 0, System.currentTimeMillis(), Map(CookieSupport.CookieJarAttributeName -> originalCookieJar))
    CookieSupport.getStoredCookies(originalSession, "https://docs.foo.com/accounts").map(x => x.value) shouldBe List("VALUE6")
    CookieSupport.getStoredCookies(originalSession, "https://docs.foo.com/accounts").map(x => x.isSecure) shouldBe List(false)
  }

  "getNonSecureStoredCookiesForHttp" should "be unable to get a secure cookie from session for http uri" in {
    val originalCookie = decode("ALPHA=VALUE6; Domain=docs.foo.com; Path=/; Expires=Wed, 13-Jan-2021 22:23:01 GMT; Secure; HttpOnly")
    val originalDomain = "docs.foo.com"
    val originalCookieJar = new CookieJar(Map(CookieKey("ALPHA", originalDomain, "/") -> StoredCookie(originalCookie, hostOnly = true, persistent = true, 0L)))
    val originalSession = Session("scenarioName", 0, System.currentTimeMillis(), Map(CookieSupport.CookieJarAttributeName -> originalCookieJar))
    CookieSupport.getStoredCookies(originalSession, "http://docs.foo.com/accounts").size shouldBe 0
  }

}
