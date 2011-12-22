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

import com.excilys.ebi.gatling.core.util.StringHelper.interpolate
import com.excilys.ebi.gatling.core.check.strategy.{ NonExistenceCheckStrategy, NonEqualityCheckStrategy, ExistenceCheckStrategy, EqualityCheckStrategy, CheckStrategy }
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY
import com.excilys.ebi.gatling.http.check.{ HttpCheckBuilder, HttpCheck }
import com.excilys.ebi.gatling.http.request.HttpPhase.{ HttpPhase, CompletePageReceived }
import com.excilys.ebi.gatling.core.check.CheckBuilderVerify
import com.excilys.ebi.gatling.core.check.CheckBuilderSave
import com.excilys.ebi.gatling.core.check.CheckBuilderFind
import com.excilys.ebi.gatling.core.check.CheckBuilderVerifyAll
import com.excilys.ebi.gatling.core.check.CheckBuilderVerifyOne

object HttpBodyRegexCheckBuilder {
	/**
	 *
	 */
	def regex(what: Session => String) = new HttpBodyRegexCheckBuilder(what, Some(0), ExistenceCheckStrategy, Nil, None) with CheckBuilderFind[HttpCheckBuilder[HttpBodyRegexCheckBuilder]]
	/**
	 *
	 */
	def regex(expression: String): HttpBodyRegexCheckBuilder with CheckBuilderFind[HttpCheckBuilder[HttpBodyRegexCheckBuilder]] = regex(interpolate(expression))
}

/**
 * This class builds a response body check based on regular expressions
 *
 * @param what the function returning the expression representing what is to be checked
 * @param strategy the strategy used to check
 * @param expected the expected value against which the extracted value will be checked
 * @param saveAs the optional session key in which the extracted value will be stored
 */
class HttpBodyRegexCheckBuilder(what: Session => String, occurrence: Option[Int], strategy: CheckStrategy, expected: List[Session => String], saveAs: Option[String])
		extends HttpCheckBuilder[HttpBodyRegexCheckBuilder](what, occurrence, strategy, expected, saveAs, CompletePageReceived) {

	def newInstance(what: Session => String, occurrence: Option[Int], strategy: CheckStrategy, expected: List[Session => String], saveAs: Option[String], when: HttpPhase) =
		new HttpBodyRegexCheckBuilder(what, occurrence, strategy, expected, saveAs)

	def newInstanceWithFindOne(occurrence: Int) =
		new HttpBodyRegexCheckBuilder(what, Some(occurrence), strategy, expected, saveAs) with CheckBuilderVerifyOne[HttpCheckBuilder[HttpBodyRegexCheckBuilder]]

	def newInstanceWithFindAll =
		new HttpBodyRegexCheckBuilder(what, None, strategy, expected, saveAs) with CheckBuilderVerifyAll[HttpCheckBuilder[HttpBodyRegexCheckBuilder]]

	def newInstanceWithVerify(strategy: CheckStrategy, expected: List[Session => String] = Nil) =
		new HttpBodyRegexCheckBuilder(what, occurrence, strategy, expected, saveAs) with CheckBuilderSave[HttpCheckBuilder[HttpBodyRegexCheckBuilder]]

	private[gatling] def build: HttpCheck = new HttpBodyRegexCheck(what, occurrence, strategy, expected, saveAs: Option[String])
}
