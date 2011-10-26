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

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.processor.CheckType

import com.excilys.ebi.gatling.http.request.HttpPhase._
import com.excilys.ebi.gatling.http.processor.capture.HttpXPathCapture

class HttpXPathCheck(expressionFormatter: Context => String, expected: String, attrKey: String, httpPhase: HttpPhase, checkType: CheckType)
		extends HttpXPathCapture(expressionFormatter, attrKey, httpPhase) with HttpCheck {

	def getCheckType = checkType

	def getExpected = expected

	override def equals(that: Any) = {
		if (!that.isInstanceOf[HttpXPathCheck])
			false
		else {
			val other = that.asInstanceOf[HttpXPathCheck]
			this.checkType == other.getCheckType && this.expressionFormatter == other.expressionFormatter && this.expected == other.getExpected
		}
	}

	override def hashCode = this.expressionFormatter.hashCode + this.expected.size + this.getCheckType.hashCode
}