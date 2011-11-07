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

import com.excilys.ebi.gatling.core.check.strategy.{NonExistenceCheckStrategy, NonEqualityCheckStrategy, ExistenceCheckStrategy, EqualityCheckStrategy, CheckStrategy}
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY
import com.excilys.ebi.gatling.http.check.{HttpCheckBuilder, HttpCheck}
import com.excilys.ebi.gatling.http.request.HttpPhase.{HttpPhase, HeadersReceived}

object HttpHeaderCheckBuilder {
	def headerEquals(what: Context => String, expected: String) = new HttpHeaderCheckBuilder(what, Some(EMPTY), EqualityCheckStrategy, Some(expected))
	def headerEquals(headerName: String, expected: String): HttpHeaderCheckBuilder = headerEquals((c: Context) => headerName, expected)

	def headerNotEquals(what: Context => String, expected: String) = new HttpHeaderCheckBuilder(what, Some(EMPTY), NonEqualityCheckStrategy, Some(expected))
	def headerNotEquals(headerName: String, expected: String): HttpHeaderCheckBuilder = headerNotEquals((c: Context) => headerName, expected)

	def headerExists(what: Context => String) = new HttpHeaderCheckBuilder(what, Some(EMPTY), ExistenceCheckStrategy, Some(EMPTY))
	def headerExists(headerName: String): HttpHeaderCheckBuilder = headerExists((c: Context) => headerName)

	def headerNotExists(what: Context => String) = new HttpHeaderCheckBuilder(what, Some(EMPTY), NonExistenceCheckStrategy, Some(EMPTY))
	def headerNotExists(headerName: String): HttpHeaderCheckBuilder = headerNotExists((c: Context) => headerName)

	def header(what: Context => String) = headerExists(what)
	def header(headerName: String) = headerExists(headerName)
}

class HttpHeaderCheckBuilder(what: Context => String, to: Option[String], strategy: CheckStrategy, expected: Option[String])
		extends HttpCheckBuilder[HttpHeaderCheckBuilder](what, to, strategy, expected, HeadersReceived) {

	def newInstance(what: Context => String, to: Option[String], strategy: CheckStrategy, expected: Option[String], when: HttpPhase) = new HttpHeaderCheckBuilder(what, to, strategy, expected)

	def build: HttpCheck = new HttpHeaderCheck(what, to, strategy, expected)
}
