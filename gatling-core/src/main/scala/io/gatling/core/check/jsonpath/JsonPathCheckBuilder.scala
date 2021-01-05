/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

package io.gatling.core.check.jsonpath

import io.gatling.core.check._
import io.gatling.core.session._

import com.fasterxml.jackson.databind.JsonNode

abstract class JsonPathCheckBuilderBase[T, X: JsonFilter](
    name: String,
    private[jsonpath] val path: Expression[String],
    private[jsonpath] val jsonPaths: JsonPaths
) extends DefaultMultipleFindCheckBuilder[T, JsonNode, X](displayActualValue = true) {

  override protected def findExtractor(occurrence: Int): Expression[Extractor[JsonNode, X]] =
    path.map(JsonPathExtractors.find[X](name, _, occurrence, jsonPaths))
  override protected def findAllExtractor: Expression[Extractor[JsonNode, Seq[X]]] = path.map(JsonPathExtractors.findAll[X](name, _, jsonPaths))
  override protected def countExtractor: Expression[Extractor[JsonNode, Int]] = path.map(JsonPathExtractors.count(name, _, jsonPaths))
}

trait JsonPathCheckType

trait JsonPathOfType { self: JsonPathCheckBuilder[String] =>

  def ofType[X: JsonFilter]: MultipleFindCheckBuilder[JsonPathCheckType, JsonNode, X] = new JsonPathCheckBuilder[X](path, jsonPaths)
}

object JsonPathCheckBuilder {

  def jsonPath(path: Expression[String], jsonPaths: JsonPaths): JsonPathCheckBuilder[String] with JsonPathOfType =
    new JsonPathCheckBuilder[String](path, jsonPaths) with JsonPathOfType
}

class JsonPathCheckBuilder[X: JsonFilter](
    path: Expression[String],
    jsonPaths: JsonPaths
) extends JsonPathCheckBuilderBase[JsonPathCheckType, X]("jsonPath", path, jsonPaths)
