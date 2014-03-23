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
import io.gatling.http.request.builder.OpenWebSocketRequestBuilder

class OpenWebSocketActionBuilder(requestName: Expression[String], wsName: String, requestBuilder: OpenWebSocketRequestBuilder) extends HttpActionBuilder {

  def build(next: ActorRef, protocols: Protocols) = {
    val request = requestBuilder.build(httpProtocol(protocols))
    actor(new OpenWebSocketAction(requestName, wsName, request, next, httpProtocol(protocols)))
  }
}

class SendWebSocketTextMessageActionBuilder(requestName: Expression[String], wsName: String, message: Expression[String]) extends HttpActionBuilder {

  def build(next: ActorRef, protocols: Protocols) = actor(new SendWebSocketTextMessageAction(requestName, wsName, message, next, httpProtocol(protocols)))
}

class SendWebSocketBinaryMessageActionBuilder(requestName: Expression[String], wsName: String, message: Expression[Array[Byte]]) extends HttpActionBuilder {

  def build(next: ActorRef, protocols: Protocols) = actor(new SendWebSocketBinaryMessageAction(requestName, wsName, message, next, httpProtocol(protocols)))
}

class CloseWebSocketActionBuilder(requestName: Expression[String], wsName: String) extends HttpActionBuilder {

  def build(next: ActorRef, protocols: Protocols) = actor(new CloseWebSocketAction(requestName, wsName, next, httpProtocol(protocols)))
}
