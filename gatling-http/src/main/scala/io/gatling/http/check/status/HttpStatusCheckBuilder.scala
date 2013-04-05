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
package io.gatling.http.check.status

import io.gatling.core.check.Extractor
import io.gatling.core.session.noopStringExpression
import io.gatling.core.validation.SuccessWrapper
import io.gatling.http.check.{ HttpCheckBuilders, HttpSingleCheckBuilder }
import io.gatling.http.response.ExtendedResponse

object HttpStatusCheckBuilder {

	val statusExtractor = new Extractor[ExtendedResponse, String, Int] {
		val name = "status"
		def apply(prepared: ExtendedResponse, criterion: String) = Some(prepared.getStatusCode).success
	}

	val status = new HttpSingleCheckBuilder[ExtendedResponse, String, Int](
		HttpCheckBuilders.statusCheckFactory,
		HttpCheckBuilders.noopResponsePreparer,
		statusExtractor,
		noopStringExpression)
}