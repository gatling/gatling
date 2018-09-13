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

package io.gatling.http.action.polling

import scala.concurrent.duration.FiniteDuration

import io.gatling.commons.util.Clock
import io.gatling.commons.validation.{ Failure, Success, Validation }
import io.gatling.core.CoreComponents
import io.gatling.core.action.{ Action, ExitableAction }
import io.gatling.core.session._
import io.gatling.core.stats.StatsEngine
import io.gatling.core.util.NameGen
import io.gatling.http.cache.HttpCaches
import io.gatling.http.engine.tx.HttpTxExecutor
import io.gatling.http.protocol.HttpProtocol
import io.gatling.http.request.HttpRequestDef
import io.gatling.http.response.ResponseBuilder

class PollingStart(
    pollerName:     String,
    period:         FiniteDuration,
    coreComponents: CoreComponents,
    httpRequestDef: HttpRequestDef,
    httpCaches:     HttpCaches,
    httpProtocol:   HttpProtocol,
    httpTxExecutor: HttpTxExecutor,
    val next:       Action
) extends ExitableAction with PollingAction with NameGen {

  import httpRequestDef._

  override val name: String = genName(pollerName)

  override def clock: Clock = coreComponents.clock

  override def statsEngine: StatsEngine = coreComponents.statsEngine

  private val responseBuilderFactory = ResponseBuilder.newResponseBuilderFactory(
    requestConfig.checks,
    requestConfig.httpProtocol.responsePart.inferHtmlResources,
    clock,
    coreComponents.configuration
  )

  override def execute(session: Session): Unit = recover(session) {

    def startPolling(): Unit = {
      logger.info(s"Starting poller $pollerName")
      val pollingActor = coreComponents.actorSystem.actorOf(
        PollerActor.props(
          pollerName,
          period,
          httpRequestDef,
          responseBuilderFactory,
          httpTxExecutor,
          httpCaches,
          httpProtocol,
          statsEngine,
          clock,
          coreComponents.configuration.core.charset
        ),
        name + "-actor-" + session.userId
      )

      val newSession = session.set(pollerName, pollingActor)

      pollingActor ! StartPolling(newSession)
      next ! newSession
    }

    fetchActor(pollerName, session) match {
      case _: Success[_] =>
        Failure(s"Unable to create a new poller with name $pollerName: already exists")
      case _ =>
        startPolling()
        Validation.unit
    }
  }
}
