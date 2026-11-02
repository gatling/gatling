/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

package io.gatling.http.check.body

import io.gatling.commons.validation.Validation
import io.gatling.core.check.Preparer
import io.gatling.core.json.JsonParsers
import io.gatling.http.response.Response

import com.fasterxml.jackson.databind.JsonNode

/**
 * Parses an HTTP response body into a Jackson JsonNode, for JsonPath and JMESPath checks.
 *
 * The PreparedCache is keyed by Preparer, so a response body is only parsed once for all the checks whose Preparers are equal. This is a case class so that all
 * the instances built for the same JsonParsers are equal: custom checks that need the parsed body should get it from this Preparer, built with the same
 * JsonParsers as the DSL (`io.gatling.core.Predef.defaultJsonParsers`), so the body is parsed only once for them and for the regular jsonPath and jmesPath
 * checks.
 */
private[gatling] final case class HttpBodyJsonPreparer(jsonParsers: JsonParsers) extends Preparer[Response, JsonNode] {
  override def apply(response: Response): Validation[JsonNode] = jsonParsers.safeParse(response.body.stream)
}
