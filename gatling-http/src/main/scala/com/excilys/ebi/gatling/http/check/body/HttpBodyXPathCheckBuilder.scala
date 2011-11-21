/**
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.http.check.body

import scala.annotation.implicitNotFound

import com.excilys.ebi.gatling.core.check.strategy.{ NonExistenceCheckStrategy, NonEqualityCheckStrategy, ExistenceCheckStrategy, EqualityCheckStrategy, CheckStrategy }
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY
import com.excilys.ebi.gatling.http.check.HttpCheckBuilder
import com.excilys.ebi.gatling.http.request.HttpPhase.{ HttpPhase, CompletePageReceived }

object HttpBodyXPathCheckBuilder {
	/**
	 *
	 */
	def xpath(what: Context => String) = new HttpBodyXPathCheckBuilder(what, None, ExistenceCheckStrategy, None, None)
	/**
	 *
	 */
	def xpath(expression: String): HttpBodyXPathCheckBuilder = xpath((c: Context) => expression)
}

/**
 * This class builds a response body check based on XPath expressions
 *
 * @param what the function returning the expression representing what is to be checked
 * @param to the optional context key in which the extracted value will be stored
 * @param strategy the strategy used to check
 * @param expected the expected value against which the extracted value will be checked
 */
class HttpBodyXPathCheckBuilder(what: Context => String, occurrence: Option[Int], strategy: CheckStrategy, expected: Option[String], saveAs: Option[String])
		extends HttpCheckBuilder[HttpBodyXPathCheckBuilder](what, occurrence, strategy, expected, saveAs, CompletePageReceived) {

	def newInstance(what: Context => String, occurrence: Option[Int], strategy: CheckStrategy, expected: Option[String], saveAs: Option[String], when: HttpPhase) =
		new HttpBodyXPathCheckBuilder(what, occurrence, strategy, expected, saveAs)

	def build = new HttpBodyXPathCheck(what, occurrence.getOrElse(0), strategy, expected, saveAs)
}
