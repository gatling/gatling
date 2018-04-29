/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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
import io.gatling.http.client.ahc.uri.Uri
import io.gatling.http.util.HttpTypeCaster

import io.netty.handler.codec.http.cookie.Cookie

object CookieSupport {

  // import optimized TypeCaster
  import HttpTypeCaster._

  val CookieJarAttributeName: String = SessionPrivateAttributes.PrivateAttributePrefix + "http.cookies"
  private val NoCookieJarFailure = "No CookieJar in session".failure

  def cookieJar(session: Session): Option[CookieJar] = session(CookieJarAttributeName).asOption[CookieJar]

  def getStoredCookies(session: Session, url: String): List[Cookie] = getStoredCookies(session, Uri.create(url))

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

  def storeCookies(session: Session, uri: Uri, cookies: List[Cookie], nowMillis: Long): Session = {
    val cookieJar = getOrCreateCookieJar(session)
    session.set(CookieJarAttributeName, cookieJar.add(uri, cookies, nowMillis))
  }

  def storeCookie(session: Session, domain: String, path: String, cookie: Cookie, nowMillis: Long): Session = {
    val cookieJar = getOrCreateCookieJar(session)
    session.set(CookieJarAttributeName, cookieJar.add(domain, path, List(cookie), nowMillis))
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
      case None => session.success
      case Some(cookieJar) =>
        val storeWithOnlyPersistentCookies = cookieJar.store.filter { case (_, storeCookie) => storeCookie.persistent }
        session.set(CookieJarAttributeName, CookieJar(storeWithOnlyPersistentCookies)).success
    }

  val FlushCookieJar: Expression[Session] = _.remove(CookieJarAttributeName).success
}
