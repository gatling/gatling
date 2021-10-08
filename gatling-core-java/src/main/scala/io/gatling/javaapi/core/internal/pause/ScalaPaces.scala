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

package io.gatling.javaapi.core.internal.pause

import java.time.Duration

import scala.concurrent.duration.FiniteDuration
import scala.jdk.DurationConverters._

import io.gatling.core.session._
import io.gatling.core.session.el._
import io.gatling.javaapi.core.StructureBuilder
import io.gatling.javaapi.core.internal.Expressions.javaDurationFunctionToExpression
import io.gatling.javaapi.core.internal.JavaExpression
import io.gatling.javaapi.core.pause.Paces

object ScalaPaces {

  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: Paces[T, W],
      duration: Duration,
      counterName: String
  ): T =
    context.make(_.pace(duration.toScala.expressionSuccess, counterName))

  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: Paces[T, W],
      duration: String,
      counterName: String
  ): T =
    context.make(_.pace(duration.el[FiniteDuration], counterName))

  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: Paces[T, W],
      duration: JavaExpression[Duration],
      counterName: String
  ): T =
    context.make(_.pace(javaDurationFunctionToExpression(duration), counterName))

  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: Paces[T, W],
      min: Duration,
      max: Duration,
      counterName: String
  ): T =
    context.make(_.pace(min.toScala.expressionSuccess, max.toScala.expressionSuccess, counterName))

  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: Paces[T, W],
      min: String,
      max: String,
      counterName: String
  ): T =
    context.make(_.pace(min.el[FiniteDuration], max.el[FiniteDuration], counterName))

  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: Paces[T, W],
      min: JavaExpression[Duration],
      max: JavaExpression[Duration],
      counterName: String
  ): T =
    context.make(_.pace(javaDurationFunctionToExpression(min), javaDurationFunctionToExpression(max), counterName))
}
