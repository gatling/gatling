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
package com.excilys.ebi.gatling.core.check

import scala.annotation.tailrec

import com.excilys.ebi.gatling.core.check.CheckContext.useCheckContext
import com.excilys.ebi.gatling.core.session.{ Expression, Session }

import scalaz._
import Scalaz._

object Check {

	/**
	 * Applies a list of checks on a given response
	 *
	 * @param session the session of the virtual user
	 * @param response the response
	 * @param checks the checks to be applied
	 * @return the result of the checks: Success or the first encountered Failure
	 */
	def applyChecks[R](session: Session, response: R, checks: List[Check[R, _]]): Validation[String, Session] = {

		@tailrec
		def applyChecksRec(checks: List[Check[R, _]], validation: Validation[String, Session]): Validation[String, Session] = checks match {
			case Nil => validation
			case check :: otherChecks =>
				val newValidation = validation.flatMap(check.apply(response))
				applyChecksRec(otherChecks, newValidation)
		}

		useCheckContext {
			applyChecksRec(checks, session.success)
		}
	}
}

/**
 * Model for Checks
 *
 * @param expression the function that returns the expression representing what the check should look for
 * @param matcher the matcher that will try to match the result of the extraction
 * @param saveAs the session attribute that will be used to store the extracted value if the checks are successful
 */
class Check[R, XC](val expression: Expression[XC], matcher: Matcher[R, XC], saveAs: Option[String]) {

	def apply(response: R)(session: Session): Validation[String, Session] = {
		val validation = expression(session).flatMap(matcher(response, session, _))
		validation.map { value => saveAs.map(session.setAttribute(_, value)).getOrElse(session) }
	}
}
