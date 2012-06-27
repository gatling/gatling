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
package com.excilys.ebi.gatling.http.check.header

import scala.collection.JavaConversions.asScalaBuffer
import com.excilys.ebi.gatling.core.check.ExtractorFactory
import com.excilys.ebi.gatling.core.check.extractor.Extractor.{ toOption, seqToOption }
import com.excilys.ebi.gatling.core.check.extractor.regex.RegexExtractor
import com.excilys.ebi.gatling.core.session.{ Session, EvaluatableString }
import com.excilys.ebi.gatling.http.check.body.HttpBodyCheckBuilder
import com.excilys.ebi.gatling.http.response.ExtendedResponse

object HttpHeaderRegexCheckBuilder {

	def headerRegex(headerName: EvaluatableString, pattern: EvaluatableString) = {

		val expression = (s: Session) => (headerName(s), pattern(s))

		new HttpBodyCheckBuilder(findExtractorFactory, findAllExtractorFactory, countExtractorFactory, expression)
	}

	private def findExtractorFactory(occurrence: Int): ExtractorFactory[ExtendedResponse, (String, String), String] =
		(response: ExtendedResponse) =>
			(headerAndPattern: (String, String)) => {
				findAllExtractorFactory(response)(headerAndPattern) match {
					case Some(results) if results.isDefinedAt(occurrence) => Some(results(occurrence))
					case _ => None
				}
			}

	private val findAllExtractorFactory: ExtractorFactory[ExtendedResponse, (String, String), Seq[String]] = (response: ExtendedResponse) =>
		(headerAndPattern: (String, String)) => {
			val (headerName, pattern) = headerAndPattern

			response.getHeaders(headerName).foldLeft(Seq.empty[String]) { (matches, header) =>
				new RegexExtractor(headerName).extractMultiple(pattern) match {
					case Some(newMatches) => newMatches ++ matches
					case None => matches
				}
			}
		}

	private val countExtractorFactory: ExtractorFactory[ExtendedResponse, (String, String), Int] =
		(response: ExtendedResponse) =>
			(headerAndPattern: (String, String)) => {
				findAllExtractorFactory(response)(headerAndPattern) match {
					case Some(results) => results.length
					case _ => 0
				}
			}
}
