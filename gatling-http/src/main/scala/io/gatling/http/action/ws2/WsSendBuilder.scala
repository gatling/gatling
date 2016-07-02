/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
package io.gatling.http.action.ws2

import scala.concurrent.duration.FiniteDuration

import io.gatling.core.action.Action
import io.gatling.core.session.Expression
import io.gatling.core.structure.ScenarioContext
import io.gatling.http.action.HttpActionBuilder

case class WsSendBuilder(
  requestName:    Expression[String],
  wsName:         String,
  message:        Expression[String],
  checkSequences: List[WsCheckSequence]
) extends HttpActionBuilder {

  def wait(timeout: FiniteDuration)(checks: WsCheck*): WsSendBuilder = copy(checkSequences = checkSequences ::: List(WsCheckSequence(timeout, checks.toList)))

  override def build(ctx: ScenarioContext, next: Action): Action =
    new WsSend(
      requestName,
      wsName,
      message,
      checkSequences,
      ctx.coreComponents.statsEngine,
      next = next
    )
}
