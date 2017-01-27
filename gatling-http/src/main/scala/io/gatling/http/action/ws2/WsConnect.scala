/**
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
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
package io.gatling.http.action.ws2

import io.gatling.commons.validation._
import io.gatling.core.action.{ Action, ExitableAction, RequestAction }
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.stats.StatsEngine
import io.gatling.core.util.NameGen
import io.gatling.http.action.async.ws.WsAction
import io.gatling.http.protocol.HttpComponents

import akka.actor.ActorSystem
import org.asynchttpclient.Request

class WsConnect(
    override val requestName: Expression[String],
    wsName:                   String,
    request:                  Expression[Request],
    connectCheckSequences:    List[WsCheckSequence],
    onConnected:              Option[Action],
    httpComponents:           HttpComponents,
    system:                   ActorSystem,
    val statsEngine:          StatsEngine,
    configuration:            GatlingConfiguration,
    val next:                 Action
) extends RequestAction with WsAction with ExitableAction with NameGen {

  override val name = genName("wsOpen")

  override def sendRequest(requestName: String, session: Session): Validation[Unit] =
    fetchActor(wsName, session) match {
      case _: Failure =>
        for {
          request <- request(session)
        } yield {
          logger.info(s"Opening websocket '$wsName': Scenario '${session.scenario}', UserId #${session.userId}")

          val wsActor = system.actorOf(WsActor.props(
            wsName,
            request,
            requestName,
            connectCheckSequences,
            onConnected,
            statsEngine,
            httpComponents.httpEngine,
            httpComponents.httpProtocol,
            configuration
          ), genName("wsActor"))

          wsActor ! PerformInitialConnect(session, next)
        }

      case _ =>
        Failure(s"Unable to create a new WebSocket with name $wsName: Already exists")
    }
}
