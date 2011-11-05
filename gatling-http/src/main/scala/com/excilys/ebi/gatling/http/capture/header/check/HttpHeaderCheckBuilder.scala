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
package com.excilys.ebi.gatling.http.capture.header.check

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.http.request.HttpPhase._
import com.excilys.ebi.gatling.core.util.StringHelper._
import com.excilys.ebi.gatling.core.capture.check.NonEqualityCheckType
import com.excilys.ebi.gatling.core.capture.check.ExistenceCheckType
import com.excilys.ebi.gatling.core.capture.check.CheckType
import com.excilys.ebi.gatling.core.capture.check.EqualityCheckType
import com.excilys.ebi.gatling.core.capture.check.NonExistenceCheckType
import com.excilys.ebi.gatling.http.capture.HttpCheck
import com.excilys.ebi.gatling.http.capture.HttpCheckBuilder

object HttpHeaderCheckBuilder {
	def headerEquals(headerNameFormatter: Context => String, expected: String) = new HttpHeaderCheckBuilder(Some(headerNameFormatter), Some(EMPTY), Some(EqualityCheckType), Some(expected))
	def headerEquals(headerName: String, expected: String): HttpHeaderCheckBuilder = headerEquals((c: Context) => headerName, expected)

	def headerNotEquals(headerNameFormatter: Context => String, expected: String) = new HttpHeaderCheckBuilder(Some(headerNameFormatter), Some(EMPTY), Some(NonEqualityCheckType), Some(expected))
	def headerNotEquals(headerName: String, expected: String): HttpHeaderCheckBuilder = headerNotEquals((c: Context) => headerName, expected)

	def headerExists(headerNameFormatter: Context => String) = new HttpHeaderCheckBuilder(Some(headerNameFormatter), Some(EMPTY), Some(ExistenceCheckType), Some(EMPTY))
	def headerExists(headerName: String): HttpHeaderCheckBuilder = headerExists((c: Context) => headerName)

	def headerNotExists(headerNameFormatter: Context => String) = new HttpHeaderCheckBuilder(Some(headerNameFormatter), Some(EMPTY), Some(NonExistenceCheckType), Some(EMPTY))
	def headerNotExists(headerName: String): HttpHeaderCheckBuilder = headerNotExists((c: Context) => headerName)
}

class HttpHeaderCheckBuilder(what: Option[Context => String], to: Option[String], checkType: Option[CheckType], expected: Option[String])
		extends HttpCheckBuilder[HttpHeaderCheckBuilder](what, to, Some(HeadersReceived), checkType, expected) {

	def newInstance(what: Option[Context => String], to: Option[String], when: Option[HttpPhase], checkType: Option[CheckType], expected: Option[String]) = {
		new HttpHeaderCheckBuilder(what, to, checkType, expected)
	}

	def build: HttpCheck = new HttpHeaderCheck(what.get, to.get, checkType.get, expected.get)
}
