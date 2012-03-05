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
import com.excilys.ebi.gatling.core.result.message.RequestStatus.OK
import com.excilys.ebi.gatling.core.session.{ Session, EvaluatableString }

object Check {

	val DEFAULT_CHECK_RESULT: CheckResult = Success(None)

	@tailrec
	private def applyChecksRec[R](session: Session, response: R, checks: List[Check[R]], previousCheckResult: CheckResult): (Session, CheckResult) = {
		checks match {
			case Nil => (session, previousCheckResult)
			case check :: otherChecks =>
				val checkResult = check.check(response, session)

				checkResult match {
					case failure @ Failure(_) => (session, failure)
					case success @ Success(extractedValue) =>
						val newSession = (
							for {
								extractedValue <- extractedValue
								saveAs <- check.saveAs
							} yield session.setAttribute(saveAs, extractedValue))
							.getOrElse(session)

						applyChecksRec(newSession, response, otherChecks, success)
				}

		}
	}

	def applyChecks[R](session: Session, response: R, checks: List[Check[R]]): (Session, CheckResult) = useCheckContext { applyChecksRec(session, response, checks, DEFAULT_CHECK_RESULT) }
}

/**
 * This class represents a Check
 *
 * @param expression the function that returns the expression representing what the check should look for
 * @param extractorFactory the extractor factory that will be used by the Check
 * @param saveAs the session attribute that will be used to store the extracted value
 * @param strategy the strategy used to perform the Check
 */
abstract class Check[R](expression: EvaluatableString, matcher: Matcher[R], val saveAs: Option[String]) {

	def check(response: R, session: Session): CheckResult = matcher(expression, session, response)
}
