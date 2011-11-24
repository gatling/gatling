/*
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
package com.excilys.ebi.gatling.core.check.strategy

/**
 * Represents a Check on the presence of value in range expected
 */
object InRangeCheckStrategy extends CheckStrategy {

	val SEPARATOR = ":"

	implicit def rangeToString(range: Range) = range.mkString(SEPARATOR)

	def check(value: List[String], expected: List[String]) = !value.isEmpty && !expected.isEmpty && expected(0).split(SEPARATOR).contains(value(0))
}