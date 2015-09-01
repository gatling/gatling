/**
 * Copyright 2011-2015 GatlingCorp (http://gatling.io)
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
package io.gatling.http.action.async.ws

import io.gatling.core.session.Expression
import io.gatling.core.structure.ScenarioContext
import io.gatling.http.action.HttpActionBuilder
import io.gatling.http.check.async.AsyncCheckBuilder
import io.gatling.http.request.builder.ws.WsOpenRequestBuilder

import akka.actor.ActorRef

class WsOpenActionBuilder(
  requestName:    Expression[String],
  wsName:         String,
  requestBuilder: WsOpenRequestBuilder,
  checkBuilder:   Option[AsyncCheckBuilder] = None
)
    extends HttpActionBuilder {

  def check(checkBuilder: AsyncCheckBuilder) = new WsOpenActionBuilder(requestName, wsName, requestBuilder, Some(checkBuilder))

  override def build(ctx: ScenarioContext, next: ActorRef) = {
    import ctx._
    implicit val configuration = ctx.configuration
    val hc = httpComponents(protocolComponentsRegistry)
    val request = requestBuilder.build(hc)
    system.actorOf(WsOpenAction.props(requestName, wsName, request, checkBuilder, coreComponents.statsEngine, hc, next), actorName("wsOpen"))
  }
}

class WsSendActionBuilder(
    requestName:  Expression[String],
    wsName:       String,
    message:      Expression[WsMessage],
    checkBuilder: Option[AsyncCheckBuilder] = None
) extends HttpActionBuilder {

  def check(checkBuilder: AsyncCheckBuilder) = new WsSendActionBuilder(requestName, wsName, message, Some(checkBuilder))

  override def build(ctx: ScenarioContext, next: ActorRef) =
    ctx.system.actorOf(WsSendAction.props(requestName, wsName, message, checkBuilder, ctx.coreComponents.statsEngine, next), actorName("wsSend"))
}

class WsSetCheckActionBuilder(requestName: Expression[String], checkBuilder: AsyncCheckBuilder, wsName: String) extends HttpActionBuilder {

  override def build(ctx: ScenarioContext, next: ActorRef) =
    ctx.system.actorOf(WsSetCheckAction.props(requestName, checkBuilder, wsName, ctx.coreComponents.statsEngine, next), actorName("wsSetCheck"))
}

class WsCancelCheckActionBuilder(requestName: Expression[String], wsName: String) extends HttpActionBuilder {

  override def build(ctx: ScenarioContext, next: ActorRef) =
    ctx.system.actorOf(WsCancelCheckAction.props(requestName, wsName, ctx.coreComponents.statsEngine, next), actorName("wsCancelCheck"))
}

class WsReconciliateActionBuilder(requestName: Expression[String], wsName: String) extends HttpActionBuilder {

  override def build(ctx: ScenarioContext, next: ActorRef) =
    ctx.system.actorOf(WsReconciliateAction.props(requestName, wsName, ctx.coreComponents.statsEngine, next), actorName("wsReconciliate"))
}

class WsCloseActionBuilder(requestName: Expression[String], wsName: String) extends HttpActionBuilder {

  override def build(ctx: ScenarioContext, next: ActorRef) =
    ctx.system.actorOf(WsCloseAction.props(requestName, wsName, ctx.coreComponents.statsEngine, next), actorName("wsClose"))
}
