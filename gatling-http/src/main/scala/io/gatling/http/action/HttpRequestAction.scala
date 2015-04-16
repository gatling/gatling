/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import com.typesafe.scalalogging.StrictLogging

import akka.actor.{ Props, ActorRef }
import io.gatling.core.akka.ActorNames
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.result.writer.DataWriters
import io.gatling.core.session.Session
import io.gatling.core.validation._
import io.gatling.http.ahc.{ HttpEngine, HttpTx }
import io.gatling.http.request.HttpRequestDef
import io.gatling.http.response._

object HttpRequestAction extends ActorNames with StrictLogging {

  def props(httpRequestDef: HttpRequestDef, httpEngine: HttpEngine, dataWriters: DataWriters, next: ActorRef)(implicit configuration: GatlingConfiguration) =
    Props(new HttpRequestAction(httpRequestDef, httpEngine, dataWriters, next))
}

/**
 * This is an action that sends HTTP requests
 *
 * @constructor constructs an HttpRequestAction
 * @param httpRequestDef the request definition
 * @param httpEngine the HttpEngine
 * @param dataWriters the DataWriters
 * @param next the next action that will be executed after the request
 */
class HttpRequestAction(httpRequestDef: HttpRequestDef, httpEngine: HttpEngine, dataWriters: DataWriters, val next: ActorRef)(implicit configuration: GatlingConfiguration)
    extends RequestAction(dataWriters) {

  import httpRequestDef._

  val responseBuilderFactory = ResponseBuilder.newResponseBuilderFactory(
    config.checks,
    config.responseTransformer,
    config.discardResponseChunks,
    config.protocol.responsePart.inferHtmlResources)
  val requestName = httpRequestDef.requestName

  def sendRequest(requestName: String, session: Session): Validation[Unit] =
    httpRequestDef.build(requestName, session).map { httpRequest =>

      val tx = HttpTx(
        session,
        httpRequest,
        responseBuilderFactory,
        next)

      httpEngine.startHttpTransaction(tx)
    }
}
