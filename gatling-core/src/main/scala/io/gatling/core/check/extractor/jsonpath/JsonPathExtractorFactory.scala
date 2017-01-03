/**
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
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

  def newJsonPathSingleExtractor[X: JsonFilter](path: String, occurrence: Int, jsonPaths: JsonPaths) =
    newSingleExtractor(
      path,
      occurrence,
      jsonPaths.extractAll(_, path).map(_.toSeq.lift(occurrence))
    )

  def newJsonPathMultipleExtractor[X: JsonFilter](path: String, jsonPaths: JsonPaths) =
    newMultipleExtractor(
      path,
      jsonPaths.extractAll(_, path).map(_.toVector.liftSeqOption)
    )

  def newJsonPathCountExtractor(path: String, jsonPaths: JsonPaths) =
    newCountExtractor(
      path,
      jsonPaths.extractAll[Any](_, path).map(i => Some(i.size))
    )
}

object JsonPathExtractorFactory extends JsonPathExtractorFactoryBase("jsonPath")
object JsonpJsonPathExtractorFactory extends JsonPathExtractorFactoryBase("jsonpJsonPath")

@deprecated("Only used in old Async checks, will be replaced with new impl, will be removed in 3.0.0", "3.0.0-M3")
class OldJsonPathExtractorFactory(jsonPaths: JsonPaths) {

  import JsonPathExtractorFactory._

  def newSingleExtractor[X: JsonFilter](path: String, occurrence: Int) = newJsonPathSingleExtractor(path, occurrence, jsonPaths)
  def newMultipleExtractor[X: JsonFilter](path: String) = newJsonPathMultipleExtractor(path, jsonPaths)
  def newCountExtractor(path: String) = newJsonPathCountExtractor(path, jsonPaths)
}
