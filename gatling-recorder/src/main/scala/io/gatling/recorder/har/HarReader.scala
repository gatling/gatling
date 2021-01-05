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

package io.gatling.recorder.har

import java.io.{ BufferedInputStream, FileInputStream, InputStream }
import java.net.{ URL, URLEncoder }
import java.nio.charset.StandardCharsets.UTF_8
import java.time.ZonedDateTime
import java.util.{ Base64, Locale }

import scala.util.{ Try, Using }

import io.gatling.commons.util.StringHelper._
import io.gatling.core.filter.Filters
import io.gatling.recorder.har.HarParser._
import io.gatling.recorder.model._

import io.netty.handler.codec.http.{ DefaultHttpHeaders, HttpHeaderNames, HttpHeaderValues, HttpHeaders, HttpMethod }
import io.netty.util.AsciiString

final case class HttpTransaction(request: HttpRequest, response: HttpResponse)

private[recorder] object HarReader {

  def readFile(path: String, filters: Option[Filters]): Seq[HttpTransaction] =
    Using.resource(new BufferedInputStream(new FileInputStream(path)))(readStream(_, filters))

  private[har] def readStream(is: InputStream, filters: Option[Filters]): Seq[HttpTransaction] = {
    val harEntries = HarParser.parseHarEntries(is)
    val filteredHarEntries = harEntries
      .filter(entry => filters.forall(_.accept(entry.request.url) && Filters.BrowserNoiseFilters.accept(entry.request.url)))
    buildHttpTransactions(filteredHarEntries)
  }

  private def parseMillisFromIso8601DateTime(time: String): Long =
    ZonedDateTime.parse(time).toInstant.toEpochMilli

  private def buildHttpTransactions(harEntries: Seq[HarEntry]): Seq[HttpTransaction] =
    harEntries.iterator
      // filter out cancelled requests
      .filter(_.response.status != 0)
      // filter out all non-HTTP protocols (eg: ws://)
      .filter(_.request.url.toString.toLowerCase(Locale.ROOT).startsWith("http"))
      // filter out CONNECT (if HAR was generated with a proxy such as Charles) and Upgrade requests (WebSockets)
      .filter(entry =>
        entry.request.method != HttpMethod.CONNECT.name && !entry.request.headers.exists(header =>
          AsciiString.contentEqualsIgnoreCase(header.name, HttpHeaderValues.UPGRADE)
        )
      )
      .filter(entry => isValidURL(entry.request.url))
      .map(buildHttpTransaction)
      .toVector
      // Chrome can mess up with request order
      .sortBy(_.request.timestamp)

  private def isValidURL(url: String): Boolean = Try(new URL(url)).isSuccess

  private def buildHttpTransaction(entry: HarEntry): HttpTransaction = {
    val start = parseMillisFromIso8601DateTime(entry.startedDateTime)
    val time = entry.time
      .orElse(entry.timings.map(_.time)) // FIXME FireFox has a null time on redirect, should open an issue
      .getOrElse(throw new IllegalArgumentException("Neither time nor timings"))
      .toLong
    val end = start + time
    HttpTransaction(
      buildRequest(entry.request, start),
      buildResponse(entry.response, end)
    )
  }

  private val WrappedValue = "\"(.*)\"".r
  private def unwrap(raw: String): String = raw match {
    case WrappedValue(unwrapped) => unwrapped
    case _                       => raw
  }

  private def buildHeaders(harHeaders: Seq[HarHeader]): HttpHeaders = {
    val headers = new DefaultHttpHeaders(false)
    harHeaders.foreach { harHeader =>
      headers.add(harHeader.name, unwrap(harHeader.value))
    }
    headers
  }

  private def buildRequest(request: HarRequest, timestamp: Long): HttpRequest = {

    val headers = buildHeaders(request.headers)
    val body = request.postData.flatMap(buildRequestBody(_, headers)).getOrElse(Array.empty)

    HttpRequest(
      httpVersion = request.httpVersion,
      method = request.method,
      uri = request.url,
      headers = headers,
      body = body,
      timestamp
    )
  }

  private def encode(s: String): String = URLEncoder.encode(s, UTF_8.name)

  private def buildRequestBody(postData: HarRequestPostData, requestHeaders: HttpHeaders): Option[Array[Byte]] =
    postData.text.flatMap(_.trimToOption) match {
      case Some(string) =>
        Some(string.getBytes(UTF_8))

      case _ =>
        // FIXME only honor params for ApplicationFormUrlEncoded for now. Charles seems utterly broken for MultipartFormData
        if (
          postData.params.nonEmpty && Option(requestHeaders.get(HttpHeaderNames.CONTENT_TYPE))
            .exists(AsciiString.contains(_, HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED))
        ) {
          Some(postData.params.map(postParam => encode(postParam.name) + "=" + encode(unwrap(postParam.value))).mkString("&").getBytes(UTF_8))

        } else {
          None
        }
    }

  private def buildResponse(response: HarResponse, timestamp: Long): HttpResponse =
    HttpResponse(response.status, response.statusText, buildHeaders(response.headers), buildResponseBody(response.content).getOrElse(Array.empty), timestamp)

  private def buildResponseBody(content: HarResponseContent): Option[Array[Byte]] =
    for {
      text <- content.text.flatMap(_.trimToOption)
      if !content.mimeType.contains("x-unknown") // Chrome
      if content.comment.isEmpty // FireFox adds a localized "response body is not included" when there's no body, eg redirect.
    } yield {
      content.encoding.flatMap(_.trimToOption) match {
        case Some("base64") => Base64.getDecoder.decode(text)
        case _              => text.getBytes(UTF_8) // FIXME we should try using charset from Content-Type
      }
    }
}
