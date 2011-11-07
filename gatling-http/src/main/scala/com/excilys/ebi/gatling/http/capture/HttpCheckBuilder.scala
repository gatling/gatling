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
package com.excilys.ebi.gatling.http.capture

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.http.request.HttpPhase.HttpPhase
import com.excilys.ebi.gatling.core.capture.check.CheckType

abstract class HttpCheckBuilder[B <: HttpCheckBuilder[B]](what: Context => String, to: Option[String], when: HttpPhase, val checkType: CheckType, val expected: Option[String])
		extends HttpCaptureBuilder[B](what, to, when) {

	def newInstance(what: Context => String, to: Option[String]) = {
		newInstance(what, to, checkType, expected)
	}

	def newInstance(what: Context => String, to: Option[String], checkType: CheckType, expected: Option[String]): B

	override def build: HttpCheck
}