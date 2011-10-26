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
package com.excilys.ebi.gatling.http.processor.capture.builder

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.util.StringHelper._

import com.excilys.ebi.gatling.http.processor.capture.HttpCapture
import com.excilys.ebi.gatling.http.processor.capture.HttpRegExpCapture
import com.excilys.ebi.gatling.http.request.HttpPhase._

object HttpRegExpCaptureBuilder {
	def regexp(expressionFormatter: Context => String) = new HttpRegExpCaptureBuilder(Some(expressionFormatter), None, Some(CompletePageReceived))
	def regexp(expression: String): HttpRegExpCaptureBuilder = regexp((c: Context) => expression)
	def regexp(expressionToFormat: String, interpolations: String*): HttpRegExpCaptureBuilder = regexp((c: Context) => interpolateString(c, expressionToFormat, interpolations))
}
class HttpRegExpCaptureBuilder(expressionFormatter: Option[Context => String], attribute: Option[String], httpPhase: Option[HttpPhase])
		extends AbstractHttpCaptureBuilder[HttpRegExpCaptureBuilder](expressionFormatter, attribute, httpPhase) {

	def newInstance(expressionFormatter: Option[Context => String], attribute: Option[String], httpPhase: Option[HttpPhase]) = {
		new HttpRegExpCaptureBuilder(expressionFormatter, attribute, httpPhase)
	}

	def build: HttpCapture = new HttpRegExpCapture(expressionFormatter.get, attribute.get, httpPhase.get)
}