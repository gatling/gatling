/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

package io.gatling.http.action.sse

import io.gatling.commons.util.Clock
import io.gatling.commons.validation.{ Failure, Validation }
import io.gatling.core.action.{ Action, RequestAction }
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.stats.StatsEngine
import io.gatling.core.util.NameGen
import io.gatling.http.action.sse.fsm.{ PerformInitialConnect, SseActor }
import io.gatling.http.check.sse.SseMessageCheckSequence
import io.gatling.http.client.Request
import io.gatling.http.protocol.HttpComponents

class SseConnect(
    val requestName:       Expression[String],
    sseName:               String,
    request:               Expression[Request],
    connectCheckSequences: List[SseMessageCheckSequence],
    httpComponents:        HttpComponents,
    val next:              Action
) extends RequestAction with SseAction with NameGen {

  override val name: String = genName("sseConnect")

  override def clock: Clock = httpComponents.coreComponents.clock

  override def statsEngine: StatsEngine = httpComponents.coreComponents.statsEngine

  override def sendRequest(requestName: String, session: Session): Validation[Unit] =
    fetchActor(sseName, session) match {
      case _: Failure =>
        for {
          request <- request(session)
        } yield {
          logger.info(s"Opening sse '$sseName': Scenario '${session.scenario}', UserId #${session.userId}")

          val wsActor = httpComponents.coreComponents.actorSystem.actorOf(SseActor.props(
            sseName,
            request,
            requestName,
            connectCheckSequences,
            statsEngine,
            httpComponents.httpEngine,
            httpComponents.httpProtocol,
            clock,
            httpComponents.coreComponents.configuration
          ), genName("sseActor"))

          wsActor ! PerformInitialConnect(session, next)
        }

      case _ =>
        Failure(s"Unable to create a new SSE stream with name $sseName: already exists")
    }
}
