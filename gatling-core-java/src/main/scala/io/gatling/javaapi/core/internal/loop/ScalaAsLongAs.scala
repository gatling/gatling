/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

package io.gatling.javaapi.core.internal.loop

import java.{ lang => jl }

import io.gatling.core.session.Expression
import io.gatling.core.session.el._
import io.gatling.javaapi.core.{ ChainBuilder, StructureBuilder }
import io.gatling.javaapi.core.internal.Expressions._
import io.gatling.javaapi.core.internal.JavaExpression
import io.gatling.javaapi.core.loop.AsLongAs

object ScalaAsLongAs {
  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: AsLongAs[T, W],
      condition: String,
      counterName: String,
      exitASAP: Boolean
  ): Loop[T, W] =
    new Loop(context, condition.el, counterName, exitASAP)

  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: AsLongAs[T, W],
      condition: JavaExpression[jl.Boolean],
      counterName: String,
      exitASAP: Boolean
  ): Loop[T, W] =
    new Loop(context, javaBooleanFunctionToExpression(condition), counterName, exitASAP)

  final class Loop[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: AsLongAs[T, W],
      condition: Expression[Boolean],
      counterName: String,
      exitASAP: Boolean
  ) {
    def loop(chain: ChainBuilder): T =
      context.make(_.asLongAs(condition, counterName, exitASAP)(chain.wrapped))
  }
}
