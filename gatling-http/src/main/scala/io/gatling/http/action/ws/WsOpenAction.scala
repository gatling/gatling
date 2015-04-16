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
package io.gatling.http.action.ws

import com.ning.http.client.Request

import akka.actor.{ Props, ActorRef }
import io.gatling.core.action.Interruptable
import io.gatling.core.result.writer.DataWriters
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.core.validation.{ Failure, Success }
import io.gatling.http.ahc.{ HttpEngine, WsTx }
import io.gatling.http.check.ws._
import io.gatling.http.config.HttpProtocol

object WsOpenAction {
  def props(requestName: Expression[String],
            wsName: String,
            request: Expression[Request],
            checkBuilder: Option[WsCheckBuilder],
            dataWriters: DataWriters,
            next: ActorRef,
            protocol: HttpProtocol)(implicit httpEngine: HttpEngine) =
    Props(new WsOpenAction(requestName, wsName, request, checkBuilder, dataWriters, next, protocol))
}

class WsOpenAction(
    requestName: Expression[String],
    wsName: String,
    request: Expression[Request],
    checkBuilder: Option[WsCheckBuilder],
    val dataWriters: DataWriters,
    val next: ActorRef,
    protocol: HttpProtocol)(implicit httpEngine: HttpEngine) extends Interruptable with WsAction {

  def execute(session: Session): Unit = {

      def open(tx: WsTx): Unit = {
        logger.info(s"Opening websocket '$wsName': Scenario '${session.scenario}', UserId #${session.userId}")
        val wsActor = context.actorOf(WsActor.props(wsName, dataWriters), actorName("wsActor"))
        httpEngine.startWsTransaction(tx, wsActor)
      }

    fetchWebSocket(wsName, session) match {
      case _: Success[_] =>
        Failure(s"Unable to create a new WebSocket with name $wsName: Already exists")
      case _ =>
        for {
          requestName <- requestName(session)
          request <- request(session)
          check = checkBuilder.map(_.build)
        } yield open(WsTx(session, request, requestName, protocol, next, nowMillis, check = check))
    }

  }
}
