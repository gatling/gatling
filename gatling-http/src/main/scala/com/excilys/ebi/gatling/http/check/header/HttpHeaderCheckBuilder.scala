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
package com.excilys.ebi.gatling.http.check.header
import scala.collection.JavaConversions.asScalaIterable

import com.excilys.ebi.gatling.core.check.extractor.ExtractorFactory
import com.excilys.ebi.gatling.core.check.extractor.Extractor
import com.excilys.ebi.gatling.core.check.{ CheckOneBuilder, CheckMultipleBuilder }
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.util.StringHelper.interpolate
import com.excilys.ebi.gatling.http.check.{ HttpMultipleCheckBuilder, HttpCheck }
import com.excilys.ebi.gatling.http.request.HttpPhase.HeadersReceived
import com.ning.http.client.Response

/**
 * HttpHeaderCheckBuilder class companion
 *
 * It contains DSL definitions
 */
object HttpHeaderCheckBuilder {

	/**
	 * Will check the value of the header in the session
	 *
	 * @param what the function returning the name of the header
	 */
	def header(what: Session => String) = new HttpHeaderCheckBuilder(what)
	/**
	 * Will check the value of the header in the session
	 *
	 * @param headerName the name of the header
	 */
	def header(headerName: String): HttpHeaderCheckBuilder = header(interpolate(headerName))
}

/**
 * This class builds a response header check
 *
 * @param what the function returning the header name to be checked
 * @param to the optional session key in which the extracted value will be stored
 * @param strategy the strategy used to check
 * @param expected the expected value against which the extracted value will be checked
 */
class HttpHeaderCheckBuilder(what: Session => String) extends HttpMultipleCheckBuilder[String](what, HeadersReceived) {

	def find: CheckOneBuilder[HttpCheck[String], Response, String] = find(0)

	def find(occurrence: Int) = new CheckOneBuilder(checkBuildFunction[String], new ExtractorFactory[Response, String] {
		def getExtractor(response: Response) = new Extractor[String] {
			def extract(expression: String) = {
				val headers = response.getHeaders(expression)
				if (headers.size > occurrence) {
					headers.get(occurrence)
				} else {
					None
				}
			}
		}
	})

	def findAll = new CheckMultipleBuilder(checkBuildFunction[List[String]], new ExtractorFactory[Response, List[String]] {
		def getExtractor(response: Response) = new Extractor[List[String]] {
			def extract(expression: String) = asScalaIterable(response.getHeaders(expression)).toList
		}
	})
}

