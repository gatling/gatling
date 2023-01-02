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

package io.gatling.javaapi.jms.internal

import java.{ lang => jl, util => ju }
import java.util.{ function => juf }
import javax.jms.Message

import io.gatling.commons.validation.{ safely, SuccessWrapper, Validation }
import io.gatling.core.session.{ Expression, Session => ScalaSession }
import io.gatling.javaapi.core.{ CheckBuilder, Session }
import io.gatling.javaapi.core.internal.Expressions._
import io.gatling.javaapi.core.internal.JavaExpression
import io.gatling.javaapi.jms.JmsRequestReplyActionBuilder

object ScalaJmsRequestReplyActionBuilderConditions {
  def untyped(context: io.gatling.jms.request.RequestReplyDslBuilder, condition: String): Untyped =
    new Untyped(context, toBooleanExpression(condition))

  def untyped(context: io.gatling.jms.request.RequestReplyDslBuilder, condition: JavaExpression[jl.Boolean]): Untyped =
    new Untyped(context, javaBooleanFunctionToExpression(condition))

  final class Untyped(context: io.gatling.jms.request.RequestReplyDslBuilder, condition: Expression[Boolean]) {
    def thenChecks(thenChecks: ju.List[CheckBuilder]): JmsRequestReplyActionBuilder =
      new JmsRequestReplyActionBuilder(context.checkIf(condition)(JmsChecks.toScalaChecks(thenChecks): _*))
  }

  def typed(context: io.gatling.jms.request.RequestReplyDslBuilder, condition: juf.BiFunction[Message, Session, jl.Boolean]): Typed =
    new Typed(context, (u, session) => safely()(condition.apply(u, new Session(session)).booleanValue.success))

  final class Typed(context: io.gatling.jms.request.RequestReplyDslBuilder, condition: (Message, ScalaSession) => Validation[Boolean]) {
    def then_(thenChecks: ju.List[CheckBuilder]): JmsRequestReplyActionBuilder =
      new JmsRequestReplyActionBuilder(context.checkIf(condition)(JmsChecks.toScalaChecks(thenChecks): _*))
  }
}
