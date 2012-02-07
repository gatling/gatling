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

import com.excilys.ebi.gatling.core.check.CheckOneBuilder
import com.excilys.ebi.gatling.core.check.CheckMultipleBuilder
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.util.StringHelper.interpolate
import com.excilys.ebi.gatling.http.check.{ HttpMultipleCheckBuilder, HttpCheck }
import com.excilys.ebi.gatling.http.request.HttpPhase.HeadersReceived
import com.ning.http.client.Response
import com.excilys.ebi.gatling.core.check.extractor.Extractor.{ toOption, listToOption }

/**
 * HttpHeaderCheckBuilder class companion
 *
 * It contains DSL definitions
 */
object HttpHeaderCheckBuilder {

	/**
	 * Will check the value of the header in the session
	 *
	 * @param expression the function returning the name of the header
	 */
	def header(expression: Session => String) = new HttpHeaderCheckBuilder(expression)
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
 * @param expression the function returning the header name to be checked
 */
class HttpHeaderCheckBuilder(expression: Session => String) extends HttpMultipleCheckBuilder[String](expression, HeadersReceived) {

	def find: CheckOneBuilder[HttpCheck[String], Response, String] = find(0)

	def find(occurrence: Int) = new CheckOneBuilder(checkBuildFunction, (response: Response) => (expression: String) => {
			val headers = response.getHeaders(expression)
			if (headers.size > occurrence) {
				headers.get(occurrence)
			} else {
				None
			}
		}
	)

	def findAll = new CheckMultipleBuilder(checkBuildFunction, (response: Response) =>  (expression: String) => asScalaIterable(response.getHeaders(expression)).toList)

	def count = new CheckOneBuilder(checkBuildFunction, (response: Response) =>  (expression: String) => response.getHeaders(expression).size)
}

