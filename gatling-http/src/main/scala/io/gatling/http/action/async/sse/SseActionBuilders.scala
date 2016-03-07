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
package io.gatling.http.action.async.sse

import io.gatling.core.action.Action
import io.gatling.core.session.Expression
import io.gatling.core.structure.ScenarioContext
import io.gatling.http.action.HttpActionBuilder
import io.gatling.http.check.async.AsyncCheckBuilder
import io.gatling.http.request.builder.sse.SseOpenRequestBuilder

class SseOpenBuilder(
    requestName:    Expression[String],
    sseName:        String,
    requestBuilder: SseOpenRequestBuilder,
    checkBuilder:   Option[AsyncCheckBuilder] = None
) extends HttpActionBuilder {

  def check(checkBuilder: AsyncCheckBuilder) = new SseOpenBuilder(requestName, sseName, requestBuilder, Some(checkBuilder))

  override def build(ctx: ScenarioContext, next: Action): Action = {
    import ctx._
    val httpComponents = lookUpHttpComponents(protocolComponentsRegistry)
    val request = requestBuilder.build(coreComponents, httpComponents)
    new SseOpen(requestName, sseName, request, checkBuilder, httpComponents, system, coreComponents.statsEngine, next)
  }
}

class SseSetCheckBuilder(requestName: Expression[String], checkBuilder: AsyncCheckBuilder, sseName: String) extends HttpActionBuilder {

  override def build(ctx: ScenarioContext, next: Action): Action =
    new SseSetCheck(requestName, checkBuilder, sseName, ctx.coreComponents.statsEngine, next)
}

class SseCancelCheckBuilder(requestName: Expression[String], sseName: String) extends HttpActionBuilder {

  override def build(ctx: ScenarioContext, next: Action): Action =
    new SseCancelCheck(requestName, sseName, ctx.coreComponents.statsEngine, next)
}

class SseReconciliateBuilder(requestName: Expression[String], sseName: String) extends HttpActionBuilder {

  override def build(ctx: ScenarioContext, next: Action): Action =
    new SseReconciliate(requestName, sseName, ctx.coreComponents.statsEngine, next)
}

class SseCloseBuilder(requestName: Expression[String], sseName: String) extends HttpActionBuilder {

  override def build(ctx: ScenarioContext, next: Action): Action =
    new SseClose(requestName, sseName, ctx.coreComponents.statsEngine, next)
}
