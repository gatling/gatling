/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

import java.{ util => ju }

import scala.jdk.CollectionConverters._

import io.gatling.commons.validation.{ safely, SuccessWrapper }
import io.gatling.core.session._
import io.gatling.core.session.el._
import io.gatling.javaapi.core.{ ChainBuilder, Session, StructureBuilder }
import io.gatling.javaapi.core.internal.Expressions._
import io.gatling.javaapi.core.internal.JavaExpression
import io.gatling.javaapi.core.loop.ForEach

object ScalaForEach {
  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: ForEach[T, W],
      seq: ju.List[_],
      attributeName: String,
      counterName: String
  ): Loop[T, W] =
    new Loop(context, seq.asScala.toSeq.expressionSuccess, attributeName, counterName)

  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: ForEach[T, W],
      seq: String,
      attributeName: String,
      counterName: String
  ): Loop[T, W] =
    new Loop(context, seq.el, attributeName, counterName)

  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: ForEach[T, W],
      seq: JavaExpression[ju.List[_]],
      attributeName: String,
      counterName: String
  ): Loop[T, W] =
    new Loop(context, session => safely()(seq.apply(new Session(session)).asScala.toSeq.success), attributeName, counterName)

  final class Loop[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W]](
      context: ForEach[T, W],
      seq: Expression[Seq[Any]],
      attributeName: String,
      counterName: String
  ) {
    def loop(chain: ChainBuilder): T =
      context.make(_.foreach(seq, attributeName, counterName)(chain.wrapped))
  }
}
