/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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

import scala.collection.JavaConversions.asScalaBuffer

import com.ning.http.client.{ Request, Response => AHCResponse }

trait Response extends AHCResponse {

	def request: Request
	def ahcResponse: Option[AHCResponse]
	def checksums: Map[String, String]
	def executionStartDate: Long
	def requestSendingEndDate: Long
	def responseReceivingStartDate: Long
	def executionEndDate: Long
	def checksum(algorithm: String): Option[String]
	def reponseTimeInMillis: Long
	def latencyInMillis: Long
	def getHeadersSafe(name: String): Seq[String]
}

class GatlingResponse(
	val request: Request,
	val ahcResponse: Option[AHCResponse],
	val checksums: Map[String, String],
	val executionStartDate: Long,
	val requestSendingEndDate: Long,
	val responseReceivingStartDate: Long,
	val executionEndDate: Long) extends Response {

	def checksum(algorithm: String): Option[String] = checksums.get(algorithm)
	def reponseTimeInMillis: Long = executionEndDate - executionStartDate
	def latencyInMillis: Long = responseReceivingStartDate - requestSendingEndDate
	def getHeadersSafe(name: String) = Option(ahcResponse.getOrElse(throw new IllegalStateException("Response was not built")).getHeaders(name).toSeq).getOrElse(Nil)

	override def toString = ahcResponse.toString
	def getStatusCode = ahcResponse.getOrElse(throw new IllegalStateException("Response was not built")).getStatusCode
	def getStatusText = ahcResponse.getOrElse(throw new IllegalStateException("Response was not built")).getStatusText
	def getResponseBodyAsBytes = ahcResponse.getOrElse(throw new IllegalStateException("Response was not built")).getResponseBodyAsBytes
	def getResponseBodyAsStream = ahcResponse.getOrElse(throw new IllegalStateException("Response was not built")).getResponseBodyAsStream
	def getResponseBodyAsByteBuffer = ahcResponse.getOrElse(throw new IllegalStateException("Response was not built")).getResponseBodyAsByteBuffer
	def getResponseBodyExcerpt(maxLength: Int, charset: String) = ahcResponse.getOrElse(throw new IllegalStateException("Response was not built")).getResponseBodyExcerpt(maxLength, charset)
	def getResponseBody(charset: String) = ahcResponse.getOrElse(throw new IllegalStateException("Response was not built")).getResponseBody(charset)
	def getResponseBodyExcerpt(maxLength: Int) = ahcResponse.getOrElse(throw new IllegalStateException("Response was not built")).getResponseBodyExcerpt(maxLength)
	def getResponseBody = ahcResponse.getOrElse(throw new IllegalStateException("Response was not built")).getResponseBody
	def getUri = ahcResponse.getOrElse(throw new IllegalStateException("Response was not built")).getUri
	def getContentType = ahcResponse.getOrElse(throw new IllegalStateException("Response was not built")).getContentType
	def getHeader(name: String) = ahcResponse.getOrElse(throw new IllegalStateException("Response was not built")).getHeader(name)
	def getHeaders(name: String) = ahcResponse.getOrElse(throw new IllegalStateException("Response was not built")).getHeaders(name)
	def getHeaders = ahcResponse.getOrElse(throw new IllegalStateException("Response was not built")).getHeaders
	def isRedirected = ahcResponse.getOrElse(throw new IllegalStateException("Response was not built")).isRedirected
	def getCookies = ahcResponse.getOrElse(throw new IllegalStateException("Response was not built")).getCookies
	def hasResponseStatus = ahcResponse.getOrElse(throw new IllegalStateException("Response was not built")).hasResponseStatus
	def hasResponseHeaders = ahcResponse.getOrElse(throw new IllegalStateException("Response was not built")).hasResponseHeaders
	def hasResponseBody = ahcResponse.getOrElse(throw new IllegalStateException("Response was not built")).hasResponseBody
}

class DelegatingReponse(delegate: Response) extends Response {

	def request: Request = delegate.request
	def ahcResponse = delegate.ahcResponse
	def checksums = delegate.checksums
	def executionStartDate = delegate.executionStartDate
	def requestSendingEndDate = delegate.requestSendingEndDate
	def responseReceivingStartDate = delegate.responseReceivingStartDate
	def executionEndDate = delegate.responseReceivingStartDate
	def checksum(algorithm: String) = delegate.checksum(algorithm)
	def reponseTimeInMillis = delegate.reponseTimeInMillis
	def latencyInMillis = delegate.latencyInMillis
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