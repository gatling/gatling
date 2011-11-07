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
package com.excilys.ebi.gatling.http.capture.status.check

import com.excilys.ebi.gatling.http.capture.status.HttpStatusCapture
import com.excilys.ebi.gatling.http.capture.HttpCheck
import com.excilys.ebi.gatling.core.capture.check.InRangeCheckType

class HttpStatusCheck(to: Option[String], val expected: Option[String]) extends HttpStatusCapture(to) with HttpCheck {

	def getCheckType = InRangeCheckType

	def getExpected = expected

	override def toString = "HttpStatusCheck (Http Response Status must be in '{" + expected + "}')"
}