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
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.check.CheckContext.useCheckContext
import com.excilys.ebi.gatling.core.session.EvaluatableString
import com.excilys.ebi.gatling.core.result.message.RequestStatus.OK
import scala.annotation.tailrec

object Check {

	val DEFAULT_CHECK_RESULT = CheckResult(true, None)

	@tailrec
	private def applyChecksRec[R](session: Session, response: R, checks: List[Check[R, _]], previousCheckResult: CheckResult[_]): (Session, CheckResult[_]) = {
		checks match {
			case Nil => (session, previousCheckResult)
			case check :: otherChecks =>
				val checkResult = check.check(response, session)

				if (!checkResult.ok)
					(session, checkResult)

				else {
					val extractedValue = checkResult.extractedValue.get
					val newSession = check.saveAs.map(session.setAttribute(_, extractedValue)).getOrElse(session)

					applyChecksRec(newSession, response, otherChecks, checkResult)
				}

		}
	}

	def applyChecks[R](session: Session, response: R, checks: List[Check[R, _]]): (Session, CheckResult[_]) = useCheckContext { applyChecksRec(session, response, checks, DEFAULT_CHECK_RESULT) }
}

/**
 * This class represents a Check
 *
 * @param expression the function that returns the expression representing what the check should look for
 * @param extractorFactory the extractor factory that will be used by the Check
 * @param saveAs the session attribute that will be used to store the extracted value
 * @param strategy the strategy used to perform the Check
 */
abstract class Check[R, X](val expression: EvaluatableString, val extractorFactory: ExtractorFactory[R, X], val strategy: CheckStrategy[X], val saveAs: Option[String]) {

	def check(response: R, session: Session): CheckResult[X] = {
		val extractor = extractorFactory(response)
		val evaluatedExpression = expression(session)
		val extractedValue = extractor(evaluatedExpression)
		strategy(extractedValue, session)
	}
}
