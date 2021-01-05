/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

package io.gatling.http.action

import io.gatling.core.action.Action
import io.gatling.core.structure.ScenarioContext
import io.gatling.http.request.builder.HttpRequestBuilder

/**
 * Builder for HttpRequestActionBuilder
 *
 * @constructor creates an HttpRequestActionBuilder
 * @param requestBuilder the builder for the request that will be sent
 */
class HttpRequestActionBuilder(requestBuilder: HttpRequestBuilder) extends HttpActionBuilder {

  override def build(ctx: ScenarioContext, next: Action): Action = {
    import ctx._
    val httpComponents = lookUpHttpComponents(protocolComponentsRegistry)
    val httpRequest = requestBuilder.build(httpComponents.httpCaches, httpComponents.httpProtocol, throttled, coreComponents.configuration)
    new HttpRequestAction(
      httpRequest,
      httpComponents.httpTxExecutor,
      coreComponents,
      next
    )
  }
}
