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
package com.excilys.ebi.gatling.http.check.status

import com.excilys.ebi.gatling.core.check.ExtractorFactory
import com.excilys.ebi.gatling.core.session.NOOP_EVALUATABLE_STRING
import com.excilys.ebi.gatling.http.check.HttpSingleCheckBuilder
import com.excilys.ebi.gatling.http.request.HttpPhase.StatusReceived
import com.excilys.ebi.gatling.http.response.ExtendedResponse

/**
 * Builder for current location (ie current request URL) check
 */
object CurrentLocationCheckBuilder {

	private val findExtractorFactory: ExtractorFactory[ExtendedResponse, String, String] = (response: ExtendedResponse) => (unused: String) => Some(response.request.getUrl)

	val currentLocation = new HttpSingleCheckBuilder(findExtractorFactory, NOOP_EVALUATABLE_STRING, StatusReceived)
}