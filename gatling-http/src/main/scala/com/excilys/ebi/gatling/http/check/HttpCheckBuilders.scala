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
package com.excilys.ebi.gatling.http.check

import com.excilys.ebi.gatling.core.check.{ Check, CheckFactory, Preparer }
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.validation.SuccessWrapper
import com.excilys.ebi.gatling.http.request.HttpPhase._
import com.excilys.ebi.gatling.http.response.ExtendedResponse

object HttpCheckBuilders {

	private def httpCheckFactory(phase: HttpPhase): CheckFactory[HttpCheck, ExtendedResponse] = (wrapped: Check[ExtendedResponse]) => HttpCheck(wrapped, phase)

	val statusReceivedCheckFactory = httpCheckFactory(StatusReceived)
	val headersReceivedCheckFactory = httpCheckFactory(HeadersReceived)
	val bodyPartReceivedCheckFactory = httpCheckFactory(BodyPartReceived)
	val completePageReceivedCheckFactory = httpCheckFactory(CompletePageReceived)
	val afterResponseReceivedCheckFactory = httpCheckFactory(AfterResponseReceived)

	val noopResponsePreparer: Preparer[ExtendedResponse, ExtendedResponse] = (r: ExtendedResponse) => r.success
	val stringResponsePreparer: Preparer[ExtendedResponse, String] = (response: ExtendedResponse) => response.getResponseBody(configuration.simulation.encoding).success
}