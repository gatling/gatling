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

package io.gatling.http.action.ws

import scala.concurrent.duration.FiniteDuration

import io.gatling.core.action.Action
import io.gatling.core.session._
import io.gatling.core.structure.ScenarioContext
import io.gatling.http.action.HttpActionBuilder
import io.gatling.http.check.ws.{ WsBinaryFrameCheck, WsTextFrameCheck }

import com.softwaremill.quicklens._

final case class WsSendTextFrameBuilder(
    requestName: Expression[String],
    wsName: Expression[String],
    message: Expression[String],
    checkSequences: List[WsFrameCheckSequenceBuilder[WsTextFrameCheck]]
) extends HttpActionBuilder {

  def await(timeout: FiniteDuration)(checks: WsTextFrameCheck*): WsSendTextFrameBuilder =
    await(timeout.expressionSuccess)(checks: _*)

  @SuppressWarnings(Array("org.wartremover.warts.ListAppend"))
  def await(timeout: Expression[FiniteDuration])(checks: WsTextFrameCheck*): WsSendTextFrameBuilder =
    this.modify(_.checkSequences).using(_ :+ WsFrameCheckSequenceBuilder(timeout, checks.toList))

  override def build(ctx: ScenarioContext, next: Action): Action =
    new WsSendTextFrame(
      requestName,
      wsName,
      message,
      checkSequences,
      ctx.coreComponents.statsEngine,
      ctx.coreComponents.clock,
      next = next
    )
}

final case class WsSendBinaryFrameBuilder(
    requestName: Expression[String],
    wsName: Expression[String],
    message: Expression[Array[Byte]],
    checkSequences: List[WsFrameCheckSequenceBuilder[WsBinaryFrameCheck]]
) extends HttpActionBuilder {

  def await(timeout: FiniteDuration)(checks: WsBinaryFrameCheck*): WsSendBinaryFrameBuilder =
    await(timeout.expressionSuccess)(checks: _*)

  def await(timeout: Expression[FiniteDuration])(checks: WsBinaryFrameCheck*): WsSendBinaryFrameBuilder =
    this.modify(_.checkSequences).using(_ ::: List(WsFrameCheckSequenceBuilder(timeout, checks.toList)))

  override def build(ctx: ScenarioContext, next: Action): Action =
    new WsSendBinaryFrame(
      requestName,
      wsName,
      message,
      checkSequences,
      ctx.coreComponents.statsEngine,
      ctx.coreComponents.clock,
      next = next
    )
}
