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
import com.excilys.ebi.gatling.http.check.HttpCheckBuilder
import com.excilys.ebi.gatling.http.request.HttpPhase.{ HttpPhase, CompletePageReceived }

object HttpBodyXPathCheckBuilder {

	/**
	 * Will check if the value extracted via an XPath expression is equal to a specified value
	 *
	 * @param what a function returning the XPath expression
	 * @param occurrence the occurrence of the XPath expression that should be extracted
	 * @param expected the value expected
	 */
	def xpathEquals(what: Context => String, occurence: Int, expected: String) = new HttpBodyXPathCheckBuilder((what, occurence), Some(EMPTY), EqualityCheckStrategy, Some(expected))
	/**
	 * Will check if the value extracted via an XPath expression is equal to a specified value
	 *
	 * The first occurrence of the XPath expression will be extracted
	 *
	 * @param what a function returning the XPath expression
	 * @param expected the value expected
	 */
	def xpathEquals(what: Context => String, expected: String): HttpBodyXPathCheckBuilder = xpathEquals(what, 0, expected)
	/**
	 * Will check if the value extracted via an XPath expression is equal to a specified value
	 *
	 * @param expression the XPath expression
	 * @param occurrence the occurrence of the XPath expression that should be extracted
	 * @param expected the value expected
	 */
	def xpathEquals(expression: String, occurence: Int, expected: String): HttpBodyXPathCheckBuilder = xpathEquals(((c: Context) => expression), occurence, expected)
	/**
	 * Will check if the value extracted via an XPath expression is equal to a specified value
	 *
	 * The first occurrence of the XPath expression will be extracted
	 *
	 * @param expression the XPath expression
	 * @param expected the value expected
	 */
	def xpathEquals(expression: String, expected: String): HttpBodyXPathCheckBuilder = xpathEquals((c: Context) => expression, expected)

	/**
	 * Will check if the value extracted via an XPath expression is different from a specified value
	 *
	 * @param what a function returning the XPath expression
	 * @param occurrence the occurrence of the XPath expression that should be extracted
	 * @param expected the value expected
	 */
	def xpathNotEquals(what: Context => String, occurence: Int, expected: String) = new HttpBodyXPathCheckBuilder((what, occurence), Some(EMPTY), NonEqualityCheckStrategy, Some(expected))
	/**
	 * Will check if the value extracted via an XPath expression is different from a specified value
	 *
	 * The first occurrence of the XPath expression will be extracted
	 *
	 * @param what a function returning the XPath expression
	 * @param expected the value expected
	 */
	def xpathNotEquals(what: Context => String, expected: String): HttpBodyXPathCheckBuilder = xpathNotEquals(what, 0, expected)
	/**
	 * Will check if the value extracted via an XPath expression is different from a specified value
	 *
	 * @param expression the XPath expression
	 * @param occurrence the occurrence of the XPath expression that should be extracted
	 * @param expected the value expected
	 */
	def xpathNotEquals(expression: String, occurence: Int, expected: String): HttpBodyXPathCheckBuilder = xpathNotEquals(((c: Context) => expression), occurence, expected)
	/**
	 * Will check if the value extracted via an XPath expression is different from a specified value
	 *
	 * The first occurrence of the XPath expression will be extracted
	 *
	 * @param expression the XPath expression
	 * @param expected the value expected
	 */
	def xpathNotEquals(expression: String, expected: String): HttpBodyXPathCheckBuilder = xpathNotEquals((c: Context) => expression, expected)

	/**
	 * Will check if the XPath expression result exists at least occurrence times
	 *
	 * @param what a function returning the XPath expression
	 * @param occurrence the occurrence of the XPath expression that should be extracted
	 */
	def xpathExists(what: Context => String, occurence: Int) = new HttpBodyXPathCheckBuilder((what, occurence), Some(EMPTY), ExistenceCheckStrategy, None)
	/**
	 * Will check if the XPath expression result exists
	 *
	 * @param what a function returning the XPath expression
	 */
	def xpathExists(what: Context => String): HttpBodyXPathCheckBuilder = xpathExists(what, 0)
	/**
	 * Will check if the XPath expression result exists at least occurrence times
	 *
	 * @param expression the XPath expression
	 * @param occurrence the occurrence of the XPath expression that should be extracted
	 */
	def xpathExists(expression: String, occurence: Int): HttpBodyXPathCheckBuilder = xpathExists((c: Context) => expression, occurence)
	/**
	 * Will check if the XPath expression result exists
	 *
	 * The first occurrence of the XPath expression will be extracted
	 *
	 * @param expression the XPath expression
	 */
	def xpathExists(expression: String): HttpBodyXPathCheckBuilder = xpathExists((c: Context) => expression)

	/**
	 * Will check if the XPath expression result does not exist more than occurrence times
	 *
	 * @param what a function returning the XPath expression
	 * @param occurrence the occurrence of the XPath expression that should be extracted
	 */
	def xpathNotExists(what: Context => String, occurence: Int) = new HttpBodyXPathCheckBuilder((what, occurence), Some(EMPTY), NonExistenceCheckStrategy, None)
	/**
	 * Will check if the XPath expression result does not exist
	 *
	 * @param what a function returning the XPath expression
	 */
	def xpathNotExists(what: Context => String): HttpBodyXPathCheckBuilder = xpathNotExists(what, 0)
	/**
	 * Will check if the XPath expression result does not exist more than occurrence times
	 *
	 * @param expression the XPath expression
	 * @param occurrence the occurrence of the XPath expression that should be extracted
	 */
	def xpathNotExists(expression: String, occurence: Int): HttpBodyXPathCheckBuilder = xpathNotExists((c: Context) => expression, occurence)
	/**
	 * Will check if the XPath expression result does not exist
	 *
	 * @param expression the XPath expression
	 */
	def xpathNotExists(expression: String): HttpBodyXPathCheckBuilder = xpathNotExists((c: Context) => expression)

	/**
	 * Will capture the occurrence-th result of the XPath expression
	 *
	 * @param what the function returning the XPath expression
	 */
	def xpath(what: Context => String, occurence: Int) = xpathExists(what, occurence)
	/**
	 * Will capture the result of the XPath expression
	 *
	 * @param what the function returning the XPath expression
	 */
	def xpath(what: Context => String) = xpathExists(what)
	/**
	 * Will capture the occurrence-th result of the XPath expression
	 *
	 * @param expression the XPath expression
	 */
	def xpath(expression: String, occurence: Int) = xpathExists(expression, occurence)
	/**
	 * Will capture the result of the XPath expression
	 *
	 * @param expression the XPath expression
	 */
	def xpath(expression: String) = xpathExists(expression)
}

/**
 * This class builds a response body check based on XPath expressions
 *
 * @param what the function returning the expression representing what is to be checked
 * @param to the optional context key in which the extracted value will be stored
 * @param strategy the strategy used to check
 * @param expected the expected value against which the extracted value will be checked
 */
class HttpBodyXPathCheckBuilder(what: (Context => String, Int), to: Option[String], strategy: CheckStrategy, expected: Option[String])
		extends HttpCheckBuilder[HttpBodyXPathCheckBuilder](what._1, to, strategy, expected, CompletePageReceived) {

	def newInstance(what: Context => String, to: Option[String], strategy: CheckStrategy, expected: Option[String], when: HttpPhase) = new HttpBodyXPathCheckBuilder((what, this.what._2), to, strategy, expected)

	def build = new HttpBodyXPathCheck(what, to, strategy, expected)
}
