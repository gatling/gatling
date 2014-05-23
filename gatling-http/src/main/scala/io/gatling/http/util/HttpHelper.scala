/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.http.util

import java.net.{ URI, URLDecoder }

import scala.collection.breakOut
import scala.io.Codec.UTF8

import com.ning.http.client.{ FluentCaseInsensitiveStringsMap, Realm }
import com.ning.http.client.Realm.AuthScheme
import com.ning.http.util.AsyncHttpProviderUtils
import com.typesafe.scalalogging.slf4j.StrictLogging

import io.gatling.core.session.{ Expression, Session }
import io.gatling.http.{ HeaderNames, HeaderValues }

object HttpHelper extends StrictLogging {

  val HttpScheme = "http"
  val HttpsScheme = "https"
  val WsScheme = "ws"
  val WssScheme = "wss"
  val OkCodes = Vector(200, 304, 201, 202, 203, 204, 205, 206, 207, 208, 209)
  val RedirectStatusCodes = Vector(301, 302, 303, 307, 308)

  def parseFormBody(body: String): List[(String, String)] = {
      def utf8Decode(s: String) = URLDecoder.decode(s, UTF8.name)

    body
      .split("&")
      .map(_.split("=", 2))
      .map { pair =>
        val paramName = utf8Decode(pair(0))
        val paramValue = if (pair.length > 1) utf8Decode(pair(1)) else ""
        paramName -> paramValue
      }(breakOut)
  }

  def buildBasicAuthRealm(username: Expression[String], password: Expression[String]) = buildRealm(username, password, AuthScheme.BASIC, preemptive = true)
  def buildDigestAuthRealm(username: Expression[String], password: Expression[String]) = buildRealm(username, password, AuthScheme.DIGEST, preemptive = false)
  def buildRealm(username: Expression[String], password: Expression[String], authScheme: AuthScheme, preemptive: Boolean): Expression[Realm] = (session: Session) =>
    for {
      usernameValue <- username(session)
      passwordValue <- password(session)
    } yield buildRealm(usernameValue, passwordValue, authScheme, preemptive)

  def buildBasicAuthRealm(username: String, password: String) = buildRealm(username, password, AuthScheme.BASIC, preemptive = true)
  def buildRealm(username: String, password: String, authScheme: AuthScheme, preemptive: Boolean): Realm = new Realm.RealmBuilder().setPrincipal(username).setPassword(password).setUsePreemptiveAuth(preemptive).setScheme(authScheme).build

  def isCss(headers: FluentCaseInsensitiveStringsMap) = Option(headers.getFirstValue(HeaderNames.ContentType)).exists(_.contains(HeaderValues.TextCss))
  def isHtml(headers: FluentCaseInsensitiveStringsMap) = Option(headers.getFirstValue(HeaderNames.ContentType)).exists(ct => ct.contains(HeaderValues.TextHhtml) || ct.contains(HeaderValues.ApplicationXhtml))
  def isAjax(headers: FluentCaseInsensitiveStringsMap) = Option(headers.getFirstValue(HeaderNames.XRequestedWith)).exists(ct => ct.contains(HeaderValues.XmlHttpRequest))
  def resolveFromURI(rootURI: URI, relative: String) = AsyncHttpProviderUtils.getRedirectUri(rootURI, relative)
  def resolveFromURISilently(rootURI: URI, relative: String): Option[URI] =
    try {
      Some(resolveFromURI(rootURI, relative))
    } catch {
      case e: Exception =>
        logger.info(s"Failed to resolve URI rootURI='$rootURI', relative='$relative'", e)
        None
    }

  def isRedirect(statusCode: Int) = RedirectStatusCodes.contains(statusCode)
  def isPermanentRedirect(statusCode: Int): Boolean = statusCode == 301
  def isNotModified(statusCode: Int) = statusCode == 304

  def isSecure(uri: URI) = uri.getScheme == HttpsScheme || uri.getScheme == WssScheme

  def isAbsoluteHttpUrl(url: String) = url.startsWith(HttpScheme)
  def isAbsoluteWsUrl(url: String) = url.startsWith(WsScheme)
}

