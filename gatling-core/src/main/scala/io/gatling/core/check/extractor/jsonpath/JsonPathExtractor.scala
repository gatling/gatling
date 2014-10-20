/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import io.gatling.core.check.extractor._
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.util.cache._
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation }
import io.gatling.jsonpath.JsonPath

object JsonPathExtractor {

  val JsonPathCacheEnabled = configuration.core.extract.jsonPath.cacheMaxCapacity > 0
  val JsonPathCache = ThreadSafeCache[String, Validation[JsonPath]](configuration.core.extract.jsonPath.cacheMaxCapacity)
}

abstract class JsonPathExtractor[X] extends CriterionExtractor[Any, String, X] {

  import JsonPathExtractor._

  val criterionName = "jsonPath"

  def extractAll[F: JsonFilter](json: Any, expression: String): Validation[Iterator[F]] =
    compileJsonPath(expression).map(_.query(json).collect(implicitly[JsonFilter[F]].filter))

  def compileJsonPath(expression: String): Validation[JsonPath] = {

      def compile(expression: String): Validation[JsonPath] = JsonPath.compile(expression) match {
        case Left(error) => error.reason.failure
        case Right(path) => path.success
      }

    if (JsonPathCacheEnabled)
      JsonPathCache.getOrElsePutIfAbsent(expression, compile(expression))
    else
      compile(expression)
  }
}

class SingleJsonPathExtractor[X: JsonFilter](val criterion: String, val occurrence: Int) extends JsonPathExtractor[X] with FindArity {

  def extract(prepared: Any): Validation[Option[X]] =
    extractAll(prepared, criterion).map(_.toSeq.lift(occurrence))
}

class MultipleJsonPathExtractor[X: JsonFilter](val criterion: String) extends JsonPathExtractor[Seq[X]] with FindAllArity {

  def extract(prepared: Any): Validation[Option[Seq[X]]] =
    extractAll(prepared, criterion).map(_.toVector.liftSeqOption)
}

class CountJsonPathExtractor(val criterion: String) extends JsonPathExtractor[Int] with CountArity {

  def extract(prepared: Any): Validation[Option[Int]] =
    extractAll[Any](prepared, criterion).map(i => Some(i.size))
}
