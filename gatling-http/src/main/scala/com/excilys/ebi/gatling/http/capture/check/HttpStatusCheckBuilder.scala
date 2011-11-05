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
package com.excilys.ebi.gatling.http.capture.check

import com.excilys.ebi.gatling.core.capture.check.CheckType
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.util.StringHelper._
import com.excilys.ebi.gatling.http.request.HttpPhase._
import com.excilys.ebi.gatling.core.capture.check.EqualityCheckType

object HttpStatusCheckBuilder {
	def statusInRange(range: Range) = new HttpStatusCheckBuilder(Some(range.mkString(":")), Some(EMPTY))
	def status(status: Int) = new HttpStatusCheckBuilder(Some(status.toString), Some(EMPTY))
}

class HttpStatusCheckBuilder(to: Option[String], expected: Option[String])
		extends HttpCheckBuilder[HttpStatusCheckBuilder](None, to, Some(StatusReceived), None, expected) {

	// FIXME remove
	def newInstance(what: Option[Context => String], to: Option[String], when: Option[HttpPhase]) = {
		new HttpStatusCheckBuilder(to, None)
	}

	def newInstance(what: Option[Context => String], to: Option[String], when: Option[HttpPhase], checkType: Option[CheckType], expected: Option[String]) = {
		new HttpStatusCheckBuilder(to, expected)
	}

	def build: HttpCheck = new HttpStatusCheck(to.get, expected.get)
}
