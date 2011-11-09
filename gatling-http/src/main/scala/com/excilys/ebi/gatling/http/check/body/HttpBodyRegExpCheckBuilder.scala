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
package com.excilys.ebi.gatling.http.check.body

import scala.annotation.implicitNotFound

import com.excilys.ebi.gatling.core.check.strategy.{ NonExistenceCheckStrategy, NonEqualityCheckStrategy, ExistenceCheckStrategy, EqualityCheckStrategy, CheckStrategy }
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY
import com.excilys.ebi.gatling.http.check.{ HttpCheckBuilder, HttpCheck }
import com.excilys.ebi.gatling.http.request.HttpPhase.{ HttpPhase, CompletePageReceived }

object HttpBodyRegExpCheckBuilder {
	def regexpEquals(what: Context => String, occurence: Int, expected: String) = new HttpBodyRegExpCheckBuilder((what, occurence), Some(EMPTY), EqualityCheckStrategy, Some(expected))
	def regexpEquals(what: Context => String, expected: String): HttpBodyRegExpCheckBuilder = regexpEquals(what, 0, expected)
	def regexpEquals(expression: String, occurence: Int, expected: String): HttpBodyRegExpCheckBuilder = regexpEquals((c: Context) => expression, occurence, expected)
	def regexpEquals(expression: String, expected: String): HttpBodyRegExpCheckBuilder = regexpEquals((c: Context) => expression, expected)

	def regexpNotEquals(what: Context => String, occurence: Int, expected: String) = new HttpBodyRegExpCheckBuilder((what, occurence), Some(EMPTY), NonEqualityCheckStrategy, Some(expected))
	def regexpNotEquals(what: Context => String, expected: String): HttpBodyRegExpCheckBuilder = regexpNotEquals(what, 0, expected)
	def regexpNotEquals(expression: String, occurence: Int, expected: String): HttpBodyRegExpCheckBuilder = regexpNotEquals((c: Context) => expression, occurence, expected)
	def regexpNotEquals(expression: String, expected: String): HttpBodyRegExpCheckBuilder = regexpNotEquals((c: Context) => expression, expected)

	def regexpExists(what: Context => String, occurence: Int) = new HttpBodyRegExpCheckBuilder((what, occurence), Some(EMPTY), ExistenceCheckStrategy, Some(EMPTY))
	def regexpExists(what: Context => String): HttpBodyRegExpCheckBuilder = regexpExists(what, 0)
	def regexpExists(expression: String, occurence: Int): HttpBodyRegExpCheckBuilder = regexpExists((c: Context) => expression, occurence)
	def regexpExists(expression: String): HttpBodyRegExpCheckBuilder = regexpExists((c: Context) => expression)

	def regexpNotExists(what: Context => String, occurence: Int) = new HttpBodyRegExpCheckBuilder((what, occurence), Some(EMPTY), NonExistenceCheckStrategy, Some(EMPTY))
	def regexpNotExists(what: Context => String): HttpBodyRegExpCheckBuilder = regexpNotExists(what, 0)
	def regexpNotExists(expression: String, occurence: Int): HttpBodyRegExpCheckBuilder = regexpNotExists((c: Context) => expression, occurence)
	def regexpNotExists(expression: String): HttpBodyRegExpCheckBuilder = regexpNotExists((c: Context) => expression)

	def regexp(what: Context => String, occurence: Int) = regexpExists(what, occurence)
	def regexp(what: Context => String) = regexpExists(what)
	def regexp(expression: String, occurence: Int) = regexpExists(expression, occurence)
	def regexp(expression: String) = regexpExists(expression)
}

/**
 * This class builds a response body check based on regular expressions
 *
 * @param what the function returning the expression representing what is to be checked
 * @param to the optional context key in which the extracted value will be stored
 * @param strategy the strategy used to check
 * @param expected the expected value against which the extracted value will be checked
 */
class HttpBodyRegExpCheckBuilder(what: (Context => String, Int), to: Option[String], strategy: CheckStrategy, expected: Option[String])
		extends HttpCheckBuilder[HttpBodyRegExpCheckBuilder](what._1, to, strategy, expected, CompletePageReceived) {

	def newInstance(what: Context => String, to: Option[String], strategy: CheckStrategy, expected: Option[String], when: HttpPhase) = new HttpBodyRegExpCheckBuilder((what, this.what._2), to, strategy, expected)

	def build: HttpCheck = new HttpBodyRegExpCheck(what, to, strategy, expected)
}
