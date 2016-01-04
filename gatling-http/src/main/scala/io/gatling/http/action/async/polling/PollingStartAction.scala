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
import io.gatling.core.action.{ Failable, Interruptable }
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.http.response.ResponseBuilder
import io.gatling.http.request.HttpRequestDef

import akka.actor.{ ActorRef, Props }

object PollingStartAction {

  def props(
    pollerName:  String,
    period:      Expression[FiniteDuration],
    requestDef:  HttpRequestDef,
    statsEngine: StatsEngine,
    next:        ActorRef
  )(implicit configuration: GatlingConfiguration): Props =
    Props(new PollingStartAction(pollerName, period, requestDef, statsEngine, next))
}

class PollingStartAction(
  pollerName:      String,
  period:          Expression[FiniteDuration],
  requestDef:      HttpRequestDef,
  val statsEngine: StatsEngine,
  val next:        ActorRef
)(implicit configuration: GatlingConfiguration)
    extends Interruptable
    with Failable
    with PollingAction {

  def executeOrFail(session: Session) = {

    val httpComponents = requestDef.config.httpComponents

    val responseBuilderFactory = ResponseBuilder.newResponseBuilderFactory(
      requestDef.config.checks,
      requestDef.config.responseTransformer,
      requestDef.config.discardResponseChunks,
      httpComponents.httpProtocol.responsePart.inferHtmlResources
    )

      def startPolling(period: FiniteDuration): Unit = {
        logger.info(s"Starting poller $pollerName")
        val pollingActor = context.actorOf(PollerActor.props(pollerName, period, requestDef, responseBuilderFactory, statsEngine, httpComponents), actorName("pollingActor"))

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
