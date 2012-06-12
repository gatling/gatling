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
package com.excilys.ebi.gatling.http.check.bodypart

import com.excilys.ebi.gatling.core.check.ExtractorFactory
import com.excilys.ebi.gatling.core.check.MatcherCheckBuilder
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY
import com.excilys.ebi.gatling.http.check.{ HttpExtractorCheckBuilder, HttpCheck }
import com.excilys.ebi.gatling.http.request.HttpPhase.BodyPartReceived
import com.ning.http.client.Response
import com.excilys.ebi.gatling.http.check.bodypart.HttpBodyPartCheckBuilder.findExtractorFactory
import com.excilys.ebi.gatling.core.session.EvaluatableString
import scala.collection.mutable
import java.security.MessageDigest
import com.excilys.ebi.gatling.core.util.StringHelper.bytes2Hex
import com.excilys.ebi.gatling.core.session.Session

/**
 * HttpBodyPartCheckBuilder class companion
 *
 * It contains DSL definitions
 */
object HttpBodyPartCheckBuilder {

	def checksum(algorythm: String) = new HttpBodyPartCheckBuilder(algorythm)

	private def findExtractorFactory: ExtractorFactory[Response, String] = (response: Response) =>
		(expression: String) =>
			response
				.asInstanceOf[ResponseWithChecksums]
				.checksums.get(expression)
				.map(md => bytes2Hex(md.digest))
}

/**
 * This class builds a body part check
 */
class HttpBodyPartCheckBuilder(algorythm: String) extends HttpExtractorCheckBuilder[String]((session: Session) => algorythm, BodyPartReceived) {

	def find = new MatcherCheckBuilder[HttpCheck, Response, String](httpCheckBuilderFactory, findExtractorFactory)
}

class ResponseWithChecksums(response: Response, val checksums: mutable.Map[String, MessageDigest]) extends Response {

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
}
