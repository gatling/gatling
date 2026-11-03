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

package io.gatling.core.check

import io.gatling.commons.validation.Validation
import io.gatling.core.json.{ JsonParsers, JsonSubset }

import com.fasterxml.jackson.databind.JsonNode

/**
 * Parses a String payload into a Jackson JsonNode, for JsonPath and JMESPath checks.
 *
 * The PreparedCache is keyed by Preparer, so a payload is only parsed once for all the checks whose Preparers are equal. This is a case class so that all the
 * instances built for the same JsonParsers are equal: use it instead of an anonymous function such as `jsonParsers.safeParse`, which would create a new key for
 * every check and parse the payload for every one of them. To share the parsed payload with the regular jsonPath and jmesPath checks, build it with the same
 * JsonParsers as the DSL (`io.gatling.core.Predef.defaultJsonParsers`).
 */
private[gatling] final case class StringJsonPreparer(jsonParsers: JsonParsers) extends Preparer[String, JsonNode] {
  override def apply(string: String): Validation[JsonNode] = jsonParsers.safeParse(string)
}

/** Parses a String payload into a partial Jackson tree, that only contains the subset the checks read, see [[PartialJson]]. */
private[gatling] final case class StringPartialJsonPreparer(jsonParsers: JsonParsers, subset: JsonSubset) extends Preparer[String, JsonNode] {
  override def apply(string: String): Validation[JsonNode] = jsonParsers.safeParsePartial(string, subset)
}

private[gatling] object StringJsonPreparers extends JsonPreparers[String] {
  override def jsonParsers(preparer: Any): Option[JsonParsers] =
    preparer match {
      case StringJsonPreparer(jsonParsers)           => Some(jsonParsers)
      case StringPartialJsonPreparer(jsonParsers, _) => Some(jsonParsers)
      case _                                         => None
    }

  override def whole(jsonParsers: JsonParsers): Preparer[String, JsonNode] = StringJsonPreparer(jsonParsers)

  override def partial(jsonParsers: JsonParsers, subset: JsonSubset): Preparer[String, JsonNode] = StringPartialJsonPreparer(jsonParsers, subset)
}
