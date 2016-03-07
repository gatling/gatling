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
package io.gatling.http.action.async.ws

import io.gatling.core.session.Expression
import io.gatling.core.structure.ScenarioContext
import io.gatling.http.action.HttpActionBuilder
import io.gatling.http.check.async.AsyncCheckBuilder
import io.gatling.http.request.builder.ws.WsOpenRequestBuilder
import akka.actor.ActorRef
import io.gatling.core.action.Action

class WsOpenBuilder(
    requestName:    Expression[String],
    wsName:         String,
    requestBuilder: WsOpenRequestBuilder,
    checkBuilder:   Option[AsyncCheckBuilder] = None
) extends HttpActionBuilder {

  def check(checkBuilder: AsyncCheckBuilder) = new WsOpenBuilder(requestName, wsName, requestBuilder, Some(checkBuilder))

  override def build(ctx: ScenarioContext, next: Action): Action = {
    import ctx._
    val httpComponents = lookUpHttpComponents(protocolComponentsRegistry)
    val request = requestBuilder.build(coreComponents, httpComponents)
    new WsOpen(requestName, wsName, request, checkBuilder, httpComponents, system, coreComponents.statsEngine, next)
  }
}

class WsSendBuilder(
    requestName:  Expression[String],
    wsName:       String,
    message:      Expression[WsMessage],
    checkBuilder: Option[AsyncCheckBuilder] = None
) extends HttpActionBuilder {

  def check(checkBuilder: AsyncCheckBuilder) = new WsSendBuilder(requestName, wsName, message, Some(checkBuilder))

  override def build(ctx: ScenarioContext, next: Action): Action =
    new WsSend(requestName, wsName, message, checkBuilder, ctx.coreComponents.statsEngine, next)
}

class WsSetCheckBuilder(requestName: Expression[String], checkBuilder: AsyncCheckBuilder, wsName: String) extends HttpActionBuilder {

  override def build(ctx: ScenarioContext, next: Action): Action =
    new WsSetCheck(requestName, checkBuilder, wsName, ctx.coreComponents.statsEngine, next)
}

class WsCancelCheckBuilder(requestName: Expression[String], wsName: String) extends HttpActionBuilder {

  override def build(ctx: ScenarioContext, next: Action): Action =
    new WsCancelCheck(requestName, wsName, ctx.coreComponents.statsEngine, next)
}

class WsReconciliateBuilder(requestName: Expression[String], wsName: String) extends HttpActionBuilder {

  override def build(ctx: ScenarioContext, next: Action): Action =
    new WsReconciliate(requestName, wsName, ctx.coreComponents.statsEngine, next)
}

class WsCloseBuilder(requestName: Expression[String], wsName: String) extends HttpActionBuilder {

  override def build(ctx: ScenarioContext, next: Action): Action =
    new WsClose(requestName, wsName, ctx.coreComponents.statsEngine, next)
}
