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

package io.gatling.core.javaapi.internal.exec

import java.{ util => ju }

import scala.jdk.CollectionConverters._

import io.gatling.commons.validation.{ safely, SuccessWrapper }
import io.gatling.core.javaapi.{ Session, StructureBuilder }
import io.gatling.core.javaapi.exec.Execs
import io.gatling.core.javaapi.internal.JavaExpression

object ScalaExecs {

  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W], B <: StructureBuilder[
    _,
    WB
  ], WB <: io.gatling.core.structure.StructureBuilder[WB]](
      context: Execs[T, W],
      f: JavaExpression[Session]
  ): T =
    context.make(_.exec(session => safely()(f.apply(new Session(session)).asScala().success)))

  def apply[T <: StructureBuilder[T, W], W <: io.gatling.core.structure.StructureBuilder[W], B <: StructureBuilder[
    _,
    WB
  ], WB <: io.gatling.core.structure.StructureBuilder[WB]](
      context: Execs[T, W],
      structureBuilders: ju.List[B]
  ): T =
    context.make(_.exec(structureBuilders.asScala.map(_.wrapped)))
}
