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
package com.excilys.ebi.gatling.http.capture.body

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.util.StringHelper.interpolateString
import com.excilys.ebi.gatling.http.capture.HttpCaptureBuilder
import com.excilys.ebi.gatling.http.capture.HttpCapture
import com.excilys.ebi.gatling.http.request.HttpPhase.CompletePageReceived

object HttpBodyRegExpCaptureBuilder {
	def regexp(expressionFormatter: Context => String) = new HttpBodyRegExpCaptureBuilder(expressionFormatter, None)
	def regexp(expression: String): HttpBodyRegExpCaptureBuilder = regexp((c: Context) => expression)
	def regexp(expressionToFormat: String, interpolations: String*): HttpBodyRegExpCaptureBuilder = regexp((c: Context) => interpolateString(c, expressionToFormat, interpolations))
}

class HttpBodyRegExpCaptureBuilder(what: Context => String, to: Option[String])
		extends HttpCaptureBuilder[HttpBodyRegExpCaptureBuilder](what, to, CompletePageReceived) {

	def newInstance(what: Context => String, to: Option[String]) = {
		new HttpBodyRegExpCaptureBuilder(what, to)
	}

	def build: HttpCapture = new HttpBodyRegExpCapture(what, to)
}