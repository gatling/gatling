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
package com.excilys.ebi.gatling.http.check.status

import com.excilys.ebi.gatling.core.check.Extractor
import com.excilys.ebi.gatling.core.session.noopStringExpression
import com.excilys.ebi.gatling.core.validation.SuccessWrapper
import com.excilys.ebi.gatling.http.check.{ HttpCheckBuilders, HttpSingleCheckBuilder }
import com.excilys.ebi.gatling.http.response.ExtendedResponse

/**
 * Builder for current location (ie current request URL) check
 */
object CurrentLocationCheckBuilder {

	val currentLocationExtractor = new Extractor[ExtendedResponse, String, String] {
		val name = "currentLocation"
		def apply(prepared: ExtendedResponse, criterion: String) = Some(prepared.request.getUrl).success
	}

	val currentLocation = new HttpSingleCheckBuilder[ExtendedResponse, String, String](
		HttpCheckBuilders.statusReceivedCheckFactory,
		HttpCheckBuilders.noopResponsePreparer,
		currentLocationExtractor,
		noopStringExpression)
}