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

import com.jayway.jsonpath.{ InvalidPathException, JsonPath }

import io.gatling.core.check.Extractor
import io.gatling.core.check.extractor.Extractors.LiftedSeqOption
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.validation.{ SuccessWrapper, Validation }
import net.minidev.json.JSONArray
import net.minidev.json.parser.JSONParser

object JaywayJsonPathExtractors {

	def parse(bytes: Array[Byte]) = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(bytes)

	abstract class JsonPathExtractor[X] extends Extractor[Any, String, X] {
		val name = "jsonPath"
	}

	val cache = mutable.Map.empty[String, JsonPath]
	def cached(expression: String): JsonPath =
		if (configuration.core.extract.jsonPath.cache) cache.getOrElseUpdate(expression, JsonPath.compile(expression))
		else JsonPath.compile(expression)

	private def extractAll(json: Any, expression: String): Option[Seq[String]] = {

		try {
			cached(expression).read[Any](json) match {
				case null => None // can't turn result into an Option as we want to turn empty Seq into None (see below)
				case array: JSONArray => array.map(_.toString).liftSeqOption
				case other => Some(List(other.toString))
			}
		} catch {
			case e: InvalidPathException => None
		}
	}

	val extractOne = (occurrence: Int) => new JsonPathExtractor[String] {

		def apply(prepared: Any, criterion: String): Validation[Option[String]] =
			extractAll(prepared, criterion).flatMap(_.lift(occurrence)).success
	}

	val extractMultiple = new JsonPathExtractor[Seq[String]] {

		def apply(prepared: Any, criterion: String): Validation[Option[Seq[String]]] =
			extractAll(prepared, criterion).flatMap(_.liftSeqOption).success
	}

	val count = new JsonPathExtractor[Int] {

		def apply(prepared: Any, criterion: String): Validation[Option[Int]] =
			extractAll(prepared, criterion).map(_.size).success
	}
}