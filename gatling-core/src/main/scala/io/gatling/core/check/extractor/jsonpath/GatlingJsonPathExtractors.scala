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
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation }
import net.minidev.json.JSONArray
import net.minidev.json.parser.JSONParser
import io.gatling.jsonpath.jsonsmart.JsonPath

object GatlingJsonPathExtractors extends JsonPathExtractors {

	abstract class JsonPathExtractor[X] extends Extractor[Any, String, X] {
		val name = "jsonPath"
	}

	val cache = mutable.Map.empty[String, Validation[JsonPath]]

	def compile(expression: String) = JsonPath.compile(expression) match {
		case Left(error) => error.reason.failure
		case Right(path) => path.success
	}

	def cached(expression: String): Validation[JsonPath] =
		if (configuration.core.extract.jsonPath.cache) cache.getOrElseUpdate(expression, compile(expression))
		else compile(expression)

	private def extractAll(json: Any, expression: String): Validation[Iterator[String]] = {

		cached(expression).map { path =>
			path.query(json).map(_.toString)
		}
	}

	val extractOne = (occurrence: Int) => new JsonPathExtractor[String] {

		def apply(prepared: Any, criterion: String): Validation[Option[String]] =
			extractAll(prepared, criterion).map(_.toStream.liftSeqOption.flatMap(_.lift(occurrence)))
	}

	val extractMultiple = new JsonPathExtractor[Seq[String]] {

		def apply(prepared: Any, criterion: String): Validation[Option[Seq[String]]] =
			extractAll(prepared, criterion).map(_.toVector.liftSeqOption.flatMap(_.liftSeqOption))
	}

	val count = new JsonPathExtractor[Int] {

		def apply(prepared: Any, criterion: String): Validation[Option[Int]] =
			extractAll(prepared, criterion).map(i => Some(i.size))
	}
}