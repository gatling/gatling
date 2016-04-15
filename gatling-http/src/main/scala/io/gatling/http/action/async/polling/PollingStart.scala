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
package io.gatling.http.action.async.polling

import scala.concurrent.duration.FiniteDuration

import io.gatling.commons.validation.{ Failure, Success }
import io.gatling.core.stats.StatsEngine
import io.gatling.core.action.{ Action, ExitableAction }
import io.gatling.core.session._
import io.gatling.core.util.NameGen
import io.gatling.http.response.ResponseBuilder
import io.gatling.http.request.HttpRequestDef

import akka.actor.ActorSystem

class PollingStart(
    pollerName:      String,
    period:          Expression[FiniteDuration],
    httpRequestDef:  HttpRequestDef,
    system:          ActorSystem,
    val statsEngine: StatsEngine,
    val next:        Action
) extends ExitableAction with PollingAction with NameGen {

  override val name = genName(pollerName)

  import httpRequestDef._

  val responseBuilderFactory = ResponseBuilder.newResponseBuilderFactory(
    config.checks,
    config.responseTransformer,
    config.discardResponseChunks,
    config.httpComponents.httpProtocol.responsePart.inferHtmlResources,
    config.coreComponents.configuration
  )

  override def execute(session: Session): Unit = recover(session) {

      def startPolling(period: FiniteDuration): Unit = {
        logger.info(s"Starting poller $pollerName")
        val pollingActor = system.actorOf(PollerActor.props(pollerName, period, httpRequestDef, responseBuilderFactory, statsEngine), name + "-actor-" + session.userId)

        val newSession = session.set(pollerName, pollingActor)

        pollingActor ! StartPolling(newSession)
        next ! newSession
      }

    fetchPoller(pollerName, session) match {
      case _: Success[_] =>
        Failure(s"Unable to create a new poller with name $pollerName: Already exists")
      case _ =>
        for (period <- period(session)) yield startPolling(period)
    }
  }
}
