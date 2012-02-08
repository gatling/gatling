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
import scala.annotation.implicitNotFound

import com.excilys.ebi.gatling.core.check.CheckOneBuilder
import com.excilys.ebi.gatling.core.check.CheckMultipleBuilder
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.http.check.{ HttpMultipleCheckBuilder, HttpCheck }
import com.excilys.ebi.gatling.http.request.HttpPhase.CompletePageReceived
import com.ning.http.client.Response

/**
 * This class builds a response body check based on regular expressions
 *
 * @param findExtractorFactory the extractor factory for find
 * @param findAllExtractoryFactory the extractor factory for findAll
 * @param countExtractoryFactory the extractor factory for count
 * @param expression the function returning the expression representing expression is to be checked
 */
class HttpBodyCheckBuilder(findExtractorFactory: Int => Response => String => Option[String],
		findAllExtractoryFactory: Response => String => Option[List[String]],
		countExtractoryFactory: Response => String => Option[Int],
		expression: Session => String) extends HttpMultipleCheckBuilder[String](expression, CompletePageReceived) {

	def find: CheckOneBuilder[HttpCheck[String], Response, String] = find(0)

	def find(occurrence: Int) = new CheckOneBuilder(checkBuildFunction, findExtractorFactory(occurrence))

	def findAll = new CheckMultipleBuilder(checkBuildFunction, findAllExtractoryFactory)

	def count = new CheckOneBuilder(checkBuildFunction, countExtractoryFactory)
}