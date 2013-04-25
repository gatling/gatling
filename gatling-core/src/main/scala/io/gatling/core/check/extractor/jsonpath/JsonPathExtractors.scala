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
package io.gatling.core.check.extractor.jsonpath

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.mutable

import io.gatling.core.check.Extractor
import io.gatling.core.check.extractor.Extractors.LiftedSeqOption
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.validation.{ SuccessWrapper, Validation }

object JsonPathExtractors {

	abstract class JsonPathExtractor[X] extends Extractor[Option[JsonNode], String, X] {
		val name = "jsonPath"
	}

	val cache = mutable.Map.empty[String, JsonPath]
	def cachedXPath(expression: String) = if (configuration.core.cache.jsonPath) cache.getOrElseUpdate(expression, new JsonPath(expression)) else new JsonPath(expression)

	private def extractAll(json: Option[JsonNode], expression: String): Option[Seq[String]] = json.map(new JsonPath(expression).selectNodes(_).map(_.asInstanceOf[JsonText].value))

	val extractOne = (occurrence: Int) => new JsonPathExtractor[String] {

		def apply(prepared: Option[JsonNode], criterion: String): Validation[Option[String]] =
			extractAll(prepared, criterion).flatMap(_.lift(occurrence)).success
	}

	val extractMultiple = new JsonPathExtractor[Seq[String]] {

		def apply(prepared: Option[JsonNode], criterion: String): Validation[Option[Seq[String]]] =
			extractAll(prepared, criterion).flatMap(_.liftSeqOption).success
	}

	val count = new JsonPathExtractor[Int] {

		def apply(prepared: Option[JsonNode], criterion: String): Validation[Option[Int]] =
			extractAll(prepared, criterion).map(_.size).success
	}
}