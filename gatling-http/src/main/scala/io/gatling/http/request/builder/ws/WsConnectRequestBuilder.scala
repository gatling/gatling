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

package io.gatling.http.request.builder.ws

import io.gatling.core.action.Action
import io.gatling.core.session.Expression
import io.gatling.core.structure.{ ChainBuilder, ScenarioContext }
import io.gatling.http.action.HttpActionBuilder
import io.gatling.http.action.ws.{ OnConnectedChainEndActionBuilder, WsAwaitActionBuilder, WsConnect, WsFrameCheckSequenceBuilder }
import io.gatling.http.check.ws.WsFrameCheck
import io.gatling.http.request.builder.{ CommonAttributes, RequestBuilder }

import com.softwaremill.quicklens._

final case class WsConnectRequestBuilder(
    commonAttributes: CommonAttributes,
    wsName: Expression[String],
    subprotocol: Option[Expression[String]],
    onConnectedChain: Option[ChainBuilder],
    checkSequences: List[WsFrameCheckSequenceBuilder[WsFrameCheck]]
) extends RequestBuilder[WsConnectRequestBuilder]
    with HttpActionBuilder
    with WsAwaitActionBuilder[WsConnectRequestBuilder, WsFrameCheck] {

  def subprotocol(sub: Expression[String]): WsConnectRequestBuilder = copy(subprotocol = Some(sub))

  def onConnected(chain: ChainBuilder): WsConnectRequestBuilder = copy(onConnectedChain = Some(chain))

  private[http] def newInstance(commonAttributes: CommonAttributes) = copy(commonAttributes = commonAttributes)

  @SuppressWarnings(Array("org.wartremover.warts.ListAppend"))
  override protected def appendCheckSequence(checkSequence: WsFrameCheckSequenceBuilder[WsFrameCheck]): WsConnectRequestBuilder =
    this.modify(_.checkSequences)(_ :+ checkSequence)

  override def build(ctx: ScenarioContext, next: Action): Action = {
    import ctx._
    val httpComponents = lookUpHttpComponents(protocolComponentsRegistry)
    val request = new WsRequestExpressionBuilder(
      commonAttributes,
      httpComponents.httpCaches,
      httpComponents.httpProtocol,
      coreComponents.configuration,
      subprotocol
    ).build

    val onConnected = onConnectedChain.map { chain =>
      chain.exec(OnConnectedChainEndActionBuilder).build(ctx, next)
    }

    new WsConnect(
      commonAttributes.requestName,
      wsName,
      request,
      checkSequences,
      onConnected,
      coreComponents,
      httpComponents,
      next = next
    )
  }
}
