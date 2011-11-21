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
package com.excilys.ebi.gatling.core.check
import com.excilys.ebi.gatling.core.check.strategy.InRangeCheckStrategy.rangeToString
import com.excilys.ebi.gatling.core.check.strategy.CheckStrategy
import com.excilys.ebi.gatling.core.check.strategy.EqualityCheckStrategy
import com.excilys.ebi.gatling.core.check.strategy.InRangeCheckStrategy
import com.excilys.ebi.gatling.core.check.strategy.NonEqualityCheckStrategy
import com.excilys.ebi.gatling.core.check.strategy.NonExistenceCheckStrategy
import com.excilys.ebi.gatling.core.context.Context
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
 * @param saveAs the context attribute that will be used to store the extracted value
 * @param strategy the strategy used to perform the Check
 * @param expected the expected value of what has been found
 */
abstract class CheckBuilder[B <: CheckBuilder[B, WHERE], WHERE](what: Context => String, occurrence: Option[Int], strategy: CheckStrategy, expected: Option[String], saveAs: Option[String]) extends Logging {

	protected def newInstance(what: Context => String, occurrence: Option[Int], strategy: CheckStrategy, expected: Option[String], saveAs: Option[String]): B

	def find(occurrence: Int) = newInstance(what, Some(occurrence), strategy, expected, saveAs): B

	def first = find(0)

	def exists = verify(ExistenceCheckStrategy)

	def notExists = verify(NonExistenceCheckStrategy)

	def eq(expected: String) = verify(EqualityCheckStrategy, expected)

	def neq(expected: String) = verify(NonEqualityCheckStrategy, expected)

	def in(range: Range) = verify(InRangeCheckStrategy, range)

	def verify(strategy: CheckStrategy) = newInstance(what, occurrence, strategy, None, saveAs): B

	def verify(strategy: CheckStrategy, expected: String) = newInstance(what, occurrence, strategy, Some(expected), saveAs): B

	def saveAs(attrName: String) = newInstance(what, occurrence, strategy, expected, Some(attrName)): B

	def build: Check[WHERE]
}