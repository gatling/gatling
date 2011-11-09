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

	/**
	 * Will check if the value extracted via a regular expression is equal to a specified value
	 *
	 * @param what a function returning the regular expression
	 * @param occurrence the occurrence of the regular expression that should be extracted
	 * @param expected the value expected
	 */
	def regexpEquals(what: Context => String, occurrence: Int, expected: String) = new HttpBodyRegExpCheckBuilder((what, occurrence), Some(EMPTY), EqualityCheckStrategy, Some(expected))
	/**
	 * Will check if the value extracted via a regular expression is equal to a specified value
	 *
	 * The first occurrence of the regular expression will be extracted
	 *
	 * @param what a function returning the regular expression
	 * @param expected the value expected
	 */
	def regexpEquals(what: Context => String, expected: String): HttpBodyRegExpCheckBuilder = regexpEquals(what, 0, expected)
	/**
	 * Will check if the value extracted via a regular expression is equal to a specified value
	 *
	 * @param expression the regular expression
	 * @param occurrence the occurrence of the regular expression that should be extracted
	 * @param expected the value expected
	 */
	def regexpEquals(expression: String, occurrence: Int, expected: String): HttpBodyRegExpCheckBuilder = regexpEquals((c: Context) => expression, occurrence, expected)
	/**
	 * Will check if the value extracted via a regular expression is equal to a specified value
	 *
	 * The first occurrence of the regular expression will be extracted
	 *
	 * @param expression the regular expression
	 * @param expected the value expected
	 */
	def regexpEquals(expression: String, expected: String): HttpBodyRegExpCheckBuilder = regexpEquals((c: Context) => expression, expected)

	/**
	 * Will check if the value extracted via a regular expression is different from a specified value
	 *
	 * @param what a function returning the regular expression
	 * @param occurrence the occurrence of the regular expression that should be extracted
	 * @param expected the value expected
	 */
	def regexpNotEquals(what: Context => String, occurrence: Int, expected: String) = new HttpBodyRegExpCheckBuilder((what, occurrence), Some(EMPTY), NonEqualityCheckStrategy, Some(expected))
	/**
	 * Will check if the value extracted via a regular expression is different from a specified value
	 *
	 * The first occurrence of the regular expression will be extracted
	 *
	 * @param what a function returning the regular expression
	 * @param expected the value expected
	 */
	def regexpNotEquals(what: Context => String, expected: String): HttpBodyRegExpCheckBuilder = regexpNotEquals(what, 0, expected)
	/**
	 * Will check if the value extracted via a regular expression is different from a specified value
	 *
	 * @param expression the regular expression
	 * @param occurrence the occurrence of the regular expression that should be extracted
	 * @param expected the value expected
	 */
	def regexpNotEquals(expression: String, occurrence: Int, expected: String): HttpBodyRegExpCheckBuilder = regexpNotEquals((c: Context) => expression, occurrence, expected)
	/**
	 * Will check if the value extracted via a regular expression is different from a specified value
	 *
	 * The first occurrence of the regular expression will be extracted
	 *
	 * @param expression the regular expression
	 * @param expected the value expected
	 */
	def regexpNotEquals(expression: String, expected: String): HttpBodyRegExpCheckBuilder = regexpNotEquals((c: Context) => expression, expected)

	/**
	 * Will check if the regular expression result exists at least occurrence times
	 *
	 * @param what a function returning the regular expression
	 * @param occurrence the occurrence of the regular expression that should be extracted
	 */
	def regexpExists(what: Context => String, occurrence: Int) = new HttpBodyRegExpCheckBuilder((what, occurrence), Some(EMPTY), ExistenceCheckStrategy, Some(EMPTY))
	/**
	 * Will check if the regular expression result exists
	 *
	 * @param what a function returning the regular expression
	 */
	def regexpExists(what: Context => String): HttpBodyRegExpCheckBuilder = regexpExists(what, 0)
	/**
	 * Will check if the regular expression result exists at least occurrence times
	 *
	 * @param expression the regular expression
	 * @param occurrence the occurrence of the regular expression that should be extracted
	 */
	def regexpExists(expression: String, occurrence: Int): HttpBodyRegExpCheckBuilder = regexpExists((c: Context) => expression, occurrence)
	/**
	 * Will check if the regular expression result exists
	 *
	 * The first occurrence of the regular expression will be extracted
	 *
	 * @param expression the regular expression
	 */
	def regexpExists(expression: String): HttpBodyRegExpCheckBuilder = regexpExists((c: Context) => expression)

	/**
	 * Will check if the regular expression result does not exist more than occurrence times
	 *
	 * @param what a function returning the regular expression
	 * @param occurrence the occurrence of the regular expression that should be extracted
	 */
	def regexpNotExists(what: Context => String, occurrence: Int) = new HttpBodyRegExpCheckBuilder((what, occurrence), Some(EMPTY), NonExistenceCheckStrategy, Some(EMPTY))
	/**
	 * Will check if the regular expression result does not exist
	 *
	 * @param what a function returning the regular expression
	 */
	def regexpNotExists(what: Context => String): HttpBodyRegExpCheckBuilder = regexpNotExists(what, 0)
	/**
	 * Will check if the regular expression result does not exist more than occurrence times
	 *
	 * @param expression the regular expression
	 * @param occurrence the occurrence of the regular expression that should be extracted
	 */
	def regexpNotExists(expression: String, occurrence: Int): HttpBodyRegExpCheckBuilder = regexpNotExists((c: Context) => expression, occurrence)
	/**
	 * Will check if the regular expression result does not exist
	 *
	 * @param expression the regular expression
	 */
	def regexpNotExists(expression: String): HttpBodyRegExpCheckBuilder = regexpNotExists((c: Context) => expression)

	/**
	 * Will capture the occurrence-th result of the regular expression
	 *
	 * @param what the function returning the regular expression
	 */
	def regexp(what: Context => String, occurrence: Int) = regexpExists(what, occurrence)
	/**
	 * Will capture the result of the regular expression
	 *
	 * @param what the function returning the regular expression
	 */
	def regexp(what: Context => String) = regexpExists(what)
	/**
	 * Will capture the occurrence-th result of the regular expression
	 *
	 * @param expression the regular expression
	 */
	def regexp(expression: String, occurrence: Int) = regexpExists(expression, occurrence)
	/**
	 * Will capture the result of the regular expression
	 *
	 * @param expression the regular expression
	 */
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
