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
package io.gatling.http.response

import java.io.ByteArrayInputStream
import java.nio.ByteBuffer

import scala.collection.JavaConversions.asScalaBuffer

import io.gatling.core.config.GatlingConfiguration.configuration
import com.ning.http.client.{ Request, Response => AHCResponse }

trait Response extends AHCResponse {

	def request: Request
	def ahcResponse: Option[AHCResponse]
	def checksums: Map[String, String]
	def firstByteSent: Long
	def lastByteSent: Long
	def firstByteReceived: Long
	def lastByteReceived: Long
	def checksum(algorithm: String): Option[String]
	def reponseTimeInMillis: Long
	def latencyInMillis: Long
	def isReceived: Boolean
	def getHeaderSafe(name: String): Option[String]
	def getHeadersSafe(name: String): Seq[String]
}

case class HttpResponse(
	request: Request,
	ahcResponse: Option[AHCResponse],
	checksums: Map[String, String],
	firstByteSent: Long,
	lastByteSent: Long,
	firstByteReceived: Long,
	lastByteReceived: Long,
	bytesOrString: Either[Array[Byte], String]) extends Response {

	def checksum(algorithm: String) = checksums.get(algorithm)
	def reponseTimeInMillis = lastByteReceived - firstByteSent
	def latencyInMillis = firstByteReceived - firstByteReceived
	def isReceived = ahcResponse.isDefined
	def getHeaderSafe(name: String): Option[String] = ahcResponse.flatMap(r => Option(r.getHeader(name)))
	def getHeadersSafe(name: String): Seq[String] = ahcResponse.flatMap(r => Option(r.getHeaders(name))).map(_.toSeq).getOrElse(Nil)

	override def toString = ahcResponse.toString

	private def receivedResponse = ahcResponse.getOrElse(throw new IllegalStateException("Response was not built"))
	def getStatusCode = receivedResponse.getStatusCode
	def getStatusText = receivedResponse.getStatusText
	def getResponseBodyAsBytes = bytesOrString match {
		case Left(bytes) => bytes
		case Right(string) => string.getBytes(configuration.core.encoding)
	}
	def getResponseBodyAsStream = throw new UnsupportedOperationException
	def getResponseBodyAsByteBuffer = throw new UnsupportedOperationException
	def getResponseBodyExcerpt(maxLength: Int, charset: String) = throw new UnsupportedOperationException
	def getResponseBody(charset: String) = bytesOrString match {
		case Left(bytes) => new String(bytes, charset)
		case Right(string) => string
	}
	def getResponseBodyExcerpt(maxLength: Int) = throw new UnsupportedOperationException
	def getResponseBody = getResponseBody(configuration.core.encoding)
	def getUri = receivedResponse.getUri
	def getContentType = receivedResponse.getContentType
	def getHeader(name: String) = receivedResponse.getHeader(name)
	def getHeaders(name: String) = receivedResponse.getHeaders(name)
	def getHeaders = receivedResponse.getHeaders
	def isRedirected = receivedResponse.isRedirected
	def getCookies = receivedResponse.getCookies
	def hasResponseStatus = receivedResponse.hasResponseStatus
	def hasResponseHeaders = receivedResponse.hasResponseHeaders
	def hasResponseBody = bytesOrString match {
		case Left(bytes) => !bytes.isEmpty
		case Right(string) => !string.isEmpty
	}
}

class DelegatingReponse(delegate: Response) extends Response {

	def request: Request = delegate.request
	def ahcResponse = delegate.ahcResponse
	def checksums = delegate.checksums
	def firstByteSent = delegate.firstByteSent
	def lastByteSent = delegate.lastByteSent
	def firstByteReceived = delegate.firstByteReceived
	def lastByteReceived = delegate.lastByteReceived
	def checksum(algorithm: String) = delegate.checksum(algorithm)
	def reponseTimeInMillis = delegate.reponseTimeInMillis
	def latencyInMillis = delegate.latencyInMillis
	def isReceived = delegate.isReceived
	def getHeaderSafe(name: String) = delegate.getHeaderSafe(name)
	def getHeadersSafe(name: String) = delegate.getHeadersSafe(name)

	def getStatusCode = delegate.getStatusCode
	def getStatusText = delegate.getStatusText
	def getResponseBodyAsBytes = delegate.getResponseBodyAsBytes
	def getResponseBodyAsStream = delegate.getResponseBodyAsStream
	def getResponseBodyAsByteBuffer = delegate.getResponseBodyAsByteBuffer
	def getResponseBodyExcerpt(maxLength: Int, charset: String) = delegate.getResponseBodyExcerpt(maxLength, charset)
	def getResponseBody(charset: String) = delegate.getResponseBody(charset)
	def getResponseBodyExcerpt(maxLength: Int) = delegate.getResponseBodyExcerpt(maxLength)
	def getResponseBody = delegate.getResponseBody
	def getUri = delegate.getUri
	def getContentType = delegate.getContentType
	def getHeader(name: String) = delegate.getHeader(name)
	def getHeaders(name: String) = delegate.getHeaders(name)
	def getHeaders = delegate.getHeaders
	def isRedirected = delegate.isRedirected
	def getCookies = delegate.getCookies
	def hasResponseStatus = delegate.hasResponseStatus
	def hasResponseHeaders = delegate.hasResponseHeaders
	def hasResponseBody = delegate.hasResponseHeaders
}