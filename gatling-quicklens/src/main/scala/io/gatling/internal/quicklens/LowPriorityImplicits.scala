/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

// the original sources from https://github.com/softwaremill/quicklens are lacking file headers.
// we'll add them as soon as they are added upstream.package io.gatling.internal.quicklens
package io.gatling.internal.quicklens

import scala.annotation.compileTimeOnly

private[quicklens] trait LowPriorityImplicits {

  /**
   * `QuicklensEach` is in `LowPriorityImplicits` to not conflict with the `QuicklensMapAtFunctor` on `each` calls.
   */
  implicit class QuicklensEach[F[_], T](t: F[T])(implicit f: QuicklensFunctor[F, T]) {
    @compileTimeOnly(canOnlyBeUsedInsideModify("each"))
    def each: T = sys.error("")

    @compileTimeOnly(canOnlyBeUsedInsideModify("eachWhere"))
    def eachWhere(p: T => Boolean): T = sys.error("")
  }
}
