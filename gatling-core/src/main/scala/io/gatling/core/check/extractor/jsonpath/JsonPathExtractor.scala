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

import scala.collection.JavaConversions.mapAsScalaConcurrentMap
import scala.collection.concurrent

import com.fasterxml.jackson.core.JsonParser.Feature
import com.fasterxml.jackson.databind.ObjectMapper

import io.gatling.core.check.extractor.{ CriterionExtractor, LiftedSeqOption }
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation }
import io.gatling.jsonpath.JsonPath
import jsr166e.ConcurrentHashMapV8

object JsonPathExtractor {

	val cache: concurrent.Map[String, Validation[JsonPath]] = new ConcurrentHashMapV8[String, Validation[JsonPath]]

	def cached(expression: String): Validation[JsonPath] =
		if (configuration.core.extract.jsonPath.cache) cache.getOrElseUpdate(expression, compile(expression))
		else compile(expression)

	def compile(expression: String): Validation[JsonPath] = JsonPath.compile(expression) match {
		case Left(error) => error.reason.failure
		case Right(path) => path.success
	}

	def extractAll[X: JsonFilter](json: Any, expression: String): Validation[Iterator[X]] =
		cached(expression).map(_.query(json).collect(implicitly[JsonFilter[X]].filter))
}

abstract class JsonPathExtractor[X] extends CriterionExtractor[Any, String, X] { val criterionName = "jsonPath" }

class SingleJsonPathExtractor[X: JsonFilter](val criterion: String, occurrence: Int) extends JsonPathExtractor[X] {

	def extract(prepared: Any): Validation[Option[X]] =
		JsonPathExtractor.extractAll(prepared, criterion).map(_.toStream.liftSeqOption.flatMap(_.lift(occurrence)))
}

class MultipleJsonPathExtractor[X: JsonFilter](val criterion: String) extends JsonPathExtractor[Seq[X]] {

	def extract(prepared: Any): Validation[Option[Seq[X]]] =
		JsonPathExtractor.extractAll(prepared, criterion).map(_.toVector.liftSeqOption.flatMap(_.liftSeqOption))
}

class CountJsonPathExtractor(val criterion: String) extends JsonPathExtractor[Int] {

	def extract(prepared: Any): Validation[Option[Int]] =
		JsonPathExtractor.extractAll[Any](prepared, criterion).map(i => Some(i.size))
}
