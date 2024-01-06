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

package io.gatling.javaapi.http.internal

import java.{ lang => jl, util => ju }
import java.util.{ function => juf }

import io.gatling.commons.validation.{ safely, SuccessWrapper, Validation }
import io.gatling.core.session.{ Expression, Session => ScalaSession }
import io.gatling.core.session.el._
import io.gatling.javaapi.core.{ CheckBuilder, Session }
import io.gatling.javaapi.core.internal.Expressions._
import io.gatling.javaapi.core.internal.JavaExpression
import io.gatling.javaapi.http.WsFrameCheck

object ScalaWsFrameCheckBinaryConditions {
  def untyped(context: io.gatling.http.check.ws.WsFrameCheck.Binary, condition: String): Untyped =
    new Untyped(context, condition.el)

  def untyped(context: io.gatling.http.check.ws.WsFrameCheck.Binary, condition: JavaExpression[jl.Boolean]): Untyped =
    new Untyped(context, javaBooleanFunctionToExpression(condition))

  final class Untyped(context: io.gatling.http.check.ws.WsFrameCheck.Binary, condition: Expression[Boolean]) {
    def then_(checkBuilders: ju.List[CheckBuilder]): WsFrameCheck.Binary =
      new WsFrameCheck.Binary(context.checkIf(condition)(WsChecks.toScalaBinaryChecks(checkBuilders): _*))
  }

  def typed(context: io.gatling.http.check.ws.WsFrameCheck.Binary, condition: juf.BiFunction[Array[Byte], Session, jl.Boolean]): Typed =
    new Typed(context, (u, session) => safely()(condition.apply(u, new Session(session)).booleanValue.success))

  final class Typed(context: io.gatling.http.check.ws.WsFrameCheck.Binary, condition: (Array[Byte], ScalaSession) => Validation[Boolean]) {
    def then_(checkBuilders: ju.List[CheckBuilder]): WsFrameCheck.Binary =
      new WsFrameCheck.Binary(context.checkIf(condition)(WsChecks.toScalaBinaryChecks(checkBuilders): _*))
  }
}
