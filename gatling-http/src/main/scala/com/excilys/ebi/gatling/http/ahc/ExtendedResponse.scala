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
package com.excilys.ebi.gatling.http.ahc

import java.security.MessageDigest

import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.util.StringHelper.bytes2Hex
import com.excilys.ebi.gatling.http.check.HttpCheck
import com.excilys.ebi.gatling.http.request.HttpPhase.{ CompletePageReceived, BodyPartReceived }
import com.ning.http.client.Response.ResponseBuilder
import com.ning.http.client.{ Response, HttpResponseStatus, HttpResponseHeaders, HttpResponseBodyPart }

class ExtendedResponse(response: Response, checksums: Map[String, MessageDigest]) extends Response {

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

	override def toString = response.toString

	def getCookies = response.getCookies

	def hasResponseStatus = response.hasResponseStatus

	def hasResponseHeaders = response.hasResponseHeaders

	def hasResponseBody = response.hasResponseBody

	def checksum(algorithm: String): Option[String] = checksums.get(algorithm).map(md => bytes2Hex(md.digest))
}

class ExtendedResponseBuilder(session: Session, checks: List[HttpCheck]) {

	val responseBuilder = new ResponseBuilder
	val checksumChecks = checks.filter(_.phase == BodyPartReceived)
	val storeBodyParts = checks.exists(check => check.phase == CompletePageReceived)
	var checksums = Map.empty[String, MessageDigest]

	def accumulate(responseStatus: HttpResponseStatus) {
		responseBuilder.accumulate(responseStatus)
	}

	def accumulate(headers: HttpResponseHeaders) {
		responseBuilder.accumulate(headers)
	}

	def accumulate(bodyPart: Option[HttpResponseBodyPart]) {
		bodyPart.map { part =>

			for (check <- checksumChecks) {
				val algorithm = check.expression(session)
				checksums.getOrElse(algorithm, {
					val md = MessageDigest.getInstance(algorithm)
					checksums += (algorithm -> md)
					md
				}).update(part.getBodyPartBytes)
			}

			if (storeBodyParts)
				responseBuilder.accumulate(part)
		}
	}

	def build: Response = {
		val response = responseBuilder.build
		responseBuilder.reset
		if (checksumChecks.isEmpty) response else new ExtendedResponse(response, checksums)
	}
}