/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.http.action.sse

import akka.actor.ActorDSL._
import akka.actor.ActorRef
import io.gatling.core.session.Expression
import io.gatling.core.structure.ScenarioContext
import io.gatling.http.action.HttpActionBuilder
import io.gatling.http.ahc.HttpEngine
import io.gatling.http.check.ws.WsCheckBuilder
import io.gatling.http.config.{ HttpProtocol, DefaultHttpProtocol }
import io.gatling.http.request.builder.sse.SseOpenRequestBuilder

class SseOpenActionBuilder(requestName: Expression[String],
                           sseName: String,
                           requestBuilder: SseOpenRequestBuilder,
                           checkBuilder: Option[WsCheckBuilder] = None)(implicit defaultHttpProtocol: DefaultHttpProtocol, httpEngine: HttpEngine) extends HttpActionBuilder {

  def check(checkBuilder: WsCheckBuilder) = new SseOpenActionBuilder(requestName, sseName, requestBuilder, Some(checkBuilder))

  override def build(next: ActorRef, ctx: ScenarioContext): ActorRef = {
    val protocol = ctx.protocols.protocol[HttpProtocol]
    val request = requestBuilder.build(protocol)
    actor(new SseOpenAction(requestName, sseName, request, checkBuilder, ctx.dataWriters, next, protocol))
  }
}

class SseSetCheckActionBuilder(requestName: Expression[String], checkBuilder: WsCheckBuilder, sseName: String)(implicit defaultHttpProtocol: DefaultHttpProtocol) extends HttpActionBuilder {

  def build(next: ActorRef, ctx: ScenarioContext): ActorRef =
    actor(actorName("sseSetCheck"))(new SseSetCheckAction(requestName, checkBuilder, sseName, ctx.dataWriters, next))
}

class SseCancelCheckActionBuilder(requestName: Expression[String], sseName: String)(implicit defaultHttpProtocol: DefaultHttpProtocol) extends HttpActionBuilder {

  def build(next: ActorRef, ctx: ScenarioContext): ActorRef =
    actor(actorName("sseCancelCheck"))(new SseCancelCheckAction(requestName, sseName, ctx.dataWriters, next))
}

class SseReconciliateActionBuilder(requestName: Expression[String], sseName: String)(implicit defaultHttpProtocol: DefaultHttpProtocol) extends HttpActionBuilder {

  override def build(next: ActorRef, ctx: ScenarioContext): ActorRef =
    actor(new SseReconciliateAction(requestName, sseName, ctx.dataWriters, next))
}

class SseCloseActionBuilder(requestName: Expression[String], sseName: String)(implicit defaultHttpProtocol: DefaultHttpProtocol) extends HttpActionBuilder {

  override def build(next: ActorRef, ctx: ScenarioContext): ActorRef =
    actor(new SseCloseAction(requestName, sseName, ctx.dataWriters, next))
}
