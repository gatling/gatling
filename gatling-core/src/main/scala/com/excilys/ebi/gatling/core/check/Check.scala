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

	val DEFAULT_CHECK_RESULT: CheckStatus = Success(None)

	@tailrec
	private def applyChecksRec[R](session: Session, response: R, checks: List[Check[R, _]], previousCheckResult: CheckStatus): (Session, CheckStatus) = {
		checks match {
			case Nil => (session, previousCheckResult)
			case check :: otherChecks =>
				val checkResult = check.check(response, session)

				checkResult.status match {
					case failure @ Failure(_) => (session, failure)
					case success @ Success(extractedValue) =>
						val newSession = check.saveAs.map {
							val toBeSaved = checkResult.transformedValue.getOrElse(extractedValue)
							session.setAttribute(_, toBeSaved)
						}.getOrElse(session)

						applyChecksRec(newSession, response, otherChecks, success)
				}

		}
	}

	def applyChecks[R](session: Session, response: R, checks: List[Check[R, _]]): (Session, CheckStatus) = useCheckContext { applyChecksRec(session, response, checks, DEFAULT_CHECK_RESULT) }
}

/**
 * This class represents a Check
 *
 * @param expression the function that returns the expression representing what the check should look for
 * @param extractorFactory the extractor factory that will be used by the Check
 * @param saveAs the session attribute that will be used to store the extracted value
 * @param strategy the strategy used to perform the Check
 */
abstract class Check[R, X](val expression: EvaluatableString, val extractorFactory: ExtractorFactory[R, X], val strategy: CheckStrategy[X], val saveAs: Option[String], val transform: Option[X => Any]) {

	def check(response: R, session: Session): CheckResult = {
		val extractor = extractorFactory(response)
		val evaluatedExpression = expression(session)
		val extractedValue = extractor(evaluatedExpression)
		val transformedValue = for {
			extractedValue <- extractedValue
			transform <- transform
		} yield transform(extractedValue)
		val checkStatus = strategy(extractedValue, session)
		CheckResult(checkStatus, saveAs, transformedValue)
	}
}
