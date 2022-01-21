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
import java.time.Duration

import scala.concurrent.duration.FiniteDuration
import scala.jdk.DurationConverters._

import io.gatling.core.session.Expression
import io.gatling.core.session.el._
import io.gatling.javaapi.core.{ ChainBuilder, StructureBuilder }
import io.gatling.javaapi.core.internal.Expressions._
import io.gatling.javaapi.core.internal.JavaExpression
import io.gatling.javaapi.core.loop.DoWhileDuring

object ScalaDoWhileDuring {

  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: DoWhileDuring[T, W],
      condition: String,
      duration: String,
      counterName: String,
      exitASAP: Boolean
  ): Loop[T, W] =
    new Loop(context, condition.el, duration.el, counterName, exitASAP)

  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: DoWhileDuring[T, W],
      condition: String,
      duration: Duration,
      counterName: String,
      exitASAP: Boolean
  ): Loop[T, W] =
    new Loop(context, condition.el, toStaticValueExpression(duration.toScala), counterName, exitASAP)

  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: DoWhileDuring[T, W],
      condition: String,
      duration: JavaExpression[Duration],
      counterName: String,
      exitASAP: Boolean
  ): Loop[T, W] =
    new Loop(context, condition.el, javaDurationFunctionToExpression(duration), counterName, exitASAP)

  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: DoWhileDuring[T, W],
      condition: JavaExpression[jl.Boolean],
      duration: JavaExpression[Duration],
      counterName: String,
      exitASAP: Boolean
  ): Loop[T, W] =
    new Loop(context, javaBooleanFunctionToExpression(condition), javaDurationFunctionToExpression(duration), counterName, exitASAP)

  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: DoWhileDuring[T, W],
      condition: JavaExpression[jl.Boolean],
      duration: Duration,
      counterName: String,
      exitASAP: Boolean
  ): Loop[T, W] =
    new Loop(context, javaBooleanFunctionToExpression(condition), toStaticValueExpression(duration.toScala), counterName, exitASAP)

  final class Loop[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: DoWhileDuring[T, W],
      condition: Expression[Boolean],
      duration: Expression[FiniteDuration],
      counterName: String,
      exitASAP: Boolean
  ) {

    def loop(chain: ChainBuilder): T =
      context.make(_.asLongAsDuring(condition, duration, counterName, exitASAP)(chain.wrapped))
  }
}
