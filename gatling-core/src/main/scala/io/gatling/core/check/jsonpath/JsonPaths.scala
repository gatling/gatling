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

import io.gatling.commons.validation._
import io.gatling.core.util.cache.Cache
import io.gatling.jsonpath.JsonPath

import com.fasterxml.jackson.databind.JsonNode

class JsonPaths(cacheMaxCapacity: Long) {

  private val jsonPathCache = {
    def compile(expression: String): Validation[JsonPath] = JsonPath.compile(expression) match {
      case Left(error) => error.reason.failure
      case Right(path) => path.success
    }

    Cache.newConcurrentLoadingCache(cacheMaxCapacity, compile)
  }

  def extractAll[X: JsonFilter](json: JsonNode, expression: String): Validation[Iterator[X]] =
    compileJsonPath(expression).map(_.query(json).collect(JsonFilter[X].filter))

  def compileJsonPath(expression: String): Validation[JsonPath] = jsonPathCache.get(expression)
}
