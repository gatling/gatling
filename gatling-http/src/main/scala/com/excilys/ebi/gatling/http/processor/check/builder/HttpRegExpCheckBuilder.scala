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
import com.excilys.ebi.gatling.http.request.HttpPhase._
import com.excilys.ebi.gatling.http.processor.check.HttpCheck
import com.excilys.ebi.gatling.http.processor.check.HttpRegExpCheck
import com.excilys.ebi.gatling.core.util.StringHelper._

object HttpRegExpCheckBuilder {
	def regexpEquals(expressionFormatter: Context => String, expected: String) = new HttpRegExpCheckBuilder(Some(expressionFormatter), Some(expected), Some(EMPTY), Some(CompletePageReceived), Some(EqualityCheckType))
	def regexpEquals(expression: String, expected: String): HttpRegExpCheckBuilder = regexpEquals((c: Context) => expression, expected)

	def regexpNotEquals(expressionFormatter: Context => String, expected: String) = new HttpRegExpCheckBuilder(Some(expressionFormatter), Some(expected), Some(EMPTY), Some(CompletePageReceived), Some(NonEqualityCheckType))
	def regexpNotEquals(expression: String, expected: String): HttpRegExpCheckBuilder = regexpNotEquals((c: Context) => expression, expected)

	def regexpExists(expressionFormatter: Context => String) = new HttpRegExpCheckBuilder(Some(expressionFormatter), Some(EMPTY), Some(EMPTY), Some(CompletePageReceived), Some(ExistenceCheckType))
	def regexpExists(expression: String): HttpRegExpCheckBuilder = regexpExists((c: Context) => expression)

	def regexpNotExists(expressionFormatter: Context => String) = new HttpRegExpCheckBuilder(Some(expressionFormatter), Some(EMPTY), Some(EMPTY), Some(CompletePageReceived), Some(NonExistenceCheckType))
	def regexpNotExists(expression: String): HttpRegExpCheckBuilder = regexpNotExists((c: Context) => expression)
}
class HttpRegExpCheckBuilder(expressionFormatter: Option[Context => String], expected: Option[String], attrKey: Option[String], httpPhase: Option[HttpPhase], checkType: Option[CheckType])
		extends HttpCheckBuilder[HttpRegExpCheckBuilder](expressionFormatter, expected, attrKey, httpPhase, checkType) {

	def newInstance(expressionFormatter: Option[Context => String], expected: Option[String], attrKey: Option[String], httpPhase: Option[HttpPhase], checkType: Option[CheckType]) = {
		new HttpRegExpCheckBuilder(expressionFormatter, expected, attrKey, httpPhase, checkType)
	}

	def build: HttpCheck = new HttpRegExpCheck(expressionFormatter.get, expected.get, attrKey.get, httpPhase.get, checkType.get)
}
