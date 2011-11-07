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
package com.excilys.ebi.gatling.http.check.status

import com.excilys.ebi.gatling.core.check.checktype.InRangeCheckType.rangeToString
import com.excilys.ebi.gatling.core.check.checktype.{ InRangeCheckType, EqualityCheckType, CheckType }
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY
import com.excilys.ebi.gatling.http.check.{ HttpCheckBuilder, HttpCheck }
import com.excilys.ebi.gatling.http.request.HttpPhase.StatusReceived

object HttpStatusCheckBuilder {
	def statusInRange(range: Range) = new HttpStatusCheckBuilder(None, InRangeCheckType, Some(range))
	def status(status: Int) = new HttpStatusCheckBuilder(None, EqualityCheckType, Some(status.toString))
}

class HttpStatusCheckBuilder(to: Option[String], checkType: CheckType, expected: Option[String])
		extends HttpCheckBuilder[HttpStatusCheckBuilder]((c: Context) => EMPTY, to, checkType, expected, StatusReceived) {

	def newInstance(what: Context => String, to: Option[String], checkType: CheckType, expected: Option[String]) = {
		new HttpStatusCheckBuilder(to, checkType, expected)
	}

	def build: HttpCheck = new HttpStatusCheck(to, expected)
}
