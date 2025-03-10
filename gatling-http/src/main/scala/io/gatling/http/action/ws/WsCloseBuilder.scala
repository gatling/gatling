/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

import io.gatling.core.action.Action
import io.gatling.core.session._
import io.gatling.core.structure.ScenarioContext
import io.gatling.http.action.HttpActionBuilder

import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus

final class WsCloseBuilder(
    requestName: Expression[String],
    wsName: Expression[String],
    closeStatus: WebSocketCloseStatus
) extends HttpActionBuilder {
  override def build(ctx: ScenarioContext, next: Action): Action =
    new WsClose(
      requestName,
      wsName,
      closeStatus,
      ctx.coreComponents.statsEngine,
      ctx.coreComponents.clock,
      next = next
    )
}
