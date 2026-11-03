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

import io.gatling.core.json.{ JsonParsers, JsonSubset }

import com.fasterxml.jackson.databind.JsonNode

/** The Preparers that parse an input into a JSON tree, either the whole of it or only a [[JsonSubset]] of it. */
private[gatling] trait JsonPreparers[R] {

  /** The JsonParsers of a Preparer that parses the whole input or a subset of it into a JSON tree, None for any other Preparer. */
  def jsonParsers(preparer: Any): Option[JsonParsers]

  def whole(jsonParsers: JsonParsers): Preparer[R, JsonNode]

  def partial(jsonParsers: JsonParsers, subset: JsonSubset): Preparer[R, JsonNode]
}

private[gatling] object PartialJson {

  /**
   * When all the JSON checks among some checks sharing a PreparedCache only read a subset of the input, eg `jsonPath("$.meta.code")`, make them share a
   * Preparer that only builds the union of their subsets. Otherwise, make them all parse the whole input.
   *
   * The JSON checks are the [[PreparedCheck]]s whose Preparer is one of the [[JsonPreparers]], built-in and custom ones alike. A custom check that reads the
   * JSON tree without being a PreparedCheck can't be seen here: it keeps parsing the whole input, separately from the checks that got the partial Preparer.
   *
   * As Preparers parsing a subset are recognized as well, it can be applied again after more checks have been added.
   *
   * @param unwrap
   *   the check a check wraps, if any, eg an HttpCheck's
   * @param rewrap
   *   a check, wrapping another one instead
   */
  def restrictToSubset[R, C](checks: List[C], jsonPreparers: JsonPreparers[R])(unwrap: C => Check[R], rewrap: (C, Check[R]) => C): List[C] = {
    def jsonCheck(check: C): Option[(PreparedCheck[R], JsonParsers)] =
      unwrap(check) match {
        case prepared: PreparedCheck[R @unchecked] => jsonPreparers.jsonParsers(prepared.preparer).map(prepared -> _)
        case _                                     => None
      }

    checks.flatMap(jsonCheck) match {
      case Nil                                => checks
      case jsonChecks @ (_, jsonParsers) :: _ =>
        val subsets = jsonChecks.map { case (prepared, checkJsonParsers) =>
          if (checkJsonParsers == jsonParsers) prepared.preparedSubset.collect { case subset: JsonSubset => subset } else None
        }

        val partialPreparer: Option[Preparer[R, JsonNode]] =
          if (subsets.forall(_.isDefined)) Some(jsonPreparers.partial(jsonParsers, subsets.flatten.reduce(_ ++ _))) else None

        checks.map { check =>
          jsonCheck(check) match {
            case Some((prepared, checkJsonParsers)) =>
              val preparer = partialPreparer.getOrElse(jsonPreparers.whole(checkJsonParsers))
              // the Preparer of a JSON check is one of the JsonPreparers, so it prepares a JsonNode
              rewrap(check, prepared.withPreparer(preparer.asInstanceOf[Preparer[R, prepared.Prepared]]))
            case _ => check
          }
        }
    }
  }
}
