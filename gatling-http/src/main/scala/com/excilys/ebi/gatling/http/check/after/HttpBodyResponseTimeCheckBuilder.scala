/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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

import com.excilys.ebi.gatling.core.check.{ ExtractorFactory, MatcherCheckBuilder }
import com.excilys.ebi.gatling.core.session.NOOP_EVALUATABLE_STRING
import com.excilys.ebi.gatling.http.check.{ HttpCheck, HttpExtractorCheckBuilder }
import com.excilys.ebi.gatling.http.request.HttpPhase.AfterResponseReceived
import com.excilys.ebi.gatling.http.response.ExtendedResponse

/**
 * HttpBodyesponseTimeCheckBuilder class companion
 *
 * It contains DSL definitions
 */
object HttpBodyResponseTimeCheckBuilder {

	private val findExtendedResponseTimeExtractorFactory: ExtractorFactory[ExtendedResponse, String, Long] = (response: ExtendedResponse) => (expression: String) => Some(response.reponseTimeInMillis)

	private val findLatencyExtractorFactory: ExtractorFactory[ExtendedResponse, String, Long] = (response: ExtendedResponse) => (expression: String) => Some(response.latencyInMillis)

	val responseTimeInMillis = new HttpBodyResponseTimeCheckBuilder(findExtendedResponseTimeExtractorFactory)

	val latencyInMillis = new HttpBodyResponseTimeCheckBuilder(findLatencyExtractorFactory)
}

/**
 * This class builds a response time check
 */
class HttpBodyResponseTimeCheckBuilder(factory: ExtractorFactory[ExtendedResponse, String, Long]) extends HttpExtractorCheckBuilder[Long, String](NOOP_EVALUATABLE_STRING, AfterResponseReceived) {

	def find = new MatcherCheckBuilder[HttpCheck[String], ExtendedResponse, String, Long](httpCheckBuilderFactory, factory)
}