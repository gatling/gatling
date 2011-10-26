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
package com.excilys.ebi.gatling.http.processor.check.builder

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.processor.CheckType
import com.excilys.ebi.gatling.core.processor.builtin.EqualityCheckType
import com.excilys.ebi.gatling.core.processor.builtin.NonEqualityCheckType
import com.excilys.ebi.gatling.core.processor.builtin.ExistenceCheckType
import com.excilys.ebi.gatling.core.processor.builtin.NonExistenceCheckType
import com.excilys.ebi.gatling.http.processor.check.HttpCheck
import com.excilys.ebi.gatling.http.processor.check.HttpHeaderCheck
import com.excilys.ebi.gatling.http.request.HttpPhase._
import com.excilys.ebi.gatling.core.util.StringHelper._

object HttpHeaderCheckBuilder {
	def headerEquals(headerNameFormatter: Context => String, expected: String) = new HttpHeaderCheckBuilder(Some(headerNameFormatter), Some(expected), Some(EMPTY), Some(EqualityCheckType))
	def headerEquals(headerName: String, expected: String): HttpHeaderCheckBuilder = headerEquals((c: Context) => headerName, expected)

	def headerNotEquals(headerNameFormatter: Context => String, expected: String) = new HttpHeaderCheckBuilder(Some(headerNameFormatter), Some(expected), Some(EMPTY), Some(NonEqualityCheckType))
	def headerNotEquals(headerName: String, expected: String): HttpHeaderCheckBuilder = headerNotEquals((c: Context) => headerName, expected)

	def headerExists(headerNameFormatter: Context => String) = new HttpHeaderCheckBuilder(Some(headerNameFormatter), Some(EMPTY), Some(EMPTY), Some(ExistenceCheckType))
	def headerExists(headerName: String): HttpHeaderCheckBuilder = headerExists((c: Context) => headerName)

	def headerNotExists(headerNameFormatter: Context => String) = new HttpHeaderCheckBuilder(Some(headerNameFormatter), Some(EMPTY), Some(EMPTY), Some(NonExistenceCheckType))
	def headerNotExists(headerName: String): HttpHeaderCheckBuilder = headerNotExists((c: Context) => headerName)
}
class HttpHeaderCheckBuilder(expressionFormatter: Option[Context => String], expected: Option[String], attrKey: Option[String], checkType: Option[CheckType])
		extends HttpCheckBuilder[HttpHeaderCheckBuilder](expressionFormatter, expected, attrKey, Some(HeadersReceived), checkType) {

	def newInstance(expressionFormatter: Option[Context => String], expected: Option[String], attrKey: Option[String], httpPhase: Option[HttpPhase], checkType: Option[CheckType]) = {
		new HttpHeaderCheckBuilder(expressionFormatter, expected, attrKey, checkType)
	}

	def build: HttpCheck = new HttpHeaderCheck(expressionFormatter.get, expected.get, attrKey.get, checkType.get)
}
