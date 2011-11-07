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

import com.excilys.ebi.gatling.core.check.strategy.{NonExistenceCheckStrategy, NonEqualityCheckStrategy, ExistenceCheckStrategy, EqualityCheckStrategy, CheckStrategy}
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY
import com.excilys.ebi.gatling.http.check.HttpCheckBuilder
import com.excilys.ebi.gatling.http.request.HttpPhase.{HttpPhase, CompletePageReceived}

object HttpBodyXPathCheckBuilder {

	def xpathEquals(what: Context => String, occurence: Int, expected: String) = new HttpBodyXPathCheckBuilder((what, occurence), Some(EMPTY), EqualityCheckStrategy, Some(expected))
	def xpathEquals(what: Context => String, expected: String) : HttpBodyXPathCheckBuilder= xpathEquals(what, 0, expected)
	def xpathEquals(expression: String, occurence: Int, expected: String): HttpBodyXPathCheckBuilder = xpathEquals(((c: Context) => expression), occurence, expected)
	def xpathEquals(expression: String, expected: String): HttpBodyXPathCheckBuilder = xpathEquals((c: Context) => expression, expected)

	def xpathNotEquals(what: Context => String, occurence: Int, expected: String) = new HttpBodyXPathCheckBuilder((what, occurence), Some(EMPTY), NonEqualityCheckStrategy, Some(expected))
	def xpathNotEquals(what: Context => String, expected: String) : HttpBodyXPathCheckBuilder= xpathNotEquals(what, 0, expected)
	def xpathNotEquals(expression: String, occurence: Int, expected: String): HttpBodyXPathCheckBuilder = xpathNotEquals(((c: Context) => expression), occurence, expected)
	def xpathNotEquals(expression: String, expected: String): HttpBodyXPathCheckBuilder = xpathNotEquals((c: Context) => expression, expected)

	def xpathExists(what: Context => String, occurence: Int) = new HttpBodyXPathCheckBuilder((what, occurence), Some(EMPTY), ExistenceCheckStrategy, None)
	def xpathExists(what: Context => String) : HttpBodyXPathCheckBuilder = xpathExists(what, 0)
	def xpathExists(expression: String, occurence: Int): HttpBodyXPathCheckBuilder = xpathExists((c: Context) => expression, occurence)
	def xpathExists(expression: String): HttpBodyXPathCheckBuilder = xpathExists((c: Context) => expression)

	def xpathNotExists(what: Context => String, occurence: Int) = new HttpBodyXPathCheckBuilder((what, occurence), Some(EMPTY), NonExistenceCheckStrategy, None)
	def xpathNotExists(what: Context => String) :HttpBodyXPathCheckBuilder = xpathNotExists(what, 0)
	def xpathNotExists(expression: String, occurence: Int): HttpBodyXPathCheckBuilder = xpathNotExists((c: Context) => expression, occurence)
	def xpathNotExists(expression: String): HttpBodyXPathCheckBuilder = xpathNotExists((c: Context) => expression)

	def xpath(what: Context => String, occurence: Int) = xpathExists(what, occurence)
	def xpath(what: Context => String) = xpathExists(what)
	def xpath(expression: String, occurence: Int) = xpathExists(expression, occurence)
	def xpath(expression: String) = xpathExists(expression)
}

class HttpBodyXPathCheckBuilder(what: (Context => String, Int), to: Option[String], strategy: CheckStrategy, expected: Option[String])
		extends HttpCheckBuilder[HttpBodyXPathCheckBuilder](what._1, to, strategy, expected, CompletePageReceived) {

	def newInstance(what: Context => String, to: Option[String], strategy: CheckStrategy, expected: Option[String], when: HttpPhase) = new HttpBodyXPathCheckBuilder((what, this.what._2), to, strategy, expected)

	def build = new HttpBodyXPathCheck(what, to, strategy, expected)
}
