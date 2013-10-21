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
package io.gatling.http.check.header

import io.gatling.core.check.Extractor
import io.gatling.core.check.extractor.Extractors.LiftedSeqOption
import io.gatling.core.check.extractor.regex.{ GroupExtractor, RegexExtractors }
import io.gatling.core.validation.{ SuccessWrapper, Validation }
import io.gatling.http.response.Response

object HttpHeaderRegexExtractors {

	abstract class HeaderRegexExtractor[X] extends Extractor[Response, (String, String), X] {
		val name = "headerRegex"
	}

	def extractHeadersValues[X](response: Response, headerNameAndPattern: (String, String))(implicit groupExtractor: GroupExtractor[X]) = {
		val (headerName, pattern) = headerNameAndPattern
		val headerValues = HttpHeaderExtractors.decodedHeaders(response, headerName)
		headerValues.map(RegexExtractors.extract(_, pattern)).flatten
	}

	def extractOne[X](occurrence: Int)(implicit groupExtractor: GroupExtractor[X]) = new HeaderRegexExtractor[X] {

		def apply(prepared: Response, criterion: (String, String)): Validation[Option[X]] =
			extractHeadersValues(prepared, criterion).lift(occurrence).success
	}

	def extractMultiple[X](implicit groupExtractor: GroupExtractor[X]) = new HeaderRegexExtractor[Seq[X]] {

		def apply(prepared: Response, criterion: (String, String)): Validation[Option[Seq[X]]] =
			extractHeadersValues(prepared, criterion).liftSeqOption.success
	}

	val count = new HeaderRegexExtractor[Int] {

		def apply(prepared: Response, criterion: (String, String)): Validation[Option[Int]] =
			extractHeadersValues[String](prepared, criterion).liftSeqOption.map(_.size).success
	}
}