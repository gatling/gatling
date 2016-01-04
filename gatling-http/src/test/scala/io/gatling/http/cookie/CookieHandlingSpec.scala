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
import io.gatling.core.session.Session

import org.asynchttpclient.cookie.CookieDecoder.decode
import org.asynchttpclient.uri.Uri

class CookieHandlingSpec extends BaseSpec {

  val emptySession = Session("scenarioName", 0)

  "getStoredCookies" should "be able to get a cookie from session" in {
    val originalCookie = decode("ALPHA=VALUE1; Domain=docs.foo.com; Path=/; Expires=Wed, 13-Jan-2021 22:23:01 GMT; Secure; HttpOnly")
    val originalDomain = "docs.foo.com"
    val originalCookieJar = new CookieJar(Map(CookieKey("ALPHA", originalDomain, "/") -> StoredCookie(originalCookie, hostOnly = true, persistent = true, 0L)))
    val originalSession = Session("scenarioName", 0, Map(CookieSupport.CookieJarAttributeName -> originalCookieJar))
    CookieSupport.getStoredCookies(originalSession, "https://docs.foo.com/accounts").map(x => x.getValue) shouldBe List("VALUE1")
  }

  it should "be called with an empty session" in {
    CookieSupport.getStoredCookies(emptySession, "https://docs.foo.com/accounts") shouldBe empty
  }

  "storeCookies" should "be able to store a cookie in an empty session" in {
    val newCookie = decode("ALPHA=VALUE1; Domain=docs.foo.com; Path=/accounts; Expires=Wed, 13-Jan-2021 22:23:01 GMT; Secure; HttpOnly")
    CookieSupport.storeCookies(emptySession, Uri.create("https://docs.foo.com/accounts"), List(newCookie))

    CookieSupport.getStoredCookies(emptySession, "https://docs.foo.com/accounts") shouldBe empty
  }
}
