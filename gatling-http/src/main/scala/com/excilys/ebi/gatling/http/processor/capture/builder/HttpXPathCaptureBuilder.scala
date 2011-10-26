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
import com.excilys.ebi.gatling.http.processor.capture.HttpXPathCapture
import com.excilys.ebi.gatling.http.request.HttpPhase._

object HttpXPathCaptureBuilder {
	def xpath(expressionFormatter: Context => String) = new HttpXPathCaptureBuilder(Some(expressionFormatter), None, Some(CompletePageReceived))
	def xpath(expression: String): HttpXPathCaptureBuilder = xpath((c: Context) => expression)
	def xpath(expressionToFormat: String, interpolations: String*): HttpXPathCaptureBuilder = xpath((c: Context) => interpolateString(c, expressionToFormat, interpolations))
}
class HttpXPathCaptureBuilder(expressionFormatter: Option[Context => String], attribute: Option[String], httpPhase: Option[HttpPhase])
		extends AbstractHttpCaptureBuilder[HttpXPathCaptureBuilder](expressionFormatter, attribute, httpPhase) {

	def newInstance(expressionFormatter: Option[Context => String], attribute: Option[String], httpPhase: Option[HttpPhase]) = {
		new HttpXPathCaptureBuilder(expressionFormatter, attribute, httpPhase)
	}

	def build(): HttpCapture = new HttpXPathCapture(expressionFormatter.get, attribute.get, httpPhase.get)
}
