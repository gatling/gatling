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

package io.gatling.javaapi.core.internal.feed

import java.{ lang => jl, util => ju }
import java.util.{ function => juf }

import scala.jdk.CollectionConverters._

import io.gatling.core.session._
import io.gatling.core.session.el._
import io.gatling.javaapi.core.{ FeederBuilder, StructureBuilder }
import io.gatling.javaapi.core.feed.Feeds
import io.gatling.javaapi.core.internal.Expressions.javaIntegerFunctionToExpression
import io.gatling.javaapi.core.internal.JavaExpression

object ScalaFeeds {
  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: Feeds[T, W],
      feederBuilder: juf.Supplier[ju.Iterator[ju.Map[String, AnyRef]]]
  ): T =
    context.make(_.feed0(() => feederBuilder.get().asScala.map(_.asScala.toMap), System.identityHashCode(feederBuilder), None, generateJavaCollection = true))

  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: Feeds[T, W],
      feederBuilder: juf.Supplier[ju.Iterator[ju.Map[String, AnyRef]]],
      numberOfRecords: Int
  ): T =
    context.make(
      _.feed0(
        () => feederBuilder.get().asScala.map(_.asScala.toMap),
        System.identityHashCode(feederBuilder),
        Some(numberOfRecords.expressionSuccess),
        generateJavaCollection = true
      )
    )

  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: Feeds[T, W],
      feederBuilder: juf.Supplier[ju.Iterator[ju.Map[String, AnyRef]]],
      numberOfRecords: String
  ): T =
    context.make(
      _.feed0(
        () => feederBuilder.get().asScala.map(_.asScala.toMap),
        System.identityHashCode(feederBuilder),
        Some(numberOfRecords.el[Int]),
        generateJavaCollection = true
      )
    )

  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: Feeds[T, W],
      feederBuilder: juf.Supplier[ju.Iterator[ju.Map[String, AnyRef]]],
      numberOfRecords: JavaExpression[jl.Integer]
  ): T =
    context.make(
      _.feed0(
        () => feederBuilder.get().asScala.map(_.asScala.toMap),
        System.identityHashCode(feederBuilder),
        Some(javaIntegerFunctionToExpression(numberOfRecords)),
        generateJavaCollection = true
      )
    )

  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: Feeds[T, W],
      feeder: ju.Iterator[ju.Map[String, AnyRef]]
  ): T =
    context.make(_.feed0(feeder.asScala.map(_.asScala.toMap), System.identityHashCode(feeder), None, generateJavaCollection = true))

  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: Feeds[T, W],
      feeder: ju.Iterator[ju.Map[String, AnyRef]],
      numberOfRecords: Int
  ): T =
    context.make(
      _.feed0(feeder.asScala.map(_.asScala.toMap), System.identityHashCode(feeder), Some(numberOfRecords.expressionSuccess), generateJavaCollection = true)
    )

  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: Feeds[T, W],
      feeder: ju.Iterator[ju.Map[String, AnyRef]],
      numberOfRecords: String
  ): T =
    context.make(_.feed0(feeder.asScala.map(_.asScala.toMap), System.identityHashCode(feeder), Some(numberOfRecords.el[Int]), generateJavaCollection = true))

  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: Feeds[T, W],
      feeder: ju.Iterator[ju.Map[String, AnyRef]],
      numberOfRecords: JavaExpression[jl.Integer]
  ): T =
    context.make(
      _.feed0(
        feeder.asScala.map(_.asScala.toMap),
        System.identityHashCode(feeder),
        Some(javaIntegerFunctionToExpression(numberOfRecords)),
        generateJavaCollection = true
      )
    )

  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: Feeds[T, W],
      feeder: FeederBuilder[_]
  ): T =
    context.make(_.feed0(feeder.asScala, System.identityHashCode(feeder), None, generateJavaCollection = true))

  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: Feeds[T, W],
      feeder: FeederBuilder[_],
      numberOfRecords: Int
  ): T =
    context.make(_.feed0(feeder.asScala, System.identityHashCode(feeder), Some(numberOfRecords.expressionSuccess), generateJavaCollection = true))

  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: Feeds[T, W],
      feeder: FeederBuilder[_],
      numberOfRecords: String
  ): T =
    context.make(_.feed0(feeder.asScala, System.identityHashCode(feeder), Some(numberOfRecords.el[Int]), generateJavaCollection = true))

  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: Feeds[T, W],
      feeder: FeederBuilder[_],
      numberOfRecords: JavaExpression[jl.Integer]
  ): T =
    context.make(
      _.feed0(feeder.asScala, System.identityHashCode(feeder), Some(javaIntegerFunctionToExpression(numberOfRecords)), generateJavaCollection = true)
    )
}
