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
package io.gatling.http.ahc

import io.gatling.core.check.CheckResult
import io.gatling.core.session.Session
import io.gatling.http.action.async.sse.SseHandler
import io.gatling.http.check.ws.WsCheck
import io.gatling.http.protocol.HttpProtocol

import akka.actor.ActorRef
import org.asynchttpclient.Request

object SseTx {

  def start(tx: SseTx, sseActor: ActorRef, httpEngine: HttpEngine): Unit = {
    val (newTx, client) = {
      val (newSession, client) = httpEngine.httpClient(tx.session, tx.protocol)
      (tx.copy(session = newSession), client)
    }

    val handler = new SseHandler(newTx, sseActor)
    client.executeRequest(newTx.request, handler)
  }
}

case class SseTx(
    session:               Session,
    request:               Request, // FIXME should it be a HttpRequest obj???
    requestName:           String,
    protocol:              HttpProtocol,
    next:                  ActorRef,
    start:                 Long,
    reconnectCount:        Int                      = 0,
    check:                 Option[WsCheck]          = None,
    pendingCheckSuccesses: List[CheckResult]        = Nil,
    updates:               List[Session => Session] = Nil
) {

  def applyUpdates(session: Session) = {
    val newSession = session.update(updates)
    copy(session = newSession, updates = Nil)
  }
}
