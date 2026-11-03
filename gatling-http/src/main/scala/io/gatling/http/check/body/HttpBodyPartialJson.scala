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
import io.gatling.core.check.{ JsonPreparers, PartialJson, Preparer }
import io.gatling.core.json.{ JsonParsers, JsonSubset }
import io.gatling.http.check.HttpCheck
import io.gatling.http.response.Response

import com.fasterxml.jackson.databind.JsonNode

/**
 * Parses an HTTP response body into a partial Jackson tree, that only contains the subset the checks of the request read, and stops reading as soon as it has
 * it.
 */
private[gatling] final case class HttpBodyPartialJsonPreparer(jsonParsers: JsonParsers, subset: JsonSubset) extends Preparer[Response, JsonNode] {
  override def apply(response: Response): Validation[JsonNode] = jsonParsers.safeParsePartial(response.body.stream, subset)
}

private[http] object HttpBodyPartialJson {
  private object HttpBodyJsonPreparers extends JsonPreparers[Response] {
    override def jsonParsers(preparer: Any): Option[JsonParsers] =
      preparer match {
        case HttpBodyJsonPreparer(jsonParsers)           => Some(jsonParsers)
        case HttpBodyPartialJsonPreparer(jsonParsers, _) => Some(jsonParsers)
        case _                                           => None
      }

    override def whole(jsonParsers: JsonParsers): Preparer[Response, JsonNode] = HttpBodyJsonPreparer(jsonParsers)

    override def partial(jsonParsers: JsonParsers, subset: JsonSubset): Preparer[Response, JsonNode] = HttpBodyPartialJsonPreparer(jsonParsers, subset)
  }

  /** See [[PartialJson.restrictToSubset]], for the checks of a request. */
  def restrictToSubset(checks: List[HttpCheck]): List[HttpCheck] =
    PartialJson.restrictToSubset(checks, HttpBodyJsonPreparers)(_.wrapped, (check, wrapped) => check.copy(wrapped = wrapped))
}
