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

import com.fasterxml.jackson.databind.JsonNode

object JsonPathExtractors {

  def find[X: JsonFilter](name: String, path: String, occurrence: Int, jsonPaths: JsonPaths): FindCriterionExtractor[JsonNode, String, X] =
    new FindCriterionExtractor[JsonNode, String, X](
      name,
      path,
      occurrence,
      jsonPaths.extractAll(_, path).map(_.slice(occurrence, occurrence + 1).nextOption())
    )

  def findAll[X: JsonFilter](name: String, path: String, jsonPaths: JsonPaths): FindAllCriterionExtractor[JsonNode, String, X] =
    new FindAllCriterionExtractor[JsonNode, String, X](
      name,
      path,
      jsonPaths.extractAll(_, path).map(_.toVector.liftSeqOption)
    )

  def count(name: String, path: String, jsonPaths: JsonPaths): CountCriterionExtractor[JsonNode, String] =
    new CountCriterionExtractor[JsonNode, String](
      name,
      path,
      jsonPaths.extractAll[Any](_, path).map(i => Some(i.size))
    )
}
