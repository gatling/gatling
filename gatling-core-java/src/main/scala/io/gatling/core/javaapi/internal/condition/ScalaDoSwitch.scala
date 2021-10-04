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

import io.gatling.core.javaapi.{ Choice, StructureBuilder }
import io.gatling.core.javaapi.condition.DoSwitch
import io.gatling.core.javaapi.internal.Expressions._
import io.gatling.core.javaapi.internal.JavaExpression
import io.gatling.core.session.Expression
import io.gatling.core.session.el._

object ScalaDoSwitch {
  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](context: DoSwitch[T, W], condition: String): Then[T, W] =
    new Then(context, condition.el)

  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: DoSwitch[T, W],
      condition: JavaExpression[jl.Object]
  ): Then[T, W] =
    new Then(context, javaObjectFunctionToExpression(condition))

  final class Then[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](context: DoSwitch[T, W], value: Expression[Any]) {
    def choices(choices: ju.List[Choice.WithValue]): T =
      context.make(_.doSwitch(value)(choices.asScala.map(p => (p.value, p.chain.wrapped)).toSeq: _*))
  }
}
