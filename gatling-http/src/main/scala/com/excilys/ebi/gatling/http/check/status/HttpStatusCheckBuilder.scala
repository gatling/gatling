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
package com.excilys.ebi.gatling.http.check.status

import scala.annotation.implicitNotFound
import com.excilys.ebi.gatling.core.check.strategy.InRangeCheckStrategy.rangeToString
import com.excilys.ebi.gatling.core.check.strategy.{ InRangeCheckStrategy, EqualityCheckStrategy, CheckStrategy }
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY
import com.excilys.ebi.gatling.http.check.{ HttpCheckBuilder, HttpCheck }
import com.excilys.ebi.gatling.http.request.HttpPhase.{ StatusReceived, HttpPhase }
import com.excilys.ebi.gatling.core.check.CheckBuilderVerify
import com.excilys.ebi.gatling.core.check.CheckBuilderSave
import com.excilys.ebi.gatling.core.check.CheckBuilderFind

/**
 * HttpStatusCheckBuilder class companion
 *
 * It contains DSL definitions
 */
object HttpStatusCheckBuilder {
	/**
	 * Will check that the response status is in the specified range
	 *
	 * @param range the specified range
	 */
	def status = new HttpStatusCheckBuilder(InRangeCheckStrategy, None, None) with CheckBuilderFind[HttpCheckBuilder[HttpStatusCheckBuilder]]
}

/**
 * This class builds a response status check
 *
 * @param to the optional context key in which the extracted value will be stored
 * @param strategy the strategy used to check
 * @param expected the expected value against which the extracted value will be checked
 */
class HttpStatusCheckBuilder(strategy: CheckStrategy, expected: Option[String], saveAs: Option[String])
		extends HttpCheckBuilder[HttpStatusCheckBuilder]((c: Context) => EMPTY, None, strategy, expected, saveAs, StatusReceived) {

	def newInstance(what: Context => String, occurrence: Option[Int], strategy: CheckStrategy, expected: Option[String], saveAs: Option[String], when: HttpPhase) =
		new HttpStatusCheckBuilder(strategy, expected, saveAs)

	def newInstanceWithFind(occurrence: Int): HttpCheckBuilder[HttpStatusCheckBuilder] with CheckBuilderVerify[HttpCheckBuilder[HttpStatusCheckBuilder]] =
		new HttpStatusCheckBuilder(strategy, expected, saveAs) with CheckBuilderVerify[HttpCheckBuilder[HttpStatusCheckBuilder]]

	def newInstanceWithVerify(strategy: CheckStrategy, expected: Option[String] = None): HttpCheckBuilder[HttpStatusCheckBuilder] with CheckBuilderSave[HttpCheckBuilder[HttpStatusCheckBuilder]] =
		new HttpStatusCheckBuilder(strategy, expected, saveAs) with CheckBuilderSave[HttpCheckBuilder[HttpStatusCheckBuilder]]

	def build: HttpCheck = new HttpStatusCheck(expected, saveAs)
}
