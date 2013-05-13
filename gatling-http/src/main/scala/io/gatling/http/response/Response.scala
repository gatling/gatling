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

import scala.collection.JavaConversions.asScalaBuffer

import com.ning.http.client.{ Request, Response => AHCResponse }

class Response(
	val request: Request,
	val ahcResponse: Option[AHCResponse],
	val checksums: Map[String, String],
	val executionStartDate: Long,
	val requestSendingEndDate: Long,
	val responseReceivingStartDate: Long,
	val executionEndDate: Long) {

	def checksum(algorithm: String) = checksums.get(algorithm)
	def reponseTimeInMillis = executionEndDate - executionStartDate
	def latencyInMillis = responseReceivingStartDate - requestSendingEndDate
	def isReceived = ahcResponse.isDefined
	def getHeadersSafe(name: String) = Option(ahcResponse.getOrElse(throw new IllegalStateException("Response was not built")).getHeaders(name).toSeq).getOrElse(Nil)

	override def toString = ahcResponse.toString
	def receivedResponse = ahcResponse.getOrElse(throw new IllegalStateException("Response was not built"))
	def getStatusCode = receivedResponse.getStatusCode
	def getStatusText = receivedResponse.getStatusText
	def getResponseBodyAsBytes = receivedResponse.getResponseBodyAsBytes
	def getResponseBodyAsStream = receivedResponse.getResponseBodyAsStream
	def getResponseBodyAsByteBuffer = receivedResponse.getResponseBodyAsByteBuffer
	def getResponseBodyExcerpt(maxLength: Int, charset: String) = receivedResponse.getResponseBodyExcerpt(maxLength, charset)
	def getResponseBody(charset: String) = receivedResponse.getResponseBody(charset)
	def getResponseBodyExcerpt(maxLength: Int) = receivedResponse.getResponseBodyExcerpt(maxLength)
	def getResponseBody = receivedResponse.getResponseBody
	def getUri = receivedResponse.getUri
	def getContentType = receivedResponse.getContentType
	def getHeader(name: String) = receivedResponse.getHeader(name)
	def getHeaders(name: String) = receivedResponse.getHeaders(name)
	def getHeaders = receivedResponse.getHeaders
	def isRedirected = receivedResponse.isRedirected
	def getCookies = receivedResponse.getCookies
	def hasResponseStatus = receivedResponse.hasResponseStatus
	def hasResponseHeaders = receivedResponse.hasResponseHeaders
	def hasResponseBody = receivedResponse.hasResponseBody
}
