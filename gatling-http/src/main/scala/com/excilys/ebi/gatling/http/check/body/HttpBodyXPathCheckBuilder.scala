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

import com.excilys.ebi.gatling.core.util.StringHelper.interpolate
import com.excilys.ebi.gatling.core.check.strategy.{ NonExistenceCheckStrategy, NonEqualityCheckStrategy, ExistenceCheckStrategy, EqualityCheckStrategy, CheckStrategy }
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY
import com.excilys.ebi.gatling.http.check.HttpCheckBuilder
import com.excilys.ebi.gatling.http.request.HttpPhase.{ HttpPhase, CompletePageReceived }
import com.excilys.ebi.gatling.core.check.CheckBuilderSave
import com.excilys.ebi.gatling.core.check.CheckBuilderVerify
import com.excilys.ebi.gatling.core.check.CheckBuilderFind
import com.excilys.ebi.gatling.core.check.CheckBuilderVerifyAll
import com.excilys.ebi.gatling.core.check.CheckBuilderVerifyOne

object HttpBodyXPathCheckBuilder {
	/**
	 *
	 */
	def xpath(what: Context => String) = new HttpBodyXPathCheckBuilder(what, Some(0), ExistenceCheckStrategy, Nil, None) with CheckBuilderFind[HttpCheckBuilder[HttpBodyXPathCheckBuilder]]
	/**
	 *
	 */
	def xpath(expression: String): HttpBodyXPathCheckBuilder with CheckBuilderFind[HttpCheckBuilder[HttpBodyXPathCheckBuilder]] = xpath(interpolate(expression))
}

/**
 * This class builds a response body check based on XPath expressions
 *
 * @param what the function returning the expression representing what is to be checked
 * @param to the optional context key in which the extracted value will be stored
 * @param strategy the strategy used to check
 * @param expected the expected value against which the extracted value will be checked
 */
class HttpBodyXPathCheckBuilder(what: Context => String, occurrence: Option[Int], strategy: CheckStrategy, expected: List[String], saveAs: Option[String])
		extends HttpCheckBuilder[HttpBodyXPathCheckBuilder](what, occurrence, strategy, expected, saveAs, CompletePageReceived) {

	private[http] def newInstance(what: Context => String, occurrence: Option[Int], strategy: CheckStrategy, expected: List[String], saveAs: Option[String], when: HttpPhase) =
		new HttpBodyXPathCheckBuilder(what, occurrence, strategy, expected, saveAs)

	private[gatling] def newInstanceWithFindOne(occurrence: Int) =
		new HttpBodyXPathCheckBuilder(what, Some(occurrence), strategy, expected, saveAs) with CheckBuilderVerifyOne[HttpCheckBuilder[HttpBodyXPathCheckBuilder]]

	private[gatling] def newInstanceWithFindAll =
		new HttpBodyXPathCheckBuilder(what, None, strategy, expected, saveAs) with CheckBuilderVerifyAll[HttpCheckBuilder[HttpBodyXPathCheckBuilder]]

	private[gatling] def newInstanceWithVerify(strategy: CheckStrategy, expected: List[String] = Nil) =
		new HttpBodyXPathCheckBuilder(what, occurrence, strategy, expected, saveAs) with CheckBuilderSave[HttpCheckBuilder[HttpBodyXPathCheckBuilder]]

	private[gatling] def build = new HttpBodyXPathCheck(what, occurrence, strategy, expected, saveAs)
}
