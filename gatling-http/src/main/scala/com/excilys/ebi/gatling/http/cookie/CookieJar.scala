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

import scala.annotation.tailrec

import com.ning.http.client.cookie.Cookie

import com.excilys.ebi.gatling.core.util.TimeHelper.nowMillis
import com.excilys.ebi.gatling.http.util.HttpHelper.isSecure

case class CookieKey(name: String, domain: String, path: String)

case class StoredCookie(cookie: Cookie, hostOnly: Boolean, persistent: Boolean, creationTime: Long)

object CookieJar {

  val unspecifiedMaxAge = -1
  val unspecifiedExpires = -1L

  def requestDomain(requestURI: URI) = requestURI.getHost.toLowerCase

  def requestPath(requestURI: URI) = requestURI.getPath match {
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
      case Some(path) if path.charAt(0) == '/' => path
      case _                                   => defaultCookiePath()
    }
  }

  def hasExpired(c: Cookie): Boolean = {
    val maxAge = c.getMaxAge
    val expires = c.getExpires
    (maxAge != CookieJar.unspecifiedMaxAge && maxAge <= 0) || (expires != CookieJar.unspecifiedExpires && expires <= nowMillis)
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

  def apply(uri: URI, cookies: List[Cookie]): CookieJar = CookieJar(Map.empty).add(uri, cookies)
}

case class CookieJar(store: Map[CookieKey, StoredCookie]) {

  /**
   * @param requestURI       the uri used to deduce defaults for  optional domains and paths
   * @param cookies    the cookies to store
   */
  def add(requestURI: URI, cookies: List[Cookie]): CookieJar = {

    val requestDomain = CookieJar.requestDomain(requestURI)
    val requestPath = CookieJar.requestPath(requestURI)

    add(requestDomain, requestPath, cookies)
  }

  def add(requestDomain: String, requestPath: String, cookies: List[Cookie]): CookieJar = {

    val newStore = cookies.foldLeft(store) {
      (updatedStore, cookie) =>

        val (keyDomain, hostOnly) = CookieJar.cookieDomain(Option(cookie.getDomain), requestDomain)

        val keyPath = CookieJar.cookiePath(Option(cookie.getPath), requestPath)

        if (CookieJar.hasExpired(cookie)) {
          updatedStore - CookieKey(cookie.getName.toLowerCase, keyDomain, keyPath)

        } else {
          val persistent = cookie.getExpires != CookieJar.unspecifiedExpires || cookie.getMaxAge != CookieJar.unspecifiedMaxAge
          updatedStore + (CookieKey(cookie.getName.toLowerCase, keyDomain, keyPath) -> StoredCookie(cookie, hostOnly, persistent, nowMillis))
        }
    }

    CookieJar(newStore)
  }

  def get(requestURI: URI): List[Cookie] = {

    if (store.isEmpty) {
      Nil
    } else {
      val requestDomain = CookieJar.requestDomain(requestURI)

      val requestPath = CookieJar.requestPath(requestURI)

      val secureURI = isSecure(requestURI)

        def isCookieMatching(key: CookieKey, storedCookie: StoredCookie) =
          CookieJar.domainsMatch(key.domain, requestDomain, storedCookie.hostOnly) &&
            CookieJar.pathsMatch(key.path, requestPath) &&
            (!storedCookie.cookie.isSecure || secureURI)

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
}
