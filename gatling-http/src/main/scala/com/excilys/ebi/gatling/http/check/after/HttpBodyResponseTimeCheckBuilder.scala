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

import com.excilys.ebi.gatling.core.check.{ MatcherCheckBuilder, ExtractorFactory }
import com.excilys.ebi.gatling.core.util.StringHelper.EMPTY
import com.excilys.ebi.gatling.http.response.ExtendedResponse
import com.excilys.ebi.gatling.http.check.{ HttpExtractorCheckBuilder, HttpCheck }
import com.excilys.ebi.gatling.http.request.HttpPhase.AfterResponseReceived

/**
 * HttpBodyesponseTimeCheckBuilder class companion
 *
 * It contains DSL definitions
 */
object HttpBodyResponseTimeCheckBuilder {

	private val findExtendedResponseTimeExtractorFactory: ExtractorFactory[ExtendedResponse, String, Long] = (response: ExtendedResponse) => (expression: String) => Some(response.reponseTimeInMillis)

	private val findLatencyExtractorFactory: ExtractorFactory[ExtendedResponse, String, Long] = (response: ExtendedResponse) => (expression: String) => Some(response.latencyInMillis)

	def responseTimeInMillis = new HttpBodyResponseTimeCheckBuilder(findExtendedResponseTimeExtractorFactory)

	def latencyInMillis = new HttpBodyResponseTimeCheckBuilder(findLatencyExtractorFactory)
}

/**
 * This class builds a response time check
 */
class HttpBodyResponseTimeCheckBuilder(factory: ExtractorFactory[ExtendedResponse, String, Long]) extends HttpExtractorCheckBuilder[Long, String](Session => EMPTY, AfterResponseReceived) {

	def find = new MatcherCheckBuilder[HttpCheck[String], ExtendedResponse, String, Long](httpCheckBuilderFactory, factory)
}