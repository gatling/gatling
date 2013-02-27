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

import java.net.URLDecoder

import scala.collection.JavaConversions.asScalaBuffer

import com.excilys.ebi.gatling.core.check.Extractor
import com.excilys.ebi.gatling.core.check.extractor.Extractors
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.http.Headers
import com.excilys.ebi.gatling.http.response.ExtendedResponse

import scalaz.Scalaz.ToValidationV
import scalaz.Validation

object HttpHeaderExtractors extends Extractors {

	abstract class HeaderExtractor[X] extends Extractor[ExtendedResponse, String, X] {
		val name = "header"
	}

	private def decode(headerName: String, headerValue: String) =
		if (headerName == Headers.Names.LOCATION)
			URLDecoder.decode(headerValue, configuration.simulation.encoding)
		else
			headerValue

	def decodedHeaders(response: ExtendedResponse, headerName: String): Seq[String] = response.getHeadersSafe(headerName).map(decode(headerName, _))

	val extractOne = (occurrence: Int) => new HeaderExtractor[String] {

		def apply(prepared: ExtendedResponse, criterion: String): Validation[String, Option[String]] =
			prepared.getHeadersSafe(criterion).lift(occurrence).map(decode(criterion, _)).success
	}

	val extractMultiple = new HeaderExtractor[Seq[String]] {

		def apply(prepared: ExtendedResponse, criterion: String): Validation[String, Option[Seq[String]]] =
			decodedHeaders(prepared, criterion).liftSeqOption.success
	}

	val count = new HeaderExtractor[Int] {

		def apply(prepared: ExtendedResponse, criterion: String): Validation[String, Option[Int]] =
			prepared.getHeadersSafe(criterion).liftSeqOption.map(_.size).success
	}
}