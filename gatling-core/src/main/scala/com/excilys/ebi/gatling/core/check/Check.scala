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
import CheckContext.useCheckContext

object Check {
	def applyChecks[R](s: Session, response: R, checks: Seq[Check[R, _]]): (Session, CheckResult[_]) = {

		var newSession = s
		var lastCheckResult: CheckResult[_] = null

		useCheckContext {
			for (check <- checks) {
				lastCheckResult = check.check(response, s)
				if (!lastCheckResult.ok)
					return (newSession, lastCheckResult)
				else if (check.saveAs.isDefined)
					newSession = newSession.setAttribute(check.saveAs.get, lastCheckResult.extractedValue.get)
			}
		}

		(newSession, lastCheckResult)
	}
}

/**
 * This class represents a Check
 *
 * @param expression the function that returns the expression representing what the check should look for
 * @param extractorFactory the extractor factory that will be used by the Check
 * @param saveAs the session attribute that will be used to store the extracted value
 * @param strategy the strategy used to perform the Check
 */
abstract class Check[R, X](val expression: Session => String, val extractorFactory: ExtractorFactory[R, X], val strategy: CheckStrategy[X], val saveAs: Option[String]) {

	def check(response: R, s: Session): CheckResult[X] = strategy(extractorFactory(response)(expression(s)), s)
}
