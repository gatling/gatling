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
import com.excilys.ebi.gatling.core.check.extractor.xpath.XPathExtractor
import com.excilys.ebi.gatling.core.session.EvaluatableString
import com.excilys.ebi.gatling.core.util.ByteBufferInputStream
import com.excilys.ebi.gatling.http.check.HttpMultipleCheckBuilder
import com.excilys.ebi.gatling.http.request.HttpPhase.CompletePageReceived
import com.excilys.ebi.gatling.http.response.ExtendedResponse

object HttpBodyXPathCheckBuilder {

	private val HTTP_BODY_XPATH_EXTRACTOR_CONTEXT_KEY = "HttpBodyXPathExtractor"

	private def getCachedExtractor(response: ExtendedResponse) = {

		def newExtractor = {
			val stream = if (response.hasResponseBody) Some(new ByteBufferInputStream(response.getResponseBodyAsByteBuffer)) else None
			XPathExtractor(stream)
		}

		getOrUpdateCheckContextAttribute(HTTP_BODY_XPATH_EXTRACTOR_CONTEXT_KEY, newExtractor)
	}

	private def newFindExtractorFactory(namespaces: List[(String, String)])(occurrence: Int): ExtractorFactory[ExtendedResponse, String, String] = (response: ExtendedResponse) => getCachedExtractor(response).extractOne(occurrence, namespaces)

	private def newFindAllExtractorFactory(namespaces: List[(String, String)]): ExtractorFactory[ExtendedResponse, String, Seq[String]] = (response: ExtendedResponse) => getCachedExtractor(response).extractMultiple(namespaces)

	private def newCountExtractorFactory(namespaces: List[(String, String)]): ExtractorFactory[ExtendedResponse, String, Int] = (response: ExtendedResponse) => getCachedExtractor(response).count(namespaces)

	def xpath(expression: EvaluatableString, namespaces: List[(String, String)]) = {

		val findExtractorFactory = newFindExtractorFactory(namespaces) _
		val findAllExtractorFactory = newFindAllExtractorFactory(namespaces)
		val countExtractorFactory = newCountExtractorFactory(namespaces)

		new HttpMultipleCheckBuilder(findExtractorFactory, findAllExtractorFactory, countExtractorFactory, expression, CompletePageReceived)
	}
}
