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

/**
 * A check that reads a value its [[Preparer]] builds out of the response, eg a JSON tree, and tells which part of it it reads, so that the preparation can be
 * tuned for all the checks of a request, eg only build the parts of the JSON tree they read.
 *
 * The built-in checks implement it. Custom checks that read the same prepared value as the built-in ones, eg the HTTP response body parsed into a JSON tree,
 * should implement it too: otherwise, the preparation can't be tuned for them, and they get a value of their own, prepared separately.
 */
trait PreparedCheck[R] extends Check[R] {
  type Prepared

  def preparer: Preparer[R, Prepared]

  /** The part of the prepared value this check reads, None if it may read all of it. */
  def preparedSubset: Option[PreparedSubset]

  /** This check, reading the value built by another Preparer, which provides at least [[preparedSubset]]. */
  def withPreparer(preparer: Preparer[R, Prepared]): PreparedCheck[R]

  /** Only prepares the response when no check before has with the same Preparer. The cache is null when the response only goes through 1 check. */
  protected final def prepare(response: R, preparedCache: Check.PreparedCache): Validation[Prepared] = {
    val p = preparer
    if (preparedCache == null) {
      p(response)
    } else {
      preparedCache.computeIfAbsent(p, _ => p(response)).asInstanceOf[Validation[Prepared]]
    }
  }
}
