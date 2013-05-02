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
package com.excilys.ebi.gatling.http.check.body

import com.excilys.ebi.gatling.core.check.CheckContext.getOrUpdateCheckContextAttribute
import com.excilys.ebi.gatling.core.check.ExtractorFactory
import com.excilys.ebi.gatling.core.check.extractor.jsonpath.JsonPathExtractor
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.session.EvaluatableString
import com.excilys.ebi.gatling.http.check.HttpMultipleCheckBuilder
import com.excilys.ebi.gatling.http.request.HttpPhase.CompletePageReceived
import com.excilys.ebi.gatling.http.response.ExtendedResponse

object HttpBodyJsonPathCheckBuilder {

	private val HTTP_BODY_JSON_EXTRACTOR_CONTEXT_KEY = "HttpBodyJsonExtractor"

	private def getCachedExtractor(response: ExtendedResponse) = {

		def newExtractor = new JsonPathExtractor(response.getResponseBody(configuration.simulation.encoding))

		getOrUpdateCheckContextAttribute(HTTP_BODY_JSON_EXTRACTOR_CONTEXT_KEY, newExtractor)
	}

	private def findExtractorFactory(occurrence: Int): ExtractorFactory[ExtendedResponse, String, String] = (response: ExtendedResponse) => getCachedExtractor(response).extractOne(occurrence)

	private val findAllExtractorFactory: ExtractorFactory[ExtendedResponse, String, Seq[String]] = (response: ExtendedResponse) => getCachedExtractor(response).extractMultiple

	private val countExtractorFactory: ExtractorFactory[ExtendedResponse, String, Int] = (response: ExtendedResponse) => getCachedExtractor(response).count

	def jsonPath(expression: EvaluatableString) = new HttpMultipleCheckBuilder(findExtractorFactory, findAllExtractorFactory, countExtractorFactory, expression, CompletePageReceived)
}
