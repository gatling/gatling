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

package io.gatling.app

import scala.util.control.{ NoStackTrace, NonFatal }

sealed abstract class GatlingLifecycleException(message: String, cause: Throwable) extends Exception(message, cause) with NoStackTrace

object GatlingLifecycleException {
  def manage[T](onNonFatal: Throwable => GatlingLifecycleException)(f: => T): T =
    try {
      f
    } catch {
      case NonFatal(t) => throw onNonFatal(t)
    }

  final class Configuration(cause: Throwable) extends GatlingLifecycleException("Failed to load GatlingConfiguration", cause)
  final class SimulationInstantiation(cause: Throwable) extends GatlingLifecycleException("Failed to create Simulation instance", cause)
  final class HookExecution(name: String, cause: Throwable) extends GatlingLifecycleException(s"Failed to execute $name hook", cause)
  final class Injection(cause: Throwable) extends GatlingLifecycleException("Simulation crashed during injection", cause)
}
