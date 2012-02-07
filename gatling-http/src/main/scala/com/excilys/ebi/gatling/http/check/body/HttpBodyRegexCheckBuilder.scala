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
import scala.annotation.implicitNotFound

import com.excilys.ebi.gatling.core.check.CheckContext.{setAndReturnCheckContextAttribute, getCheckContextAttribute}
import com.excilys.ebi.gatling.core.check.extractor.RegexExtractor
import com.excilys.ebi.gatling.core.session.Session
import com.ning.http.client.Response

object HttpBodyRegexCheckBuilder {

	def regex(expression: Session => String) = new HttpBodyCheckBuilder(findExtractorFactory, findAllExtractoryFactory, countExtractoryFactory, expression)

	private val HTTP_BODY_REGEX_EXTRACTOR_CONTEXT_KEY = "HttpBodyRegexExtractor"

	private def getCachedExtractor(response: Response) = getCheckContextAttribute(HTTP_BODY_REGEX_EXTRACTOR_CONTEXT_KEY).getOrElse {
		setAndReturnCheckContextAttribute(HTTP_BODY_REGEX_EXTRACTOR_CONTEXT_KEY, new RegexExtractor(response.getResponseBody))
	}

	private def findExtractorFactory(occurrence: Int) = (response: Response) => getCachedExtractor(response).extractOne(occurrence)(_)
	private val findAllExtractoryFactory = (response: Response) => getCachedExtractor(response).extractMultiple(_)
	private val countExtractoryFactory = (response: Response) => getCachedExtractor(response).count(_)
}