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

package io.gatling.javaapi.core.internal.condition

import io.gatling.core.session._
import io.gatling.core.session.el._
import io.gatling.javaapi.core.{ ChainBuilder, StructureBuilder }
import io.gatling.javaapi.core.condition.DoIfEquals
import io.gatling.javaapi.core.internal.Expressions._
import io.gatling.javaapi.core.internal.JavaExpression

object ScalaDoIfEquals {

  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: DoIfEquals[T, W],
      actual: String,
      expected: String
  ): Then[T, W] =
    new Then(context, actual.el, expected.el)

  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: DoIfEquals[T, W],
      actual: String,
      expected: Object
  ): Then[T, W] =
    new Then(context, actual.el, expected.expressionSuccess)

  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: DoIfEquals[T, W],
      actual: String,
      expected: JavaExpression[Object]
  ): Then[T, W] =
    new Then(context, actual.el, javaObjectFunctionToExpression(expected))

  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: DoIfEquals[T, W],
      actual: JavaExpression[Object],
      expected: String
  ): Then[T, W] =
    new Then(context, javaObjectFunctionToExpression(actual), expected.el)

  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: DoIfEquals[T, W],
      actual: JavaExpression[Object],
      expected: Object
  ): Then[T, W] =
    new Then(context, javaObjectFunctionToExpression(actual), expected.expressionSuccess)

  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: DoIfEquals[T, W],
      actual: JavaExpression[Object],
      expected: JavaExpression[Object]
  ): Then[T, W] =
    new Then(context, javaObjectFunctionToExpression(actual), javaObjectFunctionToExpression(expected))

  final class Then[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: DoIfEquals[T, W],
      actual: Expression[Any],
      expected: Expression[Any]
  ) {
    def then_(chain: ChainBuilder): T =
      context.make(_.doIfEquals(actual, expected)(chain.wrapped))
  }
}
