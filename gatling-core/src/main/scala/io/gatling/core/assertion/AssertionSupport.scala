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

package io.gatling.core.assertion

import io.gatling.commons.shared.unstable.model.stats.assertion.AssertionPathParts
import io.gatling.commons.stats.assertion.{ Details, ForAll, Global }
import io.gatling.core.config.GatlingConfiguration

trait AssertionSupport {

  implicit def string2PathParts(string: String): AssertionPathParts =
    AssertionPathParts(List(string))

  def global(implicit configuration: GatlingConfiguration): AssertionWithPath = new AssertionWithPath(Global, configuration)

  def forAll(implicit configuration: GatlingConfiguration): AssertionWithPath = new AssertionWithPath(ForAll, configuration)

  def details(pathParts: AssertionPathParts)(implicit configuration: GatlingConfiguration): AssertionWithPath =
    new AssertionWithPath(Details(pathParts.parts), configuration)
}
