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
package com.excilys.ebi.gatling.http.check

import com.excilys.ebi.gatling.core.check.strategy.CheckStrategy
import com.excilys.ebi.gatling.core.check.CheckBuilder
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.http.request.HttpPhase.HttpPhase
import com.ning.http.client.Response

abstract class HttpCheckBuilder[B <: HttpCheckBuilder[B]](what: Context => String, to: Option[String], strategy: CheckStrategy, expected: Option[String], when: HttpPhase)
		extends CheckBuilder[Response] {

	def newInstance(what: Context => String, to: Option[String], checkType: CheckStrategy, expected: Option[String]): B

	def in(to: String) = newInstance(what, Some(to), strategy, expected)

	override def build: HttpCheck
}