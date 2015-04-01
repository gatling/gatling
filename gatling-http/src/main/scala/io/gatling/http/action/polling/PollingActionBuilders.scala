/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.action.polling

import scala.concurrent.duration.FiniteDuration

import akka.actor.{ ActorSystem, ActorRef }

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Expression
import io.gatling.core.structure.ScenarioContext
import io.gatling.http.ahc.HttpEngine
import io.gatling.http.action.HttpActionBuilder
import io.gatling.http.config.{ HttpProtocol, DefaultHttpProtocol }
import io.gatling.http.request.builder.HttpRequestBuilder

class PollingStartBuilder(
  pollerName: String,
  period: Expression[FiniteDuration],
  requestBuilder: HttpRequestBuilder)(implicit configuration: GatlingConfiguration, defaultHttpProtocol: DefaultHttpProtocol, httpEngine: HttpEngine)
    extends HttpActionBuilder {

  override def build(system: ActorSystem, next: ActorRef, ctx: ScenarioContext) = {
    val requestDef = requestBuilder.build(ctx.protocols.protocol[HttpProtocol], ctx.throttled)
    system.actorOf(PollingStartAction.props(pollerName, period, requestDef, httpEngine, ctx.dataWriters, next), actorName("pollingStart"))
  }
}

class PollingStopBuilder(
  pollerName: String)(implicit defaultHttpProtocol: DefaultHttpProtocol)
    extends HttpActionBuilder {

  override def build(system: ActorSystem, next: ActorRef, ctx: ScenarioContext) =
    system.actorOf(PollingStopAction.props(pollerName, ctx.dataWriters, next), actorName("pollingStop"))
}
