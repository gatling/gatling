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

import java.net.URLDecoder
import java.nio.charset.Charset

import com.ning.http.client.uri.Uri

import scala.collection.breakOut
import scala.io.Codec.UTF8
import scala.util.Try

import com.ning.http.client.{ FluentCaseInsensitiveStringsMap, Realm }
import com.ning.http.client.Realm.AuthScheme
import com.typesafe.scalalogging.slf4j.StrictLogging

import io.gatling.core.session._
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

  def buildBasicAuthRealm(username: Expression[String], password: Expression[String]) =
    buildRealm(username, password, AuthScheme.BASIC, preemptive = true, None, None)

  def buildDigestAuthRealm(username: Expression[String], password: Expression[String]) =
    buildRealm(username, password, AuthScheme.DIGEST, preemptive = false, None, None)

  def buildNTLMAuthRealm(username: Expression[String], password: Expression[String], ntlmDomain: Option[Expression[String]], ntlmHost: Option[Expression[String]]) =
    buildRealm(username, password, AuthScheme.NTLM, preemptive = false, ntlmDomain, ntlmHost)

  def buildRealm(username: Expression[String],
                 password: Expression[String],
                 authScheme: AuthScheme,
                 preemptive: Boolean,
                 ntlmDomain: Option[Expression[String]],
                 ntlmHost: Option[Expression[String]]): Expression[Realm] =
    (session: Session) =>
      for {
        usernameValue <- username(session)
        passwordValue <- password(session)
        ntlmDomainValue <- resolveOptionalExpression(ntlmDomain, session)
        ntlmHostValue <- resolveOptionalExpression(ntlmHost, session)
      } yield new Realm.RealmBuilder()
        .setPrincipal(usernameValue)
        .setPassword(passwordValue)
        .setUsePreemptiveAuth(preemptive)
        .setScheme(authScheme)
        .setNtlmDomain(ntlmDomainValue.orNull)
        .setNtlmHost(ntlmHostValue.orNull)
        .build

  private def headerExists(headers: FluentCaseInsensitiveStringsMap, headerName: String, f: String => Boolean): Boolean = Option(headers.getFirstValue(headerName)).exists(f)
  def isCss(headers: FluentCaseInsensitiveStringsMap): Boolean = headerExists(headers, HeaderNames.ContentType, _.contains(HeaderValues.TextCss))
  def isHtml(headers: FluentCaseInsensitiveStringsMap): Boolean = headerExists(headers, HeaderNames.ContentType, ct => ct.contains(HeaderValues.TextHtml) || ct.contains(HeaderValues.ApplicationXhtml))
  def isAjax(headers: FluentCaseInsensitiveStringsMap): Boolean = headerExists(headers, HeaderNames.XRequestedWith, _.contains(HeaderValues.XmlHttpRequest))
  def isTxt(headers: FluentCaseInsensitiveStringsMap): Boolean = headerExists(headers, HeaderNames.ContentType, ct => ct.contains("text") || ct.contains("json") || ct.contains("javascript") || ct.contains("xml"))

  def resolveFromUri(rootURI: Uri, relative: String): Uri =
    if (relative.startsWith("//"))
      Uri.create(rootURI.getScheme + ":" + relative)
    else
      Uri.create(rootURI, relative)

  def resolveFromUriSilently(rootURI: Uri, relative: String): Option[Uri] =
    try {
      Some(resolveFromUri(rootURI, relative))
    } catch {
      case e: Exception =>
        logger.info(s"Failed to resolve URI rootURI='$rootURI', relative='$relative'", e)
        None
    }

  def isRedirect(statusCode: Int) = RedirectStatusCodes.contains(statusCode)
  def isPermanentRedirect(statusCode: Int): Boolean = statusCode == 301 || statusCode == 308
  def isNotModified(statusCode: Int) = statusCode == 304

  def isSecure(uri: Uri) = uri.getScheme == HttpsScheme || uri.getScheme == WssScheme

  def isAbsoluteHttpUrl(url: String) = url.startsWith(HttpScheme)
  def isAbsoluteWsUrl(url: String) = url.startsWith(WsScheme)

  def extractCharsetFromContentType(contentType: String): Option[Charset] =
    contentType.indexOf("charset=") match {
      case -1 => None

      case s =>
        var start = s + "charset=".length

        var end = contentType.indexOf(';', start) match {
          case -1 => contentType.length

          case e  => e
        }

        Try {
          while (contentType.charAt(start) == ' ' && start < end)
            start += 1

          while (contentType.charAt(end - 1) == ' ' && end > start)
            end -= 1

          if (contentType.charAt(start) == '"' && start < end)
            start += 1

          if (contentType.charAt(end - 1) == '"' && end > start)
            end -= 1

          val charsetString = contentType.substring(start, end)

          Charset.forName(charsetString)
        }.toOption
    }
}
