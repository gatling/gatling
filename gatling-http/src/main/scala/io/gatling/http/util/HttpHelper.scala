/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
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
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8

import scala.collection.{ breakOut, BitSet }
import scala.collection.JavaConverters._
import scala.util.Try
import scala.util.control.NonFatal

import io.gatling.core.session._
import io.gatling.http.client.realm.{ BasicRealm, DigestRealm, Realm }
import io.gatling.http.client.uri.Uri
import io.gatling.http.{ MissingNettyHttpHeaderNames, MissingNettyHttpHeaderValues }

import com.typesafe.scalalogging.StrictLogging
import io.netty.handler.codec.http.{ HttpHeaderNames, HttpHeaders, HttpResponseStatus }
import io.netty.handler.codec.http.HttpResponseStatus._
import io.netty.handler.codec.http.cookie.{ ClientCookieDecoder, Cookie }

private[gatling] object HttpHelper extends StrictLogging {

  val HttpScheme = "http"
  val WsScheme = "ws"
  val OkCodes
      : BitSet = BitSet.empty + OK.code + NOT_MODIFIED.code + CREATED.code + ACCEPTED.code + NON_AUTHORITATIVE_INFORMATION.code + NO_CONTENT.code + RESET_CONTENT.code + PARTIAL_CONTENT.code + MULTI_STATUS.code + 208 + 209
  private val RedirectStatusCodes = BitSet.empty + MOVED_PERMANENTLY.code + FOUND.code + SEE_OTHER.code + TEMPORARY_REDIRECT.code + PERMANENT_REDIRECT.code

  def parseFormBody(body: String): List[(String, String)] = {
    def utf8Decode(s: String) = URLDecoder.decode(s, UTF_8.name)

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

  private def headerExists(headers: HttpHeaders, headerName: CharSequence, f: String => Boolean): Boolean = Option(headers.get(headerName)).exists(f)
  def isCss(headers: HttpHeaders): Boolean = headerExists(headers, HttpHeaderNames.CONTENT_TYPE, _.startsWith(MissingNettyHttpHeaderValues.TextCss.toString))
  def isHtml(headers: HttpHeaders): Boolean =
    headerExists(
      headers,
      HttpHeaderNames.CONTENT_TYPE,
      ct => ct.startsWith(MissingNettyHttpHeaderValues.TextHtml.toString) || ct.startsWith(MissingNettyHttpHeaderValues.ApplicationXhtml.toString)
    )
  def isAjax(headers: HttpHeaders): Boolean =
    headerExists(headers, MissingNettyHttpHeaderNames.XRequestedWith, _ == MissingNettyHttpHeaderValues.XmlHttpRequest.toString)

  private val ApplicationStart = "application/"
  private val ApplicationStartOffset = ApplicationStart.length
  private val ApplicationJavascriptEnd = "javascript"
  private val ApplicationJsonEnd = "json"
  private val ApplicationXmlEnd = "xml"
  private val ApplicationFormUrlEncodedEnd = "x-www-form-urlencoded"
  private val ApplicationXhtmlEnd = "xhtml+xml"
  private val TextStart = "text/"
  private val TextStartOffset = TextStart.length
  private val TextCssEnd = "css"
  private val TextCsvEnd = "csv"
  private val TextHtmlEnd = "html"
  private val TextJavascriptEnd = "javascript"
  private val TextPlainEnd = "plain"
  private val TextXmlEnd = "xml"

  def isText(headers: HttpHeaders): Boolean =
    headerExists(
      headers,
      HttpHeaderNames.CONTENT_TYPE,
      ct =>
        ct.startsWith(ApplicationStart) && (
          ct.startsWith(ApplicationJavascriptEnd, ApplicationStartOffset)
            || ct.startsWith(ApplicationJsonEnd, ApplicationStartOffset)
            || ct.startsWith(ApplicationXmlEnd, ApplicationStartOffset)
            || ct.startsWith(ApplicationFormUrlEncodedEnd, ApplicationStartOffset)
            || ct.startsWith(ApplicationXhtmlEnd, ApplicationStartOffset)
        )
          || (ct.startsWith(TextStart) && (
            ct.startsWith(TextCssEnd, TextStartOffset)
              || ct.startsWith(TextCsvEnd, TextStartOffset)
              || ct.startsWith(TextHtmlEnd, TextStartOffset)
              || ct.startsWith(TextJavascriptEnd, TextStartOffset)
              || ct.startsWith(TextJavascriptEnd, TextStartOffset)
              || ct.startsWith(TextPlainEnd, TextStartOffset)
              || ct.startsWith(TextXmlEnd, TextStartOffset)
          ))
    )

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

        if (contentType.regionMatches(true, start, UTF_8.name, 0, 5)) {
          // minor optim, bypass lookup for most common
          Some(UTF_8)

        } else {
          var end = contentType.indexOf(';', start) match {
            case -1 => contentType.length
            case e  => e
          }

          try {
            while (contentType.charAt(start) == ' ' && start < end) start += 1

            while (contentType.charAt(end - 1) == ' ' && end > start) end -= 1

            if (contentType.charAt(start) == '"' && start < end)
              start += 1

            if (contentType.charAt(end - 1) == '"' && end > start)
              end -= 1

            val charsetString = contentType.substring(start, end)

            Some(Charset.forName(charsetString))
          } catch {
            case NonFatal(_) => None
          }
        }
    }

  def responseCookies(headers: HttpHeaders): List[Cookie] = {
    val setCookieValues = headers.getAll(HttpHeaderNames.SET_COOKIE)
    if (setCookieValues.isEmpty) {
      Nil
    } else {
      setCookieValues.asScala.flatMap(setCookie => Option(ClientCookieDecoder.LAX.decode(setCookie)).toList)(breakOut)
    }
  }
}
