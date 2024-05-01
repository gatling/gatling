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

package io.gatling.http.cookie

import java.util.Locale

import io.gatling.http.client.uri.Uri

import io.netty.handler.codec.http.cookie.Cookie

private[cookie] final case class CookieKey(name: String, domain: String, path: String)

private[cookie] final case class StoredCookie(cookie: Cookie, hostOnly: Boolean, persistent: Boolean, creationTime: Long)

private[cookie] object CookieJar {
  val Empty: CookieJar = CookieJar(Map.empty)

  private def requestDomain(requestUri: Uri) = requestUri.getHost.toLowerCase(Locale.ROOT)

  // rfc6265#section-5.2.3
  // Let cookie-domain be the attribute-value without the leading %x2E (".") character.
  private def cookieDomain(cookieDomain: Option[String], requestDomain: String) = cookieDomain match {
    case Some(dom) =>
      ((if (dom.charAt(0) == '.') dom.substring(1) else dom).toLowerCase(Locale.ROOT), false)
    case _ =>
      (requestDomain, true)
  }

  // rfc6265#section-5.2.4
  private def cookiePath(rawCookiePath: Option[String], requestPath: String) =
    rawCookiePath match {
      case Some(path) if path.nonEmpty && path.charAt(0) == '/' => path
      case _                                                    =>
        // rfc6265#section-5.1.4
        Uri.getLastDirectoryPath(requestPath)
    }

  private def hasExpired(c: Cookie): Boolean = {
    val maxAge = c.maxAge
    maxAge != Cookie.UNDEFINED_MAX_AGE && maxAge <= 0
  }

  // rfc6265#section-5.1.3
  // check "The string is a host name (i.e., not an IP address)" ignored
  private def domainsMatch(cookieDomain: String, requestDomain: String, hostOnly: Boolean) =
    (hostOnly && requestDomain == cookieDomain) ||
      (requestDomain == cookieDomain || requestDomain.endsWith("." + cookieDomain))

  // rfc6265#section-5.1.4
  private def pathsMatch(cookiePath: String, requestPath: String) =
    cookiePath == requestPath ||
      (requestPath.startsWith(cookiePath) && (cookiePath.last == '/' || requestPath.charAt(cookiePath.length) == '/'))

  def apply(uri: Uri, cookies: List[Cookie], nowMillis: Long): CookieJar = Empty.add(uri, cookies, nowMillis)
}

private[http] final case class CookieJar(store: Map[CookieKey, StoredCookie]) {
  import CookieJar._

  /**
   * @param requestUri
   *   the uri used to deduce defaults for optional domains and paths
   * @param cookies
   *   the cookies to store
   */
  def add(requestUri: Uri, cookies: List[Cookie], nowMillis: Long): CookieJar =
    add(requestDomain(requestUri), requestUri.getNonEmptyPath, cookies, nowMillis)

  def add(requestDomain: String, requestPath: String, cookies: List[Cookie], nowMillis: Long): CookieJar = {
    val newStore = cookies.foldLeft(store) { (updatedStore, cookie) =>
      val (keyDomain, hostOnly) = cookieDomain(Option(cookie.domain), requestDomain)

      val keyPath = cookiePath(Option(cookie.path), requestPath)

      if (hasExpired(cookie)) {
        updatedStore - CookieKey(cookie.name, keyDomain, keyPath)
      } else {
        val persistent = cookie.maxAge != Cookie.UNDEFINED_MAX_AGE
        updatedStore + (CookieKey(cookie.name, keyDomain, keyPath) -> StoredCookie(cookie, hostOnly, persistent, nowMillis))
      }
    }

    CookieJar(newStore)
  }
  // used by getCookieValue
  def find(name: String, requestDomain: String, requestPath: Option[String], requestSecure: Option[Boolean]): List[Cookie] =
    if (store.isEmpty) {
      Nil
    } else {
      store.collect {
        case (key, storedCookie)
            if key.name == name &&
              domainsMatch(key.domain, requestDomain, storedCookie.hostOnly) &&
              requestPath.forall(pathsMatch(key.path, _))
              && requestSecure.forall(_ == storedCookie.cookie.isSecure) =>
          storedCookie.cookie
      }.toList
    }

  // used by getStoredCookies, for HTTP requests
  def get(requestUri: Uri): List[Cookie] =
    if (store.isEmpty) {
      Nil
    } else {
      val domain = requestDomain(requestUri)
      val path = requestUri.getNonEmptyPath
      val secure = requestUri.isSecured

      val matchingCookies = store.filter { case (key, storedCookie) =>
        domainsMatch(key.domain, domain, storedCookie.hostOnly) &&
        pathsMatch(key.path, path) &&
        (secure || !storedCookie.cookie.isSecure)
      }

      matchingCookies.toList
        .sortWith { (entry1, entry2) =>
          val (key1, storedCookie1) = entry1
          val (key2, storedCookie2) = entry2

          if (key1.path.length == key2.path.length)
            storedCookie1.creationTime < storedCookie2.creationTime
          else
            key1.path.length >= key2.path.length
        }
        .map(_._2.cookie)
    }
}
