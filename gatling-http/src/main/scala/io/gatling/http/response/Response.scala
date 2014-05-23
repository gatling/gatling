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
package io.gatling.http.response

import java.net.URI
import java.nio.charset.Charset

import scala.collection.JavaConversions.{ asScalaBuffer, asScalaSet }
import scala.collection.mutable.ArrayBuffer

import com.ning.http.client.{ FluentCaseInsensitiveStringsMap, HttpResponseStatus, Request => AHCRequest }
import com.ning.http.client.cookie.{ Cookie, CookieDecoder }

import io.gatling.http.HeaderNames
import io.gatling.http.util.HttpHelper

trait Response {

  def request: AHCRequest
  def isReceived: Boolean

  def status: Option[HttpResponseStatus]
  def statusCode: Option[Int]
  def uri: Option[URI]
  def isRedirect: Boolean

  def header(name: String): Option[String]
  def headers: FluentCaseInsensitiveStringsMap
  def headers(name: String): Seq[String]
  def cookies: List[Cookie]

  def checksums: Map[String, String]
  def checksum(algorithm: String): Option[String]
  def hasResponseBody: Boolean
  def body: ResponseBody
  def bodyLength: Int
  def charset: Charset

  def firstByteSent: Long
  def lastByteSent: Long
  def firstByteReceived: Long
  def lastByteReceived: Long
  def reponseTimeInMillis: Long
  def latencyInMillis: Long
}

case class HttpResponse(
    request: AHCRequest,
    status: Option[HttpResponseStatus],
    headers: FluentCaseInsensitiveStringsMap,
    body: ResponseBody,
    checksums: Map[String, String],
    bodyLength: Int,
    charset: Charset,
    firstByteSent: Long,
    lastByteSent: Long,
    firstByteReceived: Long,
    lastByteReceived: Long) extends Response {

  def isReceived = status.isDefined
  val statusCode = status.map(_.getStatusCode)

  def reponseTimeInMillis = lastByteReceived - firstByteSent
  def latencyInMillis = firstByteReceived - lastByteSent

  val isRedirect = status match {
    case Some(s) => HttpHelper.isRedirect(s.getStatusCode)
    case _       => false
  }
  def uri = status.map(_.getUrl)

  def header(name: String): Option[String] = Option(headers.getFirstValue(name))
  def headers(name: String): Seq[String] = Option(headers.get(name)) match {
    case Some(h) => h.toSeq
    case _       => Nil
  }
  lazy val cookies =
    if (headers.isEmpty) {
      Nil
    } else {
      val buffer = new ArrayBuffer[Cookie]

      headers.entrySet.foreach { entry =>
        if (entry.getKey.equalsIgnoreCase(HeaderNames.SetCookie))
          entry.getValue.foreach { string =>
            buffer += CookieDecoder.decode(string)
          }
      }

      buffer.toList
    }

  def checksum(algorithm: String) = checksums.get(algorithm)
  def hasResponseBody = bodyLength != 0
}

class ReponseWrapper(delegate: Response) extends Response {

  def request: AHCRequest = delegate.request
  def isReceived = delegate.isReceived

  def status = delegate.status
  def statusCode = delegate.statusCode
  def isRedirect = delegate.isRedirect
  def uri = delegate.uri

  def header(name: String) = delegate.header(name)
  def headers = delegate.headers
  def headers(name: String) = delegate.headers(name)
  def cookies = delegate.cookies

  def checksums = delegate.checksums
  def checksum(algorithm: String) = delegate.checksum(algorithm)
  def hasResponseBody = delegate.hasResponseBody
  def body = delegate.body
  def bodyLength = delegate.bodyLength
  def charset = delegate.charset

  def firstByteSent = delegate.firstByteSent
  def lastByteSent = delegate.lastByteSent
  def firstByteReceived = delegate.firstByteReceived
  def lastByteReceived = delegate.lastByteReceived
  def reponseTimeInMillis = delegate.reponseTimeInMillis
  def latencyInMillis = delegate.latencyInMillis
}
