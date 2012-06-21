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
package com.excilys.ebi.gatling.http.check.body

import com.excilys.ebi.gatling.core.check.CheckContext.getOrUpdateCheckContextAttribute
import com.excilys.ebi.gatling.core.check.extractor.xpath.XPathExtractor
import com.excilys.ebi.gatling.core.check.ExtractorFactory
import com.excilys.ebi.gatling.core.session.EvaluatableString
import com.ning.http.client.Response

object HttpBodyXPathCheckBuilder {

	def xpath(expression: EvaluatableString, namespaces: List[(String, String)]) = new HttpBodyCheckBuilder(findExtractorFactory(namespaces), findAllExtractorFactory(namespaces), countExtractorFactory(namespaces), expression)

	private val HTTP_BODY_XPATH_EXTRACTOR_CONTEXT_KEY = "HttpBodyXPathExtractor"

	private def getCachedExtractor(response: Response) = getOrUpdateCheckContextAttribute(HTTP_BODY_XPATH_EXTRACTOR_CONTEXT_KEY, XPathExtractor(response.getResponseBodyAsStream))

	private def findExtractorFactory(namespaces: List[(String, String)])(occurrence: Int): ExtractorFactory[Response, String, String] = (response: Response) => getCachedExtractor(response).extractOne(occurrence, namespaces)

	private def findAllExtractorFactory(namespaces: List[(String, String)]): ExtractorFactory[Response, String, Seq[String]] = (response: Response) => getCachedExtractor(response).extractMultiple(namespaces)

	private def countExtractorFactory(namespaces: List[(String, String)]): ExtractorFactory[Response, String, Int] = (response: Response) => getCachedExtractor(response).count(namespaces)
}
