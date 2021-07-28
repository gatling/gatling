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

package io.gatling.core.javaapi.internal.condition

import java.{ lang => jl, util => ju }

import scala.jdk.CollectionConverters._

import io.gatling.core.javaapi.{ ChainBuilder, Possibility, StructureBuilder }
import io.gatling.core.javaapi.condition.DoSwitchOrElse
import io.gatling.core.javaapi.internal.Expressions._
import io.gatling.core.javaapi.internal.JavaExpression
import io.gatling.core.session.Expression
import io.gatling.core.session.el._

object ScalaDoSwitchOrElse {
  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](context: DoSwitchOrElse[T, W], condition: String): Then[T, W] =
    new Then(context, condition.el)

  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: DoSwitchOrElse[T, W],
      condition: JavaExpression[jl.Object]
  ): Then[T, W] =
    new Then(context, javaObjectFunctionToExpression(condition))

  final class Then[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](context: DoSwitchOrElse[T, W], value: Expression[Any]) {
    def possibilities(possibilities: ju.List[Possibility.WithValue]): OrElse[T, W] =
      new OrElse(context, value, possibilities)
  }

  final class OrElse[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: DoSwitchOrElse[T, W],
      value: Expression[Any],
      possibilities: ju.List[Possibility.WithValue]
  ) {
    def orElse(chain: ChainBuilder): T =
      context.make(_.doSwitchOrElse(value)(possibilities.asScala.map(p => (p.value, p.chain.wrapped)).toSeq: _*)(chain.wrapped))
  }
}
