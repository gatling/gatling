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

import com.excilys.ebi.gatling.core.check.strategy.CheckStrategy
import com.excilys.ebi.gatling.core.check.strategy.ExistenceCheckStrategy
import com.excilys.ebi.gatling.core.check.CheckBuilderFind
import com.excilys.ebi.gatling.core.check.CheckBuilderVerifyOne
import com.excilys.ebi.gatling.core.check.CheckBuilderSave
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.util.StringHelper.interpolate
import com.excilys.ebi.gatling.http.check.HttpCheck
import com.excilys.ebi.gatling.http.check.HttpCheckBuilder
import com.excilys.ebi.gatling.http.request.HttpPhase.HeadersReceived
import com.excilys.ebi.gatling.http.request.HttpPhase.HttpPhase

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
	def header(what: Session => String) = new HttpHeaderCheckBuilder(what, ExistenceCheckStrategy, Nil, None) with CheckBuilderFind[HttpCheckBuilder[HttpHeaderCheckBuilder]]
	/**
	 * Will check the value of the header in the session
	 *
	 * @param headerName the name of the header
	 */
	def header(headerName: String): HttpHeaderCheckBuilder with CheckBuilderFind[HttpCheckBuilder[HttpHeaderCheckBuilder]] = header(interpolate(headerName))
}

/**
 * This class builds a response header check
 *
 * @param what the function returning the header name to be checked
 * @param to the optional session key in which the extracted value will be stored
 * @param strategy the strategy used to check
 * @param expected the expected value against which the extracted value will be checked
 */
class HttpHeaderCheckBuilder(what: Session => String, strategy: CheckStrategy, expected: List[String], saveAs: Option[String])
		extends HttpCheckBuilder[HttpHeaderCheckBuilder](what, None, strategy, expected, saveAs, HeadersReceived) {

	private[http] def newInstance(what: Session => String, occurrence: Option[Int], strategy: CheckStrategy, expected: List[String], saveAs: Option[String], when: HttpPhase) =
		new HttpHeaderCheckBuilder(what, strategy, expected, saveAs)

	private[gatling] def newInstanceWithFindOne(occurrence: Int) =
		new HttpHeaderCheckBuilder(what, strategy, expected, saveAs) with CheckBuilderVerifyOne[HttpCheckBuilder[HttpHeaderCheckBuilder]]

	private[gatling] def newInstanceWithFindAll = throw new UnsupportedOperationException("Header checks are single valued")

	private[gatling] def newInstanceWithVerify(strategy: CheckStrategy, expected: List[String] = Nil) =
		new HttpHeaderCheckBuilder(what, strategy, expected, saveAs) with CheckBuilderSave[HttpCheckBuilder[HttpHeaderCheckBuilder]]

	private[gatling] def build: HttpCheck = new HttpHeaderCheck(what, strategy, expected, saveAs)
}
