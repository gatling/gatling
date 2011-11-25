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
import com.excilys.ebi.gatling.http.check.{ HttpCheckBuilder, HttpCheck }
import com.excilys.ebi.gatling.http.request.HttpPhase.{ HttpPhase, CompletePageReceived }
import com.excilys.ebi.gatling.core.check.CheckBuilderVerify
import com.excilys.ebi.gatling.core.check.CheckBuilderSave
import com.excilys.ebi.gatling.core.check.CheckBuilderFind
import com.excilys.ebi.gatling.core.check.CheckBuilderVerifyAll
import com.excilys.ebi.gatling.core.check.CheckBuilderVerifyOne

object HttpBodyRegExpCheckBuilder {
	/**
	 *
	 */
	def regexp(what: Context => String) = new HttpBodyRegExpCheckBuilder(what, Some(0), ExistenceCheckStrategy, Nil, None) with CheckBuilderFind[HttpCheckBuilder[HttpBodyRegExpCheckBuilder]]
	/**
	 *
	 */
	def regexp(expression: String): HttpBodyRegExpCheckBuilder with CheckBuilderFind[HttpCheckBuilder[HttpBodyRegExpCheckBuilder]] = regexp(interpolate(expression))
}

/**
 * This class builds a response body check based on regular expressions
 *
 * @param what the function returning the expression representing what is to be checked
 * @param strategy the strategy used to check
 * @param expected the expected value against which the extracted value will be checked
 * @param saveAs the optional context key in which the extracted value will be stored
 */
class HttpBodyRegExpCheckBuilder(what: Context => String, occurrence: Option[Int], strategy: CheckStrategy, expected: List[String], saveAs: Option[String])
		extends HttpCheckBuilder[HttpBodyRegExpCheckBuilder](what, occurrence, strategy, expected, saveAs, CompletePageReceived) {

	private[http] def newInstance(what: Context => String, occurrence: Option[Int], strategy: CheckStrategy, expected: List[String], saveAs: Option[String], when: HttpPhase) =
		new HttpBodyRegExpCheckBuilder(what, occurrence, strategy, expected, saveAs)

	private[gatling] def newInstanceWithFindOne(occurrence: Int) =
		new HttpBodyRegExpCheckBuilder(what, Some(occurrence), strategy, expected, saveAs) with CheckBuilderVerifyOne[HttpCheckBuilder[HttpBodyRegExpCheckBuilder]]

	private[gatling] def newInstanceWithFindAll =
		new HttpBodyRegExpCheckBuilder(what, None, strategy, expected, saveAs) with CheckBuilderVerifyAll[HttpCheckBuilder[HttpBodyRegExpCheckBuilder]]

	private[gatling] def newInstanceWithVerify(strategy: CheckStrategy, expected: List[String] = Nil) =
		new HttpBodyRegExpCheckBuilder(what, occurrence, strategy, expected, saveAs) with CheckBuilderSave[HttpCheckBuilder[HttpBodyRegExpCheckBuilder]]

	private[gatling] def build: HttpCheck = new HttpBodyRegExpCheck(what, occurrence, strategy, expected, saveAs: Option[String])
}
