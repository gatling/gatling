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
import io.gatling.core.structure.{ ChainBuilder, ScenarioContext }
import io.gatling.http.action.HttpActionBuilder
import io.gatling.http.check.ws.WsFrameCheck
import io.gatling.http.request.builder.ws.WsConnectRequestBuilder

import com.softwaremill.quicklens._

final case class WsConnectBuilder(
    requestBuilder: WsConnectRequestBuilder,
    checkSequences: List[WsFrameCheckSequenceBuilder[WsFrameCheck]],
    onConnectedChain: Option[ChainBuilder]
) extends HttpActionBuilder {

  def await(timeout: FiniteDuration)(checks: WsFrameCheck*): WsConnectBuilder =
    await(timeout.expressionSuccess)(checks: _*)

  @SuppressWarnings(Array("org.wartremover.warts.ListAppend"))
  def await(timeout: Expression[FiniteDuration])(checks: WsFrameCheck*): WsConnectBuilder =
    this.modify(_.checkSequences).using(_ :+ WsFrameCheckSequenceBuilder(timeout, checks.toList))

  def onConnected(chain: ChainBuilder): WsConnectBuilder = copy(onConnectedChain = Some(chain))

  override def build(ctx: ScenarioContext, next: Action): Action = {
    import ctx._
    val httpComponents = lookUpHttpComponents(protocolComponentsRegistry)
    val request = requestBuilder.build(httpComponents, coreComponents.configuration)

    val onConnected = onConnectedChain.map { chain =>
      chain.exec(OnConnectedChainEndActionBuilder).build(ctx, next)
    }

    new WsConnect(
      requestBuilder.commonAttributes.requestName,
      requestBuilder.wsName,
      request,
      checkSequences,
      onConnected,
      coreComponents,
      httpComponents,
      next = next
    )
  }
}
