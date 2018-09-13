/*
 * Copyright 2011-2018 GatlingCorp (https://gatling.io)
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

package io.gatling.http.engine.tx

import io.gatling.core.action.Action
import io.gatling.core.session.Session
import io.gatling.http.client.ahc.uri.Uri
import io.gatling.http.fetch.ResourceAggregator
import io.gatling.http.request.HttpRequest
import io.gatling.http.response._

case class HttpTx(
    session:                Session,
    request:                HttpRequest,
    responseBuilderFactory: ResponseBuilderFactory,
    next:                   Action,
    resourceTx:             Option[ResourceTx]     = None,
    redirectCount:          Int                    = 0
) {
  lazy val silent: Boolean = request.isSilent(resourceTx.isEmpty)

  lazy val fullRequestName: String =
    if (redirectCount > 0)
      s"${request.requestName} Redirect $redirectCount"
    else
      request.requestName
}

case class ResourceTx(aggregator: ResourceAggregator, uri: Uri)
