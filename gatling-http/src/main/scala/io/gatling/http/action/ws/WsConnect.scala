/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
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

package io.gatling.http.action.ws

import io.gatling.commons.util.Clock
import io.gatling.commons.validation._
import io.gatling.core.CoreComponents
import io.gatling.core.action.{ Action, RequestAction }
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.stats.StatsEngine
import io.gatling.core.util.NameGen
import io.gatling.http.action.ws.fsm.WsFsm
import io.gatling.http.check.ws.{ WsFrameCheck, WsFrameCheckSequence }
import io.gatling.http.client.Request
import io.gatling.http.protocol.HttpComponents

class WsConnect(
    override val requestName: Expression[String],
    wsName: String,
    subprotocol: Option[String],
    request: Expression[Request],
    connectCheckSequences: List[WsFrameCheckSequence[WsFrameCheck]],
    onConnected: Option[Action],
    coreComponents: CoreComponents,
    httpComponents: HttpComponents,
    val next: Action
) extends RequestAction
    with WsAction
    with NameGen {

  override val name: String = genName("wsConnect")

  override def clock: Clock = coreComponents.clock

  override def statsEngine: StatsEngine = coreComponents.statsEngine

  override def sendRequest(requestName: String, session: Session): Validation[Unit] =
    fetchFsm(wsName, session) match {
      case _: Failure =>
        for {
          connectRequest <- request(session)
        } yield {
          logger.info(s"Opening websocket '$wsName': Scenario '${session.scenario}', UserId #${session.userId}")

          val wsFsm = WsFsm(
            wsName,
            connectRequest,
            subprotocol,
            requestName,
            connectCheckSequences,
            onConnected,
            statsEngine,
            httpComponents.httpEngine,
            httpComponents.httpProtocol,
            session.eventLoop,
            clock,
            coreComponents.configuration
          )

          wsFsm.onPerformInitialConnect(session, next)
        }

      case _ =>
        Failure(s"Unable to create a new WebSocket with name $wsName: already exists")
    }
}
