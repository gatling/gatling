/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

package io.gatling.http.util

import java.net.URLDecoder
import java.nio.charset.{ Charset, StandardCharsets }

import scala.collection.{ BitSet, breakOut }
import scala.collection.JavaConverters._
import scala.io.Codec.UTF8
import scala.util.Try
import scala.util.control.NonFatal

import io.gatling.core.session._
import io.gatling.http.client.ahc.uri.Uri
import io.gatling.http.client.realm.{ BasicRealm, DigestRealm, Realm }
import io.gatling.http.{ HeaderNames, HeaderValues }

import io.netty.handler.codec.http.{ HttpHeaders, HttpResponseStatus }
import io.netty.handler.codec.http.HttpResponseStatus._
import com.typesafe.scalalogging.StrictLogging
import io.netty.handler.codec.http.cookie.{ ClientCookieDecoder, Cookie }

object HttpHelper extends StrictLogging {

  val HttpScheme = "http"
  val WsScheme = "ws"
  val OkCodes: BitSet = BitSet.empty + OK.code + NOT_MODIFIED.code + CREATED.code + ACCEPTED.code + NON_AUTHORITATIVE_INFORMATION.code + NO_CONTENT.code + RESET_CONTENT.code + PARTIAL_CONTENT.code + MULTI_STATUS.code + 208 + 209
  private val RedirectStatusCodes = BitSet.empty + MOVED_PERMANENTLY.code + FOUND.code + SEE_OTHER.code + TEMPORARY_REDIRECT.code + PERMANENT_REDIRECT.code

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

  def buildBasicAuthRealm(username: Expression[String], password: Expression[String]): Expression[Realm] =
    (session: Session) =>
      for {
        usernameValue <- username(session)
        passwordValue <- password(session)
      } yield new BasicRealm(usernameValue, passwordValue)

  def buildDigestAuthRealm(username: Expression[String], password: Expression[String]): Expression[Realm] =
    (session: Session) =>
      for {
        usernameValue <- username(session)
        passwordValue <- password(session)
      } yield new DigestRealm(usernameValue, passwordValue)

  private def headerExists(headers: HttpHeaders, headerName: String, f: String => Boolean): Boolean = Option(headers.get(headerName)).exists(f)
  def isCss(headers: HttpHeaders): Boolean = headerExists(headers, HeaderNames.ContentType, _.contains(HeaderValues.TextCss))
  def isHtml(headers: HttpHeaders): Boolean = headerExists(headers, HeaderNames.ContentType, ct => ct.contains(HeaderValues.TextHtml) || ct.contains(HeaderValues.ApplicationXhtml))
  def isAjax(headers: HttpHeaders): Boolean = headerExists(headers, HeaderNames.XRequestedWith, _.contains(HeaderValues.XmlHttpRequest))
  def isTxt(headers: HttpHeaders): Boolean = headerExists(headers, HeaderNames.ContentType, ct => ct.contains("text") || ct.contains("json") || ct.contains("javascript") || ct.contains("xml"))

  def resolveFromUri(rootURI: Uri, relative: String): Uri =
    if (relative.startsWith("//"))
      Uri.create(rootURI.getScheme + ":" + relative)
    else
      Uri.create(rootURI, relative)

  def resolveFromUriSilently(rootURI: Uri, relative: String): Option[Uri] =
    try {
      Some(resolveFromUri(rootURI, relative))
    } catch {
      case NonFatal(e) =>
        logger.info(s"Failed to resolve URI rootURI='$rootURI', relative='$relative'", e)
        None
    }

  def isOk(statusCode: Int): Boolean = OkCodes.contains(statusCode)
  def isRedirect(status: HttpResponseStatus): Boolean = RedirectStatusCodes.contains(status.code)
  def isPermanentRedirect(status: HttpResponseStatus): Boolean = status == MOVED_PERMANENTLY || status == PERMANENT_REDIRECT
  def isNotModified(status: HttpResponseStatus): Boolean = status == NOT_MODIFIED

  def isAbsoluteHttpUrl(url: String): Boolean = url.startsWith(HttpScheme)
  def isAbsoluteWsUrl(url: String): Boolean = url.startsWith(WsScheme)

  def extractCharsetFromContentType(contentType: String): Option[Charset] =
    contentType.indexOf("charset=") match {
      case -1 => None

      case s =>
        var start = s + "charset=".length

        if (contentType.regionMatches(true, start, "UTF-8", 0, 5)) {
          // minor optim, bypass lookup for most common
          Some(StandardCharsets.UTF_8)

        } else {
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

  def responseCookies(headers: HttpHeaders): List[Cookie] = {
    val setCookieValues = headers.getAll(HeaderNames.SetCookie)
    if (setCookieValues.isEmpty) {
      Nil
    } else {
      setCookieValues.asScala.flatMap(setCookie => Option(ClientCookieDecoder.LAX.decode(setCookie)))(breakOut)
    }
  }
}
