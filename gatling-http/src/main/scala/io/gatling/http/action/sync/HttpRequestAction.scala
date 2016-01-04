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
package io.gatling.http.action.sync

import io.gatling.commons.validation._
import io.gatling.core.akka.ActorNames
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.http.action.RequestAction
import io.gatling.http.ahc.HttpEngine
import io.gatling.http.request.HttpRequestDef
import io.gatling.http.response._

import akka.actor.{ ActorRef, Props }
import com.typesafe.scalalogging.StrictLogging

object HttpRequestAction extends ActorNames with StrictLogging {

  def props(httpRequestDef: HttpRequestDef, statsEngine: StatsEngine, httpEngine: HttpEngine, next: ActorRef)(implicit configuration: GatlingConfiguration) =
    Props(new HttpRequestAction(httpRequestDef, statsEngine, httpEngine, next))
}

/**
 * This is an action that sends HTTP requests
 *
 * @constructor constructs an HttpRequestAction
 * @param httpRequestDef the request definition
 * @param statsEngine the StatsEngine
 * @param httpEngine the HttpEngine
 * @param next the next action that will be executed after the request
 */
class HttpRequestAction(httpRequestDef: HttpRequestDef, statsEngine: StatsEngine, httpEngine: HttpEngine, val next: ActorRef)(implicit configuration: GatlingConfiguration)
    extends RequestAction(statsEngine) {

  import httpRequestDef._

  val responseBuilderFactory = ResponseBuilder.newResponseBuilderFactory(
    config.checks,
    config.responseTransformer,
    config.discardResponseChunks,
    config.httpComponents.httpProtocol.responsePart.inferHtmlResources
  )
  val requestName = httpRequestDef.requestName

  def sendRequest(requestName: String, session: Session): Validation[Unit] =
    httpRequestDef.build(requestName, session).map { httpRequest =>

      val tx = HttpTx(
        session,
        httpRequest,
        responseBuilderFactory,
        next
      )

      HttpTx.start(tx, config.httpComponents)
    }
}
