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

package io.gatling.core.counter

import io.gatling.core.action.builder.CounterBuilder

trait CounterSupport {

  /**
   * Bootstrap a builder for an action that stores an incrementing value into the virtual users' Session. Values are shared amongst all the virtual users of
   * this load generator, unless perUser is used.
   *
   * @param key
   *   the name of the Session attribute the value is stored into
   */
  def counter(key: String): CounterBuilder = CounterBuilder(key)
}
