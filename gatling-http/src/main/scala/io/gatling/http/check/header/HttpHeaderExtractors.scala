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

import java.net.URLDecoder

import io.gatling.core.check.Extractor
import io.gatling.core.check.extractor.Extractors.LiftedSeqOption
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.validation.{ SuccessWrapper, Validation }
import io.gatling.http.HeaderNames
import io.gatling.http.response.Response

object HttpHeaderExtractors {

	abstract class HeaderExtractor[X] extends Extractor[Response, String, X] {
		val name = "header"
	}

	def decode(headerName: String, headerValue: String) =
		if (headerName == HeaderNames.LOCATION)
			URLDecoder.decode(headerValue, configuration.core.encoding)
		else
			headerValue

	def decodedHeaders(response: Response, headerName: String): Seq[String] = response.getHeadersSafe(headerName).map(decode(headerName, _))

	val extractOne = (occurrence: Int) => new HeaderExtractor[String] {

		def apply(prepared: Response, criterion: String): Validation[Option[String]] =
			prepared.getHeadersSafe(criterion).lift(occurrence).map(decode(criterion, _)).success
	}

	val extractMultiple = new HeaderExtractor[Seq[String]] {

		def apply(prepared: Response, criterion: String): Validation[Option[Seq[String]]] =
			decodedHeaders(prepared, criterion).liftSeqOption.success
	}

	val count = new HeaderExtractor[Int] {

		def apply(prepared: Response, criterion: String): Validation[Option[Int]] =
			prepared.getHeadersSafe(criterion).liftSeqOption.map(_.size).success
	}
}