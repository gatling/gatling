/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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

package io.gatling.javaapi.core.internal.group

import io.gatling.core.session._
import io.gatling.core.session.el._
import io.gatling.javaapi.core.{ ChainBuilder, StructureBuilder }
import io.gatling.javaapi.core.group.Groups
import io.gatling.javaapi.core.internal.Expressions.javaFunctionToExpression
import io.gatling.javaapi.core.internal.JavaExpression

object ScalaGroups {
  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: Groups[T, W],
      name: String
  ): Grouping[T, W] =
    new Grouping(context, name.el)

  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: Groups[T, W],
      name: JavaExpression[String]
  ): Grouping[T, W] =
    new Grouping(context, javaFunctionToExpression(name))

  final class Grouping[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: Groups[T, W],
      name: Expression[String]
  ) {
    def grouping(chain: ChainBuilder): T =
      context.make(_.group(name)(chain.wrapped))
  }
}
