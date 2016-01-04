/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
package io.gatling.core

import io.gatling.commons.validation.Validation

package object check {

  /**
   * Transform the raw response into something that will be used as check input,
   * e.g. building a DOM tree from an HTTP response body.
   * The result might be cached and reused for other checks of the same kind performed on the same response.
   */
  type Preparer[R, P] = R => Validation[P]

  /**
   * Build a protocol specific check from a base one.
   * Usually just decorate the base one and add some more information.
   */
  type Extender[C <: Check[R], R] = Check[R] => C
}
