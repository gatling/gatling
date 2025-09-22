/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

package io.gatling.javaapi.core.internal

import scala.annotation.tailrec

import io.gatling.javaapi.core.Simulation

object Simulations {
  def toScalaHookOption(c: Class[_ <: Simulation], hookName: String, f: Runnable): Option[() => Unit] =
    Option.when(isHookDefinedRec(c, hookName))(() => f.run())

  @tailrec
  private def isHookDefinedRec(c: Class[_], hookName: String): Boolean =
    try {
      c.getDeclaredMethod(hookName)
      true
    } catch {
      case _: NoSuchMethodException =>
        val parent = c.getSuperclass
        parent != null && parent.getName != "io.gatling.javaapi.core.Simulation" && isHookDefinedRec(parent, hookName)
    }
}
