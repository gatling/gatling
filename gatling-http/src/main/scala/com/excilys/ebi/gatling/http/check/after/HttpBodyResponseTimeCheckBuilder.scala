/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.http.check.after

import com.excilys.ebi.gatling.core.check.Extractor
import com.excilys.ebi.gatling.core.session.noopStringExpression
import com.excilys.ebi.gatling.http.check.{ HttpCheckBuilders, HttpSingleCheckBuilder }
import com.excilys.ebi.gatling.http.response.ExtendedResponse

import scalaz.Scalaz.ToValidationV

object HttpBodyResponseTimeCheckBuilder {

	val responseTimeInMillis = new HttpBodyResponseTimeCheckBuilder(new Extractor[ExtendedResponse, String, Long] {
		val name = "responseTime"
		def apply(prepared: ExtendedResponse, criterion: String) = Some(prepared.reponseTimeInMillis).success
	})

	val latencyInMillis = new HttpBodyResponseTimeCheckBuilder(new Extractor[ExtendedResponse, String, Long] {
		val name = "latency"
		def apply(prepared: ExtendedResponse, criterion: String) = Some(prepared.latencyInMillis).success
	})
}

class HttpBodyResponseTimeCheckBuilder(extractor: Extractor[ExtendedResponse, String, Long]) extends HttpSingleCheckBuilder[ExtendedResponse, String, Long](
	HttpCheckBuilders.afterResponseReceivedCheckFactory,
	HttpCheckBuilders.noopResponsePreparer,
	extractor,
	noopStringExpression)