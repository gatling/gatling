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

import io.gatling.commons.util.TimeHelper._

import org.asynchttpclient.cookie.Cookie
import org.asynchttpclient.uri.Uri

case class CookieKey(name: String, domain: String, path: String)

case class StoredCookie(cookie: Cookie, hostOnly: Boolean, persistent: Boolean, creationTime: Long)

object CookieJar {

  val UnspecifiedMaxAge = Long.MinValue

  def requestDomain(requestUri: Uri) = requestUri.getHost.toLowerCase

  def requestPath(requestUri: Uri) = requestUri.getPath match {
    case "" => "/"
    case p  => p
  }

  // rfc6265#section-5.2.3
  // Let cookie-domain be the attribute-value without the leading %x2E (".") character.
  def cookieDomain(cookieDomain: Option[String], requestDomain: String) = cookieDomain match {
    case Some(dom) =>
      val domain = (if (dom.charAt(0) == '.') dom.substring(1) else dom).toLowerCase
      (domain, false)
    case None =>
      (requestDomain, true)
  }

  // rfc6265#section-5.2.4
  def cookiePath(rawCookiePath: Option[String], requestPath: String) = {

      // rfc6265#section-5.1.4
      def defaultCookiePath() = requestPath match {
        case p if !p.isEmpty && p.charAt(0) == '/' && p.count(_ == '/') > 1 => p.substring(0, p.lastIndexOf('/'))
        case _ => "/"
      }

    rawCookiePath match {
      case Some(path) if !path.isEmpty && path.charAt(0) == '/' => path
      case _ => defaultCookiePath()
    }
  }

  def hasExpired(c: Cookie): Boolean = {
    val maxAge = c.getMaxAge
    maxAge != CookieJar.UnspecifiedMaxAge && maxAge <= 0
  }

  // rfc6265#section-5.1.3
  // check "The string is a host name (i.e., not an IP address)" ignored
  def domainsMatch(cookieDomain: String, requestDomain: String, hostOnly: Boolean) =
    (hostOnly && requestDomain == cookieDomain) ||
      (requestDomain == cookieDomain || requestDomain.endsWith("." + cookieDomain))

  // rfc6265#section-5.1.4
  def pathsMatch(cookiePath: String, requestPath: String) =
    cookiePath == requestPath ||
      (requestPath.startsWith(cookiePath) && (cookiePath.last == '/' || requestPath.charAt(cookiePath.length) == '/'))

  def apply(uri: Uri, cookies: List[Cookie]): CookieJar = CookieJar(Map.empty).add(uri, cookies)
}

case class CookieJar(store: Map[CookieKey, StoredCookie]) {

  import CookieJar._

  /**
   * @param requestUri       the uri used to deduce defaults for  optional domains and paths
   * @param cookies    the cookies to store
   */
  def add(requestUri: Uri, cookies: List[Cookie]): CookieJar = {

    val thisRequestDomain = requestDomain(requestUri)
    val thisRequestPath = requestPath(requestUri)

    add(thisRequestDomain, thisRequestPath, cookies)
  }

  def add(requestDomain: String, requestPath: String, cookies: List[Cookie]): CookieJar = {

    val newStore = cookies.foldLeft(store) {
      (updatedStore, cookie) =>

        val (keyDomain, hostOnly) = cookieDomain(Option(cookie.getDomain), requestDomain)

        val keyPath = cookiePath(Option(cookie.getPath), requestPath)

        if (hasExpired(cookie)) {
          updatedStore - CookieKey(cookie.getName.toLowerCase, keyDomain, keyPath)

        } else {
          val persistent = cookie.getMaxAge != UnspecifiedMaxAge
          updatedStore + (CookieKey(cookie.getName.toLowerCase, keyDomain, keyPath) -> StoredCookie(cookie, hostOnly, persistent, unpreciseNowMillis))
        }
    }

    CookieJar(newStore)
  }

  def get(requestUri: Uri): List[Cookie] =
    if (store.isEmpty) {
      Nil
    } else {
      val thisRequestDomain = requestDomain(requestUri)

      val thisRequestPath = requestPath(requestUri)

        def isCookieMatching(key: CookieKey, storedCookie: StoredCookie) =
          domainsMatch(key.domain, thisRequestDomain, storedCookie.hostOnly) &&
            pathsMatch(key.path, thisRequestPath) &&
            (!storedCookie.cookie.isSecure || requestUri.isSecured)

      val matchingCookies = store.filter {
        case (key, storedCookie) => isCookieMatching(key, storedCookie)
      }

      matchingCookies.toList.sortWith {
        (entry1, entry2) =>

          val (key1, storedCookie1) = entry1
          val (key2, storedCookie2) = entry2

          if (key1.path.length == key2.path.length)
            storedCookie1.creationTime < storedCookie2.creationTime
          else
            key1.path.length >= key2.path.length

      }.map(_._2.cookie)
    }
}
