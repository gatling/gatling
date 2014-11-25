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
import io.gatling.core.config.Protocols
import io.gatling.core.session.Expression
import io.gatling.http.action.HttpActionBuilder
import io.gatling.http.check.ws._
import io.gatling.http.request.builder.ws.WsOpenRequestBuilder

class WsOpenActionBuilder(requestName: Expression[String], wsName: String, requestBuilder: WsOpenRequestBuilder, checkBuilder: Option[WsCheckBuilder] = None) extends HttpActionBuilder {

  def check(checkBuilder: WsCheckBuilder) = new WsOpenActionBuilder(requestName, wsName, requestBuilder, Some(checkBuilder))

  def build(next: ActorRef, protocols: Protocols): ActorRef = {
    val request = requestBuilder.build(httpProtocol(protocols))
    val protocol = httpProtocol(protocols)
    actor(actorName("wsOpen"))(new WsOpenAction(requestName, wsName, request, checkBuilder, next, protocol))
  }
}

class WsSendActionBuilder(requestName: Expression[String], wsName: String, message: Expression[WsMessage], checkBuilder: Option[WsCheckBuilder] = None) extends HttpActionBuilder {

  def check(checkBuilder: WsCheckBuilder) = new WsSendActionBuilder(requestName, wsName, message, Some(checkBuilder))

  def build(next: ActorRef, protocols: Protocols): ActorRef = actor(actorName("wsSend"))(new WsSendAction(requestName, wsName, message, checkBuilder, next))
}

class WsSetCheckActionBuilder(requestName: Expression[String], checkBuilder: WsCheckBuilder, wsName: String) extends HttpActionBuilder {

  def build(next: ActorRef, protocols: Protocols): ActorRef = actor(actorName("wsSetCheck"))(new WsSetCheckAction(requestName, checkBuilder, wsName, next))
}

class WsCancelCheckActionBuilder(requestName: Expression[String], wsName: String) extends HttpActionBuilder {

  def build(next: ActorRef, protocols: Protocols): ActorRef = actor(actorName("wsCancelCheck"))(new WsCancelCheckAction(requestName, wsName, next))
}

class WsReconciliateActionBuilder(requestName: Expression[String], wsName: String) extends HttpActionBuilder {

  def build(next: ActorRef, protocols: Protocols): ActorRef = actor(actorName("wsReconciliate"))(new WsReconciliateAction(requestName, wsName, next))
}

class WsCloseActionBuilder(requestName: Expression[String], wsName: String) extends HttpActionBuilder {

  def build(next: ActorRef, protocols: Protocols): ActorRef = actor(actorName("wsClose"))(new WsCloseAction(requestName, wsName, next))
}
