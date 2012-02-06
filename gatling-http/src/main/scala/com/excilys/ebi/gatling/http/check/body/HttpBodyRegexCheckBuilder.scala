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
import com.excilys.ebi.gatling.core.check.extractor.ExtractorFactory
import com.excilys.ebi.gatling.core.check.extractor.{ RegexExtractor, MultiRegexExtractor }
import com.excilys.ebi.gatling.core.check.CheckOneBuilder
import com.excilys.ebi.gatling.core.check.CheckMultipleBuilder
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.util.StringHelper.interpolate
import com.excilys.ebi.gatling.http.check.{ HttpMultipleCheckBuilder, HttpCheck }
import com.excilys.ebi.gatling.http.request.HttpPhase.CompletePageReceived
import com.ning.http.client.Response
import HttpBodyRegexCheckBuilder.HTTP_RESPONSE_BODY_CHECK_CONTEXT_KEY
import com.excilys.ebi.gatling.core.check.extractor.TransformerExtractor

object HttpBodyRegexCheckBuilder {

	val HTTP_RESPONSE_BODY_CHECK_CONTEXT_KEY = "httpResponseBody"

	def regex(what: Session => String) = new HttpBodyRegexCheckBuilder(what)

	def regex(expression: String): HttpBodyRegexCheckBuilder = regex(interpolate(expression))
}

/**
 * This class builds a response body check based on regular expressions
 *
 * @param what the function returning the expression representing what is to be checked
 * @param strategy the strategy used to check
 * @param expected the expected value against which the extracted value will be checked
 * @param saveAs the optional session key in which the extracted value will be stored
 */
class HttpBodyRegexCheckBuilder(what: Session => String) extends HttpMultipleCheckBuilder[String](what, CompletePageReceived) {

	def find: CheckOneBuilder[HttpCheck[String], Response, String] = find(0)

	def getResponseBody(response: Response) = getCheckContextAttribute(HTTP_RESPONSE_BODY_CHECK_CONTEXT_KEY).getOrElse {
		setAndReturnCheckContextAttribute(HTTP_RESPONSE_BODY_CHECK_CONTEXT_KEY, response.getResponseBody)
	}
	
	def find(occurence: Int) = new CheckOneBuilder(checkBuildFunction[String], new ExtractorFactory[Response, String] {
		def getExtractor(response: Response) = new RegexExtractor(getResponseBody(response), occurence)
	})

	def findAll = new CheckMultipleBuilder(checkBuildFunction[List[String]], new ExtractorFactory[Response, List[String]] {
		def getExtractor(response: Response) = new MultiRegexExtractor(getResponseBody(response))
	})

	def count = new CheckOneBuilder(checkBuildFunction[Int], new ExtractorFactory[Response, Int] {
		def getExtractor(response: Response) = new TransformerExtractor(new MultiRegexExtractor(getResponseBody(response)), (list: List[_]) => list.size)
	})
}
