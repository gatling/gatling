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

import com.jayway.jsonpath.JsonPath

import io.gatling.core.check.Extractor
import io.gatling.core.check.extractor.Extractors.LiftedSeqOption
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.validation.{ SuccessWrapper, Validation }
import net.minidev.json.JSONArray

object JsonPathExtractors {

	abstract class JsonPathExtractor[X] extends Extractor[String, String, X] {
		val name = "jsonPath"
	}

	val cache = mutable.Map.empty[String, JsonPath]
	def cachedXPath(expression: String): JsonPath = if (configuration.core.cache.jsonPath) cache.getOrElseUpdate(expression, JsonPath.compile(expression)) else JsonPath.compile(expression)

	private def extractAll(json: String, expression: String): Option[Seq[String]] = {

		val result: Any = cachedXPath(expression).read(json)
		result match {
			case null => None // can't turn result into an Option as we want to turn empty Seq into None (see below)
			case array: JSONArray => array.map(_.toString).liftSeqOption
			case other => Some(List(other.toString))
		}
	}

	val extractOne = (occurrence: Int) => new JsonPathExtractor[String] {

		def apply(prepared: String, criterion: String): Validation[Option[String]] =
			extractAll(prepared, criterion).flatMap(_.lift(occurrence)).success
	}

	val extractMultiple = new JsonPathExtractor[Seq[String]] {

		def apply(prepared: String, criterion: String): Validation[Option[Seq[String]]] =
			extractAll(prepared, criterion).flatMap(_.liftSeqOption).success
	}

	val count = new JsonPathExtractor[Int] {

		def apply(prepared: String, criterion: String): Validation[Option[Int]] =
			extractAll(prepared, criterion).map(_.size).success
	}
}