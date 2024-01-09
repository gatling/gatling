/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

class HttpHelperSampleJava {

  static {
//#addCookie
exec(addCookie(Cookie("name", "value")));
//#addCookie

//#cookie
// with static values
Cookie("name", "value")
  .withDomain("domain")
  .withPath("path")
  .withMaxAge(10)
  .withSecure(true);

// with Gatling EL strings
Cookie("#{name}", "#{value}")
  .withDomain("domain")
  .withPath("path")
  .withMaxAge(10)
  .withSecure(true);

// with functions
Cookie(
  session -> session.getString("cookieName"),
  session -> session.getString("cookieValue")
)
  .withDomain("domain")
  .withPath("path")
  .withMaxAge(10)
  .withSecure(true);
//#cookie

//#getCookie
exec(getCookieValue(CookieKey("name")));
//#getCookie

//#cookieKey
// with static values
CookieKey("name")
  .withDomain("domain")
  .withPath("path")
  .withSecure(true)
  .saveAs("key");

// with Gatling EL strings
CookieKey("#{name}")
  .withDomain("#{domain}")
  .withPath("path")
  .withSecure(true)
  .saveAs("key");

// with functions
CookieKey(session -> session.getString("cookieName"))
  .withDomain(session -> session.getString("cookieDomain"))
  .withPath("path")
  .withSecure(true)
  .saveAs("key");
//#cookieKey

//#flushSessionCookies
exec(flushSessionCookies());
//#flushSessionCookies

//#flushCookieJar
exec(flushCookieJar());
//#flushCookieJar

//#flushHttpCache
exec(flushHttpCache());
//#flushHttpCache
  }
}
