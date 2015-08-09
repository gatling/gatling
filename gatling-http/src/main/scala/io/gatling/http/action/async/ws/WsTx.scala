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
package io.gatling.http.action.async.ws

import io.gatling.http.action.async.AsyncTx
import io.gatling.http.ahc.HttpEngine

import akka.actor.ActorRef
import org.asynchttpclient.ws.WebSocketUpgradeHandler

object WsTx {

  def start(tx: AsyncTx, wsActor: ActorRef, httpEngine: HttpEngine): Unit = {
    val (newTx, client) = {
      val (newSession, client) = httpEngine.httpClient(tx.session, tx.protocol)
      (tx.copy(session = newSession), client)
    }

    val listener = new WsListener(newTx, wsActor)

    val handler = new WebSocketUpgradeHandler.Builder().addWebSocketListener(listener).build
    client.executeRequest(tx.request, handler)
  }
}
