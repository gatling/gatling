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
package com.excilys.ebi.gatling.http.check.body

import com.excilys.ebi.gatling.core.check.CheckContext.{ setAndReturnCheckContextAttribute, getCheckContextAttribute }
import com.excilys.ebi.gatling.core.check.extractor.RegexExtractor
import com.excilys.ebi.gatling.core.check.CheckOneBuilder
import com.excilys.ebi.gatling.core.check.CheckMultipleBuilder
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.util.StringHelper.interpolate
import com.excilys.ebi.gatling.http.check.{ HttpMultipleCheckBuilder, HttpCheck }
import com.excilys.ebi.gatling.http.request.HttpPhase.CompletePageReceived
import com.ning.http.client.Response

import HttpBodyRegexCheckBuilder.HTTP_BODY_REGEX_EXTRACTOR_CONTEXT_KEY

object HttpBodyRegexCheckBuilder {

	val HTTP_BODY_REGEX_EXTRACTOR_CONTEXT_KEY = "HttpBodyRegexExtractor"

	def regex(expression: Session => String) = new HttpBodyRegexCheckBuilder(expression)

	def regex(expression: String): HttpBodyRegexCheckBuilder = regex(interpolate(expression))
}

/**
 * This class builds a response body check based on regular expressions
 *
 * @param expression the function returning the expression representing expression is to be checked
 */
class HttpBodyRegexCheckBuilder(expression: Session => String) extends HttpMultipleCheckBuilder[String](expression, CompletePageReceived) {

	def find: CheckOneBuilder[HttpCheck[String], Response, String] = find(0)

	private def getCachedExtractor(response: Response) = getCheckContextAttribute(HTTP_BODY_REGEX_EXTRACTOR_CONTEXT_KEY).getOrElse {
		setAndReturnCheckContextAttribute(HTTP_BODY_REGEX_EXTRACTOR_CONTEXT_KEY, new RegexExtractor(response.getResponseBody))
	}

	def find(occurrence: Int) = new CheckOneBuilder(checkBuildFunction, (response: Response) => getCachedExtractor(response).extractOne(occurrence))

	def findAll = new CheckMultipleBuilder(checkBuildFunction, (response: Response) => getCachedExtractor(response).extractMultiple)

	def count = new CheckOneBuilder(checkBuildFunction, (response: Response) => getCachedExtractor(response).count)
}
