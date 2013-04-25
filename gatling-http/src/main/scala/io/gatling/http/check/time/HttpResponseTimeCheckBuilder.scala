/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.http.check.time

import io.gatling.core.check.Extractor
import io.gatling.core.session.noopStringExpression
import io.gatling.core.validation.SuccessWrapper
import io.gatling.http.check.{ HttpCheckBuilders, HttpSingleCheckBuilder }
import io.gatling.http.response.Response

object HttpResponseTimeCheckBuilder {

	val responseTimeInMillis = apply(new Extractor[Response, String, Long] {
		val name = "responseTime"
		def apply(prepared: Response, criterion: String) = Some(prepared.reponseTimeInMillis).success
	})

	val latencyInMillis = apply(new Extractor[Response, String, Long] {
		val name = "latency"
		def apply(prepared: Response, criterion: String) = Some(prepared.latencyInMillis).success
	})

	def apply(extractor: Extractor[Response, String, Long]) = new HttpSingleCheckBuilder[Response, String, Long](
		HttpCheckBuilders.timeCheckFactory,
		HttpCheckBuilders.noopResponsePreparer,
		extractor,
		noopStringExpression)
}