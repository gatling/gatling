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

import akka.actor.{ ActorSystem, ActorRef }
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

  override def build(system: ActorSystem, next: ActorRef, ctx: ScenarioContext) = {
    val protocol = ctx.protocols.protocol[HttpProtocol]
    val request = requestBuilder.build(protocol)
    system.actorOf(WsOpenAction.props(requestName, wsName, request, checkBuilder, ctx.dataWriters, next, protocol), actorName("wsOpen"))
  }
}

class WsSendActionBuilder(
  requestName: Expression[String],
  wsName: String,
  message: Expression[WsMessage],
  checkBuilder: Option[WsCheckBuilder] = None)(implicit defaultHttpProtocol: DefaultHttpProtocol)
    extends HttpActionBuilder {

  def check(checkBuilder: WsCheckBuilder) = new WsSendActionBuilder(requestName, wsName, message, Some(checkBuilder))

  override def build(system: ActorSystem, next: ActorRef, ctx: ScenarioContext) =
    system.actorOf(WsSendAction.props(requestName, wsName, message, checkBuilder, ctx.dataWriters, next), actorName("wsSend"))
}

class WsSetCheckActionBuilder(
  requestName: Expression[String],
  checkBuilder: WsCheckBuilder,
  wsName: String)(implicit defaultHttpProtocol: DefaultHttpProtocol)
    extends HttpActionBuilder {

  override def build(system: ActorSystem, next: ActorRef, ctx: ScenarioContext) =
    system.actorOf(WsSetCheckAction.props(requestName, checkBuilder, wsName, ctx.dataWriters, next), actorName("wsSetCheck"))
}

class WsCancelCheckActionBuilder(
  requestName: Expression[String],
  wsName: String)(implicit defaultHttpProtocol: DefaultHttpProtocol)
    extends HttpActionBuilder {

  override def build(system: ActorSystem, next: ActorRef, ctx: ScenarioContext) =
    system.actorOf(WsCancelCheckAction.props(requestName, wsName, ctx.dataWriters, next), actorName("wsCancelCheck"))
}

class WsReconciliateActionBuilder(
  requestName: Expression[String],
  wsName: String)(implicit defaultHttpProtocol: DefaultHttpProtocol)
    extends HttpActionBuilder {

  override def build(system: ActorSystem, next: ActorRef, ctx: ScenarioContext) =
    system.actorOf(WsReconciliateAction.props(requestName, wsName, ctx.dataWriters, next), actorName("wsReconciliate"))
}

class WsCloseActionBuilder(
  requestName: Expression[String],
  wsName: String)(implicit defaultHttpProtocol: DefaultHttpProtocol)
    extends HttpActionBuilder {

  override def build(system: ActorSystem, next: ActorRef, ctx: ScenarioContext) =
    system.actorOf(WsCloseAction.props(requestName, wsName, ctx.dataWriters, next), actorName("wsClose"))
}
