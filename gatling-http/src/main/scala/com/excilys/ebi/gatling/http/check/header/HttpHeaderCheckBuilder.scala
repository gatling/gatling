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
package com.excilys.ebi.gatling.http.check.header

import scala.annotation.implicitNotFound

import com.excilys.ebi.gatling.core.check.strategy.{ NonExistenceCheckStrategy, NonEqualityCheckStrategy, ExistenceCheckStrategy, EqualityCheckStrategy, CheckStrategy }
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY
import com.excilys.ebi.gatling.http.check.{ HttpCheckBuilder, HttpCheck }
import com.excilys.ebi.gatling.http.request.HttpPhase.{ HttpPhase, HeadersReceived }

/**
 * HttpHeaderCheckBuilder class companion
 *
 * It contains DSL definitions
 */
object HttpHeaderCheckBuilder {
	/**
	 * Will check that the specified header's value is equal to the user defined one
	 *
	 * @param what the function returning the name of the header
	 * @param expected the expected value of the header
	 */
	def headerEquals(what: Context => String, expected: String) = new HttpHeaderCheckBuilder(what, Some(EMPTY), EqualityCheckStrategy, Some(expected))
	/**
	 * Will check that the specified header's value is equal to the user defined one
	 *
	 * @param headerName the name of the header
	 * @param expected the expected value of the header
	 */
	def headerEquals(headerName: String, expected: String): HttpHeaderCheckBuilder = headerEquals((c: Context) => headerName, expected)

	/**
	 * Will check that the specified header's value is different from the user defined one
	 *
	 * @param what the function returning the name of the header
	 * @param expected the expected value of the header
	 */
	def headerNotEquals(what: Context => String, expected: String) = new HttpHeaderCheckBuilder(what, Some(EMPTY), NonEqualityCheckStrategy, Some(expected))
	/**
	 * Will check that the specified header's value is different from the user defined one
	 *
	 * @param headerName the name of the header
	 * @param expected the expected value of the header
	 */
	def headerNotEquals(headerName: String, expected: String): HttpHeaderCheckBuilder = headerNotEquals((c: Context) => headerName, expected)

	/**
	 * Will check that the specified header exists
	 *
	 * @param what the function returning the name of the header
	 */
	def headerExists(what: Context => String) = new HttpHeaderCheckBuilder(what, Some(EMPTY), ExistenceCheckStrategy, Some(EMPTY))
	/**
	 * Will check that the specified header exists
	 *
	 * @param headerName the name of the header
	 */
	def headerExists(headerName: String): HttpHeaderCheckBuilder = headerExists((c: Context) => headerName)

	/**
	 * Will check that the specified header is not present
	 *
	 * @param what the function returning the name of the header
	 */
	def headerNotExists(what: Context => String) = new HttpHeaderCheckBuilder(what, Some(EMPTY), NonExistenceCheckStrategy, Some(EMPTY))
	/**
	 * Will check that the specified header is not present
	 *
	 * @param headerName the name of the header
	 */
	def headerNotExists(headerName: String): HttpHeaderCheckBuilder = headerNotExists((c: Context) => headerName)

	/**
	 * Will capture the value of the header in the context
	 *
	 * @param what the function returning the name of the header
	 */
	def header(what: Context => String) = headerExists(what)
	/**
	 * Will capture the value of the header in the context
	 *
	 * @param headerName the name of the header
	 */
	def header(headerName: String) = headerExists(headerName)
}

/**
 * This class builds a response header check
 *
 * @param what the function returning the header name to be checked
 * @param to the optional context key in which the extracted value will be stored
 * @param strategy the strategy used to check
 * @param expected the expected value against which the extracted value will be checked
 */
class HttpHeaderCheckBuilder(what: Context => String, to: Option[String], strategy: CheckStrategy, expected: Option[String])
		extends HttpCheckBuilder[HttpHeaderCheckBuilder](what, to, strategy, expected, HeadersReceived) {

	def newInstance(what: Context => String, to: Option[String], strategy: CheckStrategy, expected: Option[String], when: HttpPhase) = new HttpHeaderCheckBuilder(what, to, strategy, expected)

	def build: HttpCheck = new HttpHeaderCheck(what, to, strategy, expected)
}
