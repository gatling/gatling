/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.excilys.com)
 * Copyright 2012 Gilt Groupe, Inc. (www.gilt.com)
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
import io.gatling.http.check.ws.WsCheck
import io.gatling.http.request.builder.ws.WsOpenRequestBuilder

class WsOpenActionBuilder(requestName: Expression[String], wsName: String, requestBuilder: WsOpenRequestBuilder) extends HttpActionBuilder {

  def build(next: ActorRef, protocols: Protocols) = {
    val request = requestBuilder.build(httpProtocol(protocols))
    val protocol = httpProtocol(protocols)
    actor(new WsOpenAction(requestName, wsName, request, next, protocol))
  }
}

class WsSendActionBuilder(requestName: Expression[String], wsName: String, message: Expression[WsMessage], check: Option[WsCheck] = None) extends HttpActionBuilder {

  def check(check: WsCheck) = new WsSendActionBuilder(requestName, wsName, message, Some(check))

  def build(next: ActorRef, protocols: Protocols) = actor(new WsSendAction(requestName, wsName, message, check, next))
}

class WsSetCheckActionBuilder(requestName: Expression[String], check: WsCheck, wsName: String) extends HttpActionBuilder {

  def build(next: ActorRef, protocols: Protocols) = actor(new WsSetCheckAction(requestName, check, wsName, next))
}

class WsCancelCheckActionBuilder(requestName: Expression[String], wsName: String) extends HttpActionBuilder {

  def build(next: ActorRef, protocols: Protocols) = actor(new WsCancelCheckAction(requestName, wsName, next))
}

class WsReconciliateActionBuilder(requestName: Expression[String], wsName: String) extends HttpActionBuilder {

  def build(next: ActorRef, protocols: Protocols) = actor(new WsReconciliateAction(requestName, wsName, next))
}

class WsCloseActionBuilder(requestName: Expression[String], wsName: String) extends HttpActionBuilder {

  def build(next: ActorRef, protocols: Protocols) = actor(new WsCloseAction(requestName, wsName, next))
}
