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
package com.excilys.ebi.gatling.http.check.header

import com.excilys.ebi.gatling.core.check.Extractor
import com.excilys.ebi.gatling.core.check.extractor.Extractors
import com.excilys.ebi.gatling.core.check.extractor.regex.RegexExtractors
import com.excilys.ebi.gatling.http.response.ExtendedResponse

import scalaz.Scalaz.ToValidationV
import scalaz.Validation

object HttpHeaderRegexExtractors extends Extractors {

	abstract class HeaderRegexExtractor[X] extends Extractor[ExtendedResponse, (String, String), X] {
		val name = "headerRegex"
	}

	def extractHeadersValues(response: ExtendedResponse, headerNameAndPattern: (String, String)) = {
		val (headerName, pattern) = headerNameAndPattern
		val headerValues = HttpHeaderExtractors.decodedHeaders(response, headerName)
		headerValues.map(RegexExtractors.extract(_, pattern)).flatten
	}

	val extractOne = (occurrence: Int) => new HeaderRegexExtractor[String] {

		def apply(prepared: ExtendedResponse, criterion: (String, String)): Validation[String, Option[String]] =
			extractHeadersValues(prepared, criterion).lift(occurrence).success
	}

	val extractMultiple = new HeaderRegexExtractor[Seq[String]] {

		def apply(prepared: ExtendedResponse, criterion: (String, String)): Validation[String, Option[Seq[String]]] =
			extractHeadersValues(prepared, criterion).liftSeqOption.success
	}

	val count = new HeaderRegexExtractor[Int] {

		def apply(prepared: ExtendedResponse, criterion: (String, String)): Validation[String, Option[Int]] =
			extractHeadersValues(prepared, criterion).liftSeqOption.map(_.size).success
	}
}