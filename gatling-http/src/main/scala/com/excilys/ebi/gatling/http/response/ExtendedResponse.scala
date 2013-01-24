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
package com.excilys.ebi.gatling.http.response

import java.security.MessageDigest

import com.excilys.ebi.gatling.core.util.StringHelper.{ END_OF_LINE, bytes2Hex }
import com.excilys.ebi.gatling.http.util.HttpHelper.dumpFluentCaseInsensitiveStringsMap
import com.ning.http.client.{ Request, Response }

class ExtendedResponse(
	val request: Request,
	response: Option[Response],
	checksums: Map[String, MessageDigest],
	val executionStartDate: Long,
	val requestSendingEndDate: Long,
	val responseReceivingStartDate: Long,
	val executionEndDate: Long) extends Response {

	def isBuilt = response.isDefined

	def checksum(algorithm: String): Option[String] = checksums.get(algorithm).map(md => bytes2Hex(md.digest))

	def reponseTimeInMillis: Long = executionEndDate - executionStartDate

	def latencyInMillis: Long = responseReceivingStartDate - requestSendingEndDate

	def dumpTo(buff: StringBuilder) {
		response.map { response =>
			if (response.hasResponseStatus)
				buff.append("status=").append(END_OF_LINE).append(response.getStatusCode).append(" ").append(response.getStatusText).append(END_OF_LINE)

			if (response.hasResponseHeaders) {
				buff.append("headers= ").append(END_OF_LINE)
				dumpFluentCaseInsensitiveStringsMap(response.getHeaders, buff)
				buff.append(END_OF_LINE)
			}

			if (response.hasResponseBody)
				buff.append("body=").append(END_OF_LINE).append(response.getResponseBody)
		}
	}

	def dump: StringBuilder = {
		val buff = new StringBuilder
		dumpTo(buff)
		buff
	}

	override def toString = response.toString

	def getStatusCode = response.getOrElse(throw new IllegalStateException("Response was not built")).getStatusCode

	def getStatusText = response.getOrElse(throw new IllegalStateException("Response was not built")).getStatusText

	def getResponseBodyAsBytes = response.getOrElse(throw new IllegalStateException("Response was not built")).getResponseBodyAsBytes

	def getResponseBodyAsStream = response.getOrElse(throw new IllegalStateException("Response was not built")).getResponseBodyAsStream

	def getResponseBodyExcerpt(maxLength: Int, charset: String) = response.getOrElse(throw new IllegalStateException("Response was not built")).getResponseBodyExcerpt(maxLength, charset)

	def getResponseBody(charset: String) = response.getOrElse(throw new IllegalStateException("Response was not built")).getResponseBody(charset)

	def getResponseBodyExcerpt(maxLength: Int) = response.getOrElse(throw new IllegalStateException("Response was not built")).getResponseBodyExcerpt(maxLength)

	def getResponseBody = response.getOrElse(throw new IllegalStateException("Response was not built")).getResponseBody

	def getUri = response.getOrElse(throw new IllegalStateException("Response was not built")).getUri

	def getContentType = response.getOrElse(throw new IllegalStateException("Response was not built")).getContentType

	def getHeader(name: String) = response.getOrElse(throw new IllegalStateException("Response was not built")).getHeader(name)

	def getHeaders(name: String) = response.getOrElse(throw new IllegalStateException("Response was not built")).getHeaders(name)

	def getHeaders = response.getOrElse(throw new IllegalStateException("Response was not built")).getHeaders

	def isRedirected = response.getOrElse(throw new IllegalStateException("Response was not built")).isRedirected

	def getCookies = response.getOrElse(throw new IllegalStateException("Response was not built")).getCookies

	def hasResponseStatus = response.getOrElse(throw new IllegalStateException("Response was not built")).hasResponseStatus

	def hasResponseHeaders = response.getOrElse(throw new IllegalStateException("Response was not built")).hasResponseHeaders

	def hasResponseBody = response.getOrElse(throw new IllegalStateException("Response was not built")).hasResponseBody
}