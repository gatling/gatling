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
import io.gatling.core.structure.{ ChainBuilder, ScenarioContext }
import io.gatling.http.action.HttpActionBuilder
import io.gatling.http.request.builder.ws2.WsConnectRequestBuilder

case class WsConnectBuilder(
    requestBuilder:   WsConnectRequestBuilder,
    checkSequences:   List[WsCheckSequence],
    onConnectedChain: Option[ChainBuilder]
) extends HttpActionBuilder {

  def wait(timeout: FiniteDuration)(checks: WsCheck*): WsConnectBuilder = copy(checkSequences = checkSequences ::: List(WsCheckSequence(timeout, checks.toList)))

  def onConnected(chain: ChainBuilder): WsConnectBuilder = copy(onConnectedChain = Some(chain))

  override def build(ctx: ScenarioContext, next: Action): Action = {
    import ctx._
    val httpComponents = lookUpHttpComponents(protocolComponentsRegistry)
    val request = requestBuilder.build(coreComponents, httpComponents)

    val onConnected = onConnectedChain.map { chain =>
      chain.exec(OnConnectedChainEndActionBuilder).build(ctx, next)
    }

    new WsConnect(
      requestBuilder.commonAttributes.requestName,
      requestBuilder.wsName,
      request,
      checkSequences,
      onConnected,
      httpComponents = httpComponents,
      ctx.system,
      ctx.coreComponents.statsEngine,
      next = next
    )
  }
}
