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
import com.excilys.ebi.gatling.core.session.Session

object Check {

	/**
	 * Applies a list of checks on a given response
	 *
	 * @param session the session of the virtual user
	 * @param response the response
	 * @param checks the checks to be applied
	 * @return the result of the checks: Success or the first encountered Failure
	 */
	def applyChecks[R](session: Session, response: R, checks: List[Check[R, _]]): (Session, CheckResult) = {

		@tailrec
		def applyChecksRec(session: Session, checks: List[Check[R, _]], previousCheckResult: CheckResult): (Session, CheckResult) = checks match {
			case Nil =>
				(session, previousCheckResult)

			case check :: otherChecks =>
				val (newSession, checkResult) = check(response, session)

				checkResult match {
					case failure @ Failure(_) => (newSession.setFailed, failure)
					case success @ Success(extractedValue) => applyChecksRec(newSession, otherChecks, success)
				}
		}

		useCheckContext {
			applyChecksRec(session, checks, Success(None))
		}
	}
}

/**
 * Model for Checks
 *
 * @param expression the function that returns the expression representing what the check should look for
 * @param matcher the matcher that will try to match the result of the extraction
 * @param saveAs the session attribute that will be used to store the extracted value if the checks are successful
 * @param strategy the strategy used to perform the Check
 */
class Check[R, XC](val expression: Session => XC, matcher: Matcher[R, XC], saveAs: Option[String]) {

	def apply(response: R, session: Session): (Session, CheckResult) = matcher(expression, session, response) match {
		case success @ Success(extractedValue) =>
			val newSessionWithSaved = for {
				extractedValue <- extractedValue
				saveAs <- saveAs
			} yield session.setAttribute(saveAs, extractedValue)

			(newSessionWithSaved.getOrElse(session), success)

		case failure => (session, failure)
	}
}
