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
package com.excilys.ebi.gatling.core.check.extractor.jsonpath

import scala.collection.JavaConversions.asScalaBuffer

import com.excilys.ebi.gatling.core.check.Extractor
import com.excilys.ebi.gatling.core.check.extractor.Extractors.LiftedSeqOption
import com.excilys.ebi.gatling.core.validation.{ SuccessWrapper, Validation }

object JsonPathExtractors {

	abstract class CssExtractor[X] extends Extractor[Option[JsonNode], String, X] {
		val name = "jsonPath"
	}

	private def extractAll(json: Option[JsonNode], expression: String): Option[Seq[String]] = json.map(new JsonPath(expression).selectNodes(_).map(_.asInstanceOf[JsonText].value))

	val extractOne = (occurrence: Int) => new CssExtractor[String] {

		def apply(prepared: Option[JsonNode], criterion: String): Validation[Option[String]] =
			extractAll(prepared, criterion).flatMap(_.lift(occurrence)).success
	}

	val extractMultiple = new CssExtractor[Seq[String]] {

		def apply(prepared: Option[JsonNode], criterion: String): Validation[Option[Seq[String]]] =
			extractAll(prepared, criterion).flatMap(_.liftSeqOption).success
	}

	val count = new CssExtractor[Int] {

		def apply(prepared: Option[JsonNode], criterion: String): Validation[Option[Int]] =
			extractAll(prepared, criterion).map(_.size).success
	}
}