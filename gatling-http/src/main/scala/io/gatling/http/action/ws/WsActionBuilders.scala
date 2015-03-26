/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.action.ws

import akka.actor.ActorDSL.actor
import akka.actor.ActorRef
import io.gatling.core.session.Expression
import io.gatling.core.structure.ScenarioContext
import io.gatling.http.action.HttpActionBuilder
import io.gatling.http.ahc.HttpEngine
import io.gatling.http.check.ws._
import io.gatling.http.config.{ HttpProtocol, DefaultHttpProtocol }
import io.gatling.http.request.builder.ws.WsOpenRequestBuilder

class WsOpenActionBuilder(
  requestName: Expression[String],
  wsName: String,
  requestBuilder: WsOpenRequestBuilder,
  checkBuilder: Option[WsCheckBuilder] = None)(implicit defaultHttpProtocol: DefaultHttpProtocol, httpEngine: HttpEngine)
    extends HttpActionBuilder {

  def check(checkBuilder: WsCheckBuilder) = new WsOpenActionBuilder(requestName, wsName, requestBuilder, Some(checkBuilder))

  override def build(next: ActorRef, ctx: ScenarioContext) = {
    val protocol = ctx.protocols.protocol[HttpProtocol]
    val request = requestBuilder.build(protocol)
    actor(actorName("wsOpen"))(new WsOpenAction(requestName, wsName, request, checkBuilder, ctx.dataWriters, next, protocol))
  }
}

class WsSendActionBuilder(
  requestName: Expression[String],
  wsName: String,
  message: Expression[WsMessage],
  checkBuilder: Option[WsCheckBuilder] = None)(implicit defaultHttpProtocol: DefaultHttpProtocol)
    extends HttpActionBuilder {

  def check(checkBuilder: WsCheckBuilder) = new WsSendActionBuilder(requestName, wsName, message, Some(checkBuilder))

  override def build(next: ActorRef, ctx: ScenarioContext) =
    actor(actorName("wsSend"))(new WsSendAction(requestName, wsName, message, checkBuilder, ctx.dataWriters, next))
}

class WsSetCheckActionBuilder(
  requestName: Expression[String],
  checkBuilder: WsCheckBuilder,
  wsName: String)(implicit defaultHttpProtocol: DefaultHttpProtocol)
    extends HttpActionBuilder {

  override def build(next: ActorRef, ctx: ScenarioContext) =
    actor(actorName("wsSetCheck"))(new WsSetCheckAction(requestName, checkBuilder, wsName, ctx.dataWriters, next))
}

class WsCancelCheckActionBuilder(
  requestName: Expression[String],
  wsName: String)(implicit defaultHttpProtocol: DefaultHttpProtocol)
    extends HttpActionBuilder {

  override def build(next: ActorRef, ctx: ScenarioContext) =
    actor(actorName("wsCancelCheck"))(new WsCancelCheckAction(requestName, wsName, ctx.dataWriters, next))
}

class WsReconciliateActionBuilder(
  requestName: Expression[String],
  wsName: String)(implicit defaultHttpProtocol: DefaultHttpProtocol)
    extends HttpActionBuilder {

  override def build(next: ActorRef, ctx: ScenarioContext) =
    actor(actorName("wsReconciliate"))(new WsReconciliateAction(requestName, wsName, ctx.dataWriters, next))
}

class WsCloseActionBuilder(
  requestName: Expression[String],
  wsName: String)(implicit defaultHttpProtocol: DefaultHttpProtocol)
    extends HttpActionBuilder {

  override def build(next: ActorRef, ctx: ScenarioContext) =
    actor(actorName("wsClose"))(new WsCloseAction(requestName, wsName, ctx.dataWriters, next))
}
