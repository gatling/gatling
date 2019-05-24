/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.core.check.extractor.jsonpath

import io.gatling.core.check.extractor._

sealed abstract class JsonPathExtractorFactoryBase(name: String) extends CriterionExtractorFactory[Any, String](name) {

  private def lift[T](it: Iterator[T], i: Int): Option[T] = {
    var j = 0
    while (it.hasNext) {
      val value = it.next()
      if (i == j) {
        return Some(value)
      }
      j += 1
    }
    None
  }

  def newJsonPathSingleExtractor[X: JsonFilter](path: String, occurrence: Int, jsonPaths: JsonPaths): CriterionExtractor[Any, String, X] with FindArity =
    newSingleExtractor(
      path,
      occurrence,
      jsonPaths.extractAll(_, path).map(lift(_, occurrence))
    )

  def newJsonPathMultipleExtractor[X: JsonFilter](path: String, jsonPaths: JsonPaths): CriterionExtractor[Any, String, Seq[X]] with FindAllArity =
    newMultipleExtractor(
      path,
      jsonPaths.extractAll(_, path).map(_.toVector.liftSeqOption)
    )

  def newJsonPathCountExtractor(path: String, jsonPaths: JsonPaths): CriterionExtractor[Any, String, Int] with CountArity =
    newCountExtractor(
      path,
      jsonPaths.extractAll[Any](_, path).map(i => Some(i.size))
    )
}

object JsonPathExtractorFactory extends JsonPathExtractorFactoryBase("jsonPath")
object JsonpJsonPathExtractorFactory extends JsonPathExtractorFactoryBase("jsonpJsonPath")
