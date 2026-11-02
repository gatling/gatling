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
import io.gatling.core.check.{ Check, Preparer }
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
  private def jsonCheck(check: HttpCheck): Option[(Check.Default[Response, JsonNode, Any], JsonParsers)] =
    check.wrapped match {
      case default: Check.Default[_, _, _] =>
        default.preparer match {
          case HttpBodyJsonPreparer(jsonParsers) => Some((default.asInstanceOf[Check.Default[Response, JsonNode, Any]], jsonParsers))
          case _                                 => None
        }
      case _ => None
    }

  /**
   * When all the checks of a request that parse the response body into a JSON tree only read a subset of it, eg `jsonPath("$.meta.code")`, make them share a
   * Preparer that only builds the union of their subsets. Otherwise, the checks are left untouched and the whole tree gets built.
   */
  def restrictToSubset(checks: List[HttpCheck]): List[HttpCheck] =
    checks.flatMap(jsonCheck) match {
      case Nil                                => checks
      case jsonChecks @ (_, jsonParsers) :: _ =>
        val subsets = jsonChecks.map { case (default, checkJsonParsers) =>
          if (checkJsonParsers == jsonParsers) default.preparedSubset.collect { case subset: JsonSubset => subset } else None
        }

        if (subsets.forall(_.isDefined)) {
          val preparer = HttpBodyPartialJsonPreparer(jsonParsers, subsets.flatten.reduce(_ ++ _))
          checks.map { check =>
            jsonCheck(check) match {
              case Some((default, _)) => check.copy(wrapped = default.copy(preparer = preparer))
              case _                  => check
            }
          }
        } else {
          checks
        }
    }
}
