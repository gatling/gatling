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
package com.excilys.ebi.gatling.http.processor.check

import com.excilys.ebi.gatling.core.processor.builtin.InRangeCheckType

import com.excilys.ebi.gatling.http.processor.capture.HttpStatusCapture

class HttpStatusCheck(val expected: String, attrKey: String)
		extends HttpStatusCapture(attrKey) with HttpCheck {

	def getCheckType = InRangeCheckType

	def getExpected = expected

	override def toString = "HttpStatusCheck (Http Response Status must be in '{" + expected + "}')"

	override def equals(that: Any) = that.isInstanceOf[HttpStatusCheck]

	override def hashCode() = 1
}