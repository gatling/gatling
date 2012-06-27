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

import com.excilys.ebi.gatling.core.check.{ MultipleExtractorCheckBuilder, MatcherCheckBuilder, ExtractorFactory }
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.http.check.{ HttpExtractorCheckBuilder, HttpCheck }
import com.excilys.ebi.gatling.http.request.HttpPhase.CompletePageReceived
import com.excilys.ebi.gatling.http.response.ExtendedResponse

/**
 * This class builds a response body check based on regular expressions
 *
 * @param findExtractorFactory the extractor factory for find
 * @param findAllExtractorFactory the extractor factory for findAll
 * @param countExtractorFactory the extractor factory for count
 * @param expression the function returning the expression representing expression is to be checked
 */
class HttpBodyCheckBuilder[XC](
	findExtractorFactory: Int => ExtractorFactory[ExtendedResponse, XC, String],
	findAllExtractorFactory: ExtractorFactory[ExtendedResponse, XC, Seq[String]],
	countExtractorFactory: ExtractorFactory[ExtendedResponse, XC, Int],
	expression: Session => XC)
		extends HttpExtractorCheckBuilder[String, XC](expression, CompletePageReceived)
		with MultipleExtractorCheckBuilder[HttpCheck[XC], ExtendedResponse, XC, String] {

	def find: MatcherCheckBuilder[HttpCheck[XC], ExtendedResponse, XC, String] = find(0)

	def find(occurrence: Int) = new MatcherCheckBuilder(httpCheckBuilderFactory, findExtractorFactory(occurrence))

	def findAll = new MatcherCheckBuilder(httpCheckBuilderFactory, findAllExtractorFactory)

	def count = new MatcherCheckBuilder(httpCheckBuilderFactory, countExtractorFactory)
}