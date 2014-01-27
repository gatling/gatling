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

import scala.collection.JavaConversions.asScalaBuffer

import com.ning.http.client.{ Cookie, FluentCaseInsensitiveStringsMap, Request => AHCRequest, Response => AHCResponse }

import io.gatling.core.config.GatlingConfiguration.configuration

trait Response {

	def request: AHCRequest
	def ahcResponse: Option[AHCResponse]
	def isReceived: Boolean

	def firstByteSent: Long
	def lastByteSent: Long
	def firstByteReceived: Long
	def lastByteReceived: Long
	def reponseTimeInMillis: Long
	def latencyInMillis: Long

	def statusCode: Int
	def statusText: String
	def isRedirected: Boolean
	def uri: URI

	def header(name: String): String
	def headers: FluentCaseInsensitiveStringsMap
	def headers(name: String): Seq[String]
	def headerSafe(name: String): Option[String]
	def headersSafe(name: String): Seq[String]
	def contentType: String
	def cookies: Seq[Cookie]

	def checksums: Map[String, String]
	def checksum(algorithm: String): Option[String]
	def hasResponseBody: Boolean
	def bodyString: String
	def bodyBytes: Array[Byte]
}

case class HttpResponse(
	request: AHCRequest,
	ahcResponse: Option[AHCResponse],
	firstByteSent: Long,
	lastByteSent: Long,
	firstByteReceived: Long,
	lastByteReceived: Long,
	checksums: Map[String, String],
	bodyBytes: Array[Byte]) extends Response {

	def isReceived = ahcResponse.isDefined

	def reponseTimeInMillis = lastByteReceived - firstByteSent
	def latencyInMillis = firstByteReceived - firstByteReceived

	def statusCode = receivedResponse.getStatusCode
	def statusText = receivedResponse.getStatusText
	def isRedirected = ahcResponse.map(_.isRedirected).getOrElse(false)
	def uri = receivedResponse.getUri

	def header(name: String) = receivedResponse.getHeader(name)
	def headers = receivedResponse.getHeaders
	def headers(name: String) = receivedResponse.getHeaders(name)
	def headerSafe(name: String): Option[String] = ahcResponse.flatMap(r => Option(r.getHeader(name)))
	def headersSafe(name: String): Seq[String] = ahcResponse.flatMap(r => Option(r.getHeaders(name))).map(_.toSeq).getOrElse(Nil)
	def contentType = receivedResponse.getContentType
	def cookies = receivedResponse.getCookies

	def checksum(algorithm: String) = checksums.get(algorithm)
	def hasResponseBody = bodyBytes.length != 0
	lazy val bodyString = new String(bodyBytes, configuration.core.charSet)

	private def receivedResponse = ahcResponse.getOrElse(throw new IllegalStateException("Response was not built"))
	override def toString = ahcResponse.toString
}

class DelegatingReponse(delegate: Response) extends Response {

	def request: AHCRequest = delegate.request
	def ahcResponse = delegate.ahcResponse
	def isReceived = delegate.isReceived

	def firstByteSent = delegate.firstByteSent
	def lastByteSent = delegate.lastByteSent
	def firstByteReceived = delegate.firstByteReceived
	def lastByteReceived = delegate.lastByteReceived
	def reponseTimeInMillis = delegate.reponseTimeInMillis
	def latencyInMillis = delegate.latencyInMillis

	def statusCode = delegate.statusCode
	def statusText = delegate.statusText
	def isRedirected = delegate.isRedirected
	def uri = delegate.uri

	def header(name: String) = delegate.header(name)
	def headers = delegate.headers
	def headers(name: String) = delegate.headers(name)
	def headerSafe(name: String) = delegate.headerSafe(name)
	def headersSafe(name: String) = delegate.headersSafe(name)
	def contentType = delegate.contentType
	def cookies = delegate.cookies

	def checksums = delegate.checksums
	def checksum(algorithm: String) = delegate.checksum(algorithm)
	def hasResponseBody = delegate.hasResponseBody
	def bodyBytes = delegate.bodyBytes
	def bodyString = delegate.bodyString
}
