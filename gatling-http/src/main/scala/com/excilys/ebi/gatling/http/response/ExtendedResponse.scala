/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.http.response

import java.security.MessageDigest

import com.excilys.ebi.gatling.core.util.StringHelper.{ bytes2Hex, END_OF_LINE }
import com.ning.http.client.Response

class ExtendedResponse(
		response: Response,
		checksums: Map[String, MessageDigest],
		executionStartDate: Long,
		requestSendingEndDate: Long,
		responseReceivingStartDate: Long,
		executionEndDate: Long) extends Response {

	def checksum(algorithm: String): Option[String] = checksums.get(algorithm).map(md => bytes2Hex(md.digest))

	def reponseTimeInMillis: Long = executionEndDate - executionStartDate

	def latencyInMillis: Long = responseReceivingStartDate - requestSendingEndDate

	def dump: StringBuilder = {
		val buff = new StringBuilder().append(END_OF_LINE)
		if (response.hasResponseStatus)
			buff.append("status=").append(END_OF_LINE).append(response.getStatusCode()).append(" ").append(response.getStatusText()).append(END_OF_LINE)

		if (response.hasResponseHeaders)
			buff.append("headers= ").append(END_OF_LINE).append(response.getHeaders()).append(END_OF_LINE)

		if (response.hasResponseBody)
			buff.append("body=").append(END_OF_LINE).append(response.getResponseBody())

		buff
	}
	
	override def toString = response.toString

	def getStatusCode = response.getStatusCode

	def getStatusText = response.getStatusText

	def getResponseBodyAsBytes = response.getResponseBodyAsBytes

	def getResponseBodyAsStream = response.getResponseBodyAsStream

	def getResponseBodyExcerpt(maxLength: Int, charset: String) = response.getResponseBodyExcerpt(maxLength, charset)

	def getResponseBody(charset: String) = response.getResponseBody(charset)

	def getResponseBodyExcerpt(maxLength: Int) = response.getResponseBodyExcerpt(maxLength)

	def getResponseBody = response.getResponseBody

	def getUri = response.getUri

	def getContentType = response.getContentType

	def getHeader(name: String) = response.getHeader(name)

	def getHeaders(name: String) = response.getHeaders(name)

	def getHeaders = response.getHeaders

	def isRedirected = response.isRedirected

	def getCookies = response.getCookies

	def hasResponseStatus = response.hasResponseStatus

	def hasResponseHeaders = response.hasResponseHeaders

	def hasResponseBody = response.hasResponseBody
}