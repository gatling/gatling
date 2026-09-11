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

package io.gatling.core.queue

import io.gatling.core.action.builder.SharedQueueBuilder

trait QueueSupport {

  /**
   * Bootstrap a factory of actions that exchange values over an in-memory queue, so virtual users can communicate with each other.
   *
   * The queue is owned by the returned instance, so it must be stored in a `val` that's then used everywhere the queue must be accessed.
   *
   * The queue is local to this load generator: when running a distributed test with Gatling Enterprise, each load generator has its own queue and they don't
   * exchange values with each other.
   *
   * @param name
   *   the name of the queue, only used for logging
   */
  def sharedQueue(name: String): SharedQueueBuilder = SharedQueueBuilder(name)
}
