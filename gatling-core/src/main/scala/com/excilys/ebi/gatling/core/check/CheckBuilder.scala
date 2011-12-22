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

import com.excilys.ebi.gatling.core.check.strategy.CheckStrategy
import com.excilys.ebi.gatling.core.check.strategy.EqualityCheckStrategy
import com.excilys.ebi.gatling.core.check.strategy.InRangeCheckStrategy
import com.excilys.ebi.gatling.core.check.strategy.NonEqualityCheckStrategy
import com.excilys.ebi.gatling.core.check.strategy.NonExistenceCheckStrategy
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.log.Logging
import strategy.ExistenceCheckStrategy

object CheckBuilder {
	implicit def intToString(i: Int) = i.toString
}
/**
 * This class serves as model for all check builders
 *
 * @param what the function that returns the expression representing what the check should look for
 * @param how the extractor that will be used by the Check
 * @param saveAs the session attribute that will be used to store the extracted value
 * @param strategy the strategy used to perform the Check
 * @param expected the expected value of what has been found
 */
abstract class CheckBuilder[B <: CheckBuilder[B, WHERE], WHERE](what: Session => String, occurrence: Option[Int], strategy: CheckStrategy, expected: List[Session => String], saveAs: Option[String])
		extends Logging {

	private[gatling] def build: Check[WHERE]

	private[gatling] def newInstance(what: Session => String, occurrence: Option[Int], strategy: CheckStrategy, expected: List[Session => String], saveAs: Option[String]): B
	private[gatling] def newInstanceWithVerify(strategy: CheckStrategy, expected: List[Session => String] = Nil): B with CheckBuilderSave[B]
	private[gatling] def newInstanceWithFindOne(occurrence: Int): B with CheckBuilderVerifyOne[B]
	private[gatling] def newInstanceWithFindAll: B with CheckBuilderVerifyAll[B]
	private[gatling] def newInstanceWithSaveAs(saveAs: String): B = newInstance(what, occurrence, strategy, expected, Some(saveAs))
}

