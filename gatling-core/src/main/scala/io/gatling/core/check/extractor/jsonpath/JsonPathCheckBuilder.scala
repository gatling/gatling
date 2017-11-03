/*
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

import io.gatling.core.check._
import io.gatling.core.session._

trait JsonPathCheckType

trait JsonPathOfType { self: JsonPathCheckBuilder[String] =>

  def ofType[X: JsonFilter] = new JsonPathCheckBuilder[X](path, jsonPaths)
}

object JsonPathCheckBuilder {

  def jsonPath(path: Expression[String], jsonPaths: JsonPaths) =
    new JsonPathCheckBuilder[String](path, jsonPaths) with JsonPathOfType
}

class JsonPathCheckBuilder[X: JsonFilter](
    private[jsonpath] val path:      Expression[String],
    private[jsonpath] val jsonPaths: JsonPaths
)
  extends DefaultMultipleFindCheckBuilder[JsonPathCheckType, Any, X] {

  import JsonPathExtractorFactory._

  override def findExtractor(occurrence: Int) = path.map(newJsonPathSingleExtractor[X](_, occurrence, jsonPaths))
  override def findAllExtractor = path.map(newJsonPathMultipleExtractor[X](_, jsonPaths))
  override def countExtractor = path.map(newJsonPathCountExtractor(_, jsonPaths))
}
