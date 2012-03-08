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
import scala.collection.JavaConverters.asScalaBufferConverter

import com.excilys.ebi.gatling.core.check.extractor.Extractor.{ toOption, seqToOption }
import com.excilys.ebi.gatling.core.check.ExtractorFactory
import com.excilys.ebi.gatling.core.check.{ MultipleExtractorCheckBuilder, MatcherCheckBuilder }
import com.excilys.ebi.gatling.core.session.EvaluatableString
import com.excilys.ebi.gatling.http.check.header.HttpHeaderCheckBuilder.{ findExtractorFactory, findAllExtractorFactory, countExtractorFactory }
import com.excilys.ebi.gatling.http.check.{ HttpExtractorCheckBuilder, HttpCheck }
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
	 * @param expression the function returning the name of the header
	 */
	def header(expression: EvaluatableString) = new HttpHeaderCheckBuilder(expression)

	private def findExtractorFactory(occurrence: Int): ExtractorFactory[Response, String] = (response: Response) => (expression: String) => {
		val headers = response.getHeaders(expression)
		if (headers.size > occurrence)
			toOption(headers.get(occurrence))
		else
			None
	}

	private val findAllExtractorFactory: ExtractorFactory[Response, Seq[String]] = (response: Response) => (expression: String) => seqToOption(response.getHeaders(expression).asScala)

	private val countExtractorFactory: ExtractorFactory[Response, Int] = (response: Response) => (expression: String) => toOption(response.getHeaders(expression).size)
}

/**
 * This class builds a response header check
 *
 * @param expression the function returning the header name to be checked
 */
class HttpHeaderCheckBuilder(expression: EvaluatableString) extends HttpExtractorCheckBuilder[String](expression, HeadersReceived) with MultipleExtractorCheckBuilder[HttpCheck, Response, String] {

	def find: MatcherCheckBuilder[HttpCheck, Response, String] = find(0)

	def find(occurrence: Int) = new MatcherCheckBuilder(httpCheckBuilderFactory, findExtractorFactory(occurrence))

	def findAll = new MatcherCheckBuilder(httpCheckBuilderFactory, findAllExtractorFactory)

	def count = new MatcherCheckBuilder(httpCheckBuilderFactory, countExtractorFactory)
}

