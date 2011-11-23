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
import com.excilys.ebi.gatling.core.check.strategy.CheckStrategy
import com.excilys.ebi.gatling.core.check.strategy.ExistenceCheckStrategy
import com.excilys.ebi.gatling.core.check.strategy.NonExistenceCheckStrategy
import com.excilys.ebi.gatling.core.check.strategy.EqualityCheckStrategy
import com.excilys.ebi.gatling.core.check.strategy.NonEqualityCheckStrategy
import com.excilys.ebi.gatling.core.check.strategy.InRangeCheckStrategy
import com.excilys.ebi.gatling.core.check.strategy.InRangeCheckStrategy.rangeToString

trait CheckBuilderVerify[B <: CheckBuilder[B, _]] extends CheckBuilderSave[B] { this: CheckBuilder[B, _] with CheckBuilderSave[B] =>
	def verify(strategy: CheckStrategy) = newInstanceWithVerify(strategy)

	def verify(strategy: CheckStrategy, expected: String) = newInstanceWithVerify(strategy, Some(expected))

	def exists = verify(ExistenceCheckStrategy)

	def notExists = verify(NonExistenceCheckStrategy)

	def eq(expected: String) = verify(EqualityCheckStrategy, expected)

	def neq(expected: String) = verify(NonEqualityCheckStrategy, expected)

	def in(range: Range) = verify(InRangeCheckStrategy, range)
}