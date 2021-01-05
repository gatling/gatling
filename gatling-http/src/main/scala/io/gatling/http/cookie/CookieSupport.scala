/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

import io.gatling.commons.validation._
import io.gatling.core.session.{ Session, SessionPrivateAttributes }
import io.gatling.core.session.Expression
import io.gatling.http.action.cookie.{ AddCookieBuilder, AddCookieDsl, GetCookieBuilder, GetCookieDsl }
import io.gatling.http.cache.HttpCaches
import io.gatling.http.client.uri.Uri

import io.netty.handler.codec.http.cookie.Cookie

private[http] object CookieSupport {

  val CookieJarAttributeName: String = SessionPrivateAttributes.PrivateAttributePrefix + "http.cookies"
  private val NoCookieJarFailure = "No CookieJar in session".failure

  def cookieJar(session: Session): Option[CookieJar] = session.attributes.get(CookieJarAttributeName).map(_.asInstanceOf[CookieJar])

  def getStoredCookies(session: Session, uri: Uri): List[Cookie] =
    cookieJar(session) match {
      case Some(cookieJar) => cookieJar.get(uri)
      case _               => Nil
    }

  private def getOrCreateCookieJar(session: Session) =
    cookieJar(session) match {
      case Some(cookieJar) => cookieJar
      case _               => CookieJar.Empty
    }

  def storeCookies(session: Session, uri: Uri, cookies: List[Cookie], nowMillis: Long): Session =
    if (cookies.isEmpty) {
      session
    } else {
      val cookieJar = getOrCreateCookieJar(session)
      session.set(CookieJarAttributeName, cookieJar.add(uri, cookies, nowMillis))
    }

  def storeCookie(session: Session, domain: String, path: String, cookie: Cookie, nowMillis: Long): Session = {
    val cookieJar = getOrCreateCookieJar(session)
    session.set(CookieJarAttributeName, cookieJar.add(domain, path, cookie :: Nil, nowMillis))
  }

  def getCookieValue(session: Session, domain: String, path: String, name: String, secure: Boolean): Validation[String] =
    cookieJar(session) match {
      case Some(cookieJar) =>
        cookieJar.get(domain, path, secure = secure).filter(_.name == name) match {
          case Nil           => s"No Cookie matching parameters domain=$domain, path=$path, name=$name, secure=$secure".failure
          case cookie :: Nil => cookie.value.success
          case _             => s"Found more than one matching cookie domain=$domain, path=$path, name=$name, secure=$secure!!?".failure
        }
      case _ => NoCookieJarFailure
    }

  val FlushSessionCookies: Expression[Session] = session =>
    cookieJar(session) match {
      case Some(cookieJar) =>
        val storeWithOnlyPersistentCookies = cookieJar.store.filter { case (_, storeCookie) => storeCookie.persistent }
        session.set(CookieJarAttributeName, CookieJar(storeWithOnlyPersistentCookies)).success
      case _ => session.success
    }

  val FlushCookieJar: Expression[Session] = _.remove(CookieJarAttributeName).success
}

trait CookieSupport {
  def addCookie(cookie: AddCookieDsl): AddCookieBuilder = AddCookieBuilder(cookie)
  def getCookieValue(cookie: GetCookieDsl): GetCookieBuilder = GetCookieBuilder(cookie)
  def flushSessionCookies: Expression[Session] = CookieSupport.FlushSessionCookies
  def flushCookieJar: Expression[Session] = CookieSupport.FlushCookieJar
  def flushHttpCache: Expression[Session] = HttpCaches.FlushCache

  def Cookie(name: Expression[String], value: Expression[String]): AddCookieDsl =
    AddCookieDsl(name, value, domain = None, path = None, maxAge = None, secure = false)
  def CookieKey(name: Expression[String]): GetCookieDsl = GetCookieDsl(name, domain = None, path = None, secure = false, saveAs = None)
}
