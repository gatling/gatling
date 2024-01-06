/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

import java.{ lang => jl, util => ju }
import java.util.{ function => juf }

import scala.jdk.CollectionConverters._
import scala.jdk.FunctionConverters._

import io.gatling.commons.validation.safely
import io.gatling.javaapi.core.{ CheckBuilder, Session }
import io.gatling.javaapi.core.internal.Expressions._

private[core] object CoreCheckBuilders {
  def bodyLength(): CheckBuilder.Find[Integer] =
    new CheckBuilder.Find.Default(io.gatling.core.Predef.bodyLength, CoreCheckType.BodyLength, classOf[Integer], (int: Int) => int.asInstanceOf[Integer])

  def substring(pattern: String): CheckBuilder.MultipleFind[Integer] =
    new CheckBuilder.MultipleFind.Default(
      io.gatling.core.Predef.substring(toStringExpression(pattern)),
      CoreCheckType.Substring,
      classOf[Integer],
      (int: Int) => int.asInstanceOf[Integer]
    )

  def substring(pattern: juf.Function[Session, String]): CheckBuilder.MultipleFind[Integer] =
    new CheckBuilder.MultipleFind.Default(
      io.gatling.core.Predef.substring(javaFunctionToExpression(pattern)),
      CoreCheckType.Substring,
      classOf[Integer],
      (int: Int) => int.asInstanceOf[Integer]
    )

  val responseTimeInMillis: CheckBuilder.Find[Integer] =
    new CheckBuilder.Find.Default(
      io.gatling.core.Predef.responseTimeInMillis,
      CoreCheckType.ResponseTime,
      classOf[Integer],
      (int: Int) => int.asInstanceOf[Integer]
    )

  private val ScalaXToJavaXFErrorMapper: String => String = "scalaXToJavaXF crashed: " + _

  def convertExtractedValueToJava[T, P, ScalaX, JavaX](
      wrapped: io.gatling.core.check.CheckBuilder.Validate[T, P, ScalaX],
      scalaXToJavaX: juf.Function[ScalaX, JavaX]
  ): io.gatling.core.check.CheckBuilder.Validate[T, P, JavaX] =
    if (scalaXToJavaX == null) {
      wrapped.asInstanceOf[io.gatling.core.check.CheckBuilder.Validate[T, P, JavaX]]
    } else {
      val scalaXToJavaXF = scalaXToJavaX.asScala
      wrapped.transform0(
        extracted =>
          safely(ScalaXToJavaXFErrorMapper) {
            extracted.map(_.map(scalaXToJavaXF))
          },
        identity
      )
    }

  def convertExtractedSeqToJava[T, P, ScalaX, JavaX](
      wrapped: io.gatling.core.check.CheckBuilder.Validate[T, P, Seq[ScalaX]],
      scalaXToJavaX: juf.Function[ScalaX, JavaX]
  ): io.gatling.core.check.CheckBuilder.Validate[T, P, ju.List[JavaX]] =
    if (scalaXToJavaX == null) {
      wrapped
        .asInstanceOf[io.gatling.core.check.CheckBuilder.Validate[T, P, Seq[JavaX]]]
        .transform0(
          _.map(_.map(_.asJava)),
          identity
        )
    } else {
      val scalaXToJavaXF = scalaXToJavaX.asScala
      wrapped.transform0(
        extracted =>
          safely(ScalaXToJavaXFErrorMapper) {
            extracted.map(_.map(_.map(scalaXToJavaXF).asJava))
          },
        identity
      )
    }

  def toFindRandomCheck[T, P, X](
      wrapped: io.gatling.core.check.CheckBuilder.MultipleFind[T, P, X],
      num: Int,
      failIfLess: Boolean
  ): io.gatling.core.check.CheckBuilder.Validate[T, P, ju.List[X]] =
    wrapped.findRandom(num, failIfLess).transform(_.asJava)

  def toCountCheck[T, P, X](wrapped: io.gatling.core.check.CheckBuilder.MultipleFind[T, P, X]): io.gatling.core.check.CheckBuilder.Validate[T, P, jl.Integer] =
    wrapped.count.transform(_.asInstanceOf[jl.Integer])
}
