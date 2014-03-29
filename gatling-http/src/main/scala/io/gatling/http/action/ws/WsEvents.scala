/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.excilys.com)
 * Copyright 2012 Gilt Groupe, Inc. (www.gilt.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.action.ws

import com.ning.http.client.websocket.WebSocket

import akka.actor.ActorRef
import io.gatling.http.check.ws.WsCheck
import io.gatling.http.ahc.WsTx
import io.gatling.core.session.Session

sealed trait WsEvent
case class OnOpen(tx: WsTx, webSocket: WebSocket, time: Long) extends WsEvent
case class OnFailedOpen(tx: WsTx, message: String, time: Long) extends WsEvent
case class OnMessage(message: String, time: Long) extends WsEvent
case class OnClose(status: Int, reason: String, time: Long) extends WsEvent
case class CheckTimeout(check: WsCheck) extends WsEvent

sealed trait WsAction extends WsEvent {
  def requestName: String
  def next: ActorRef
  def session: Session
}
case class Send(requestName: String, message: WsMessage, check: Option[WsCheck], next: ActorRef, session: Session) extends WsAction
case class SetCheck(requestName: String, check: WsCheck, next: ActorRef, session: Session) extends WsAction
case class CancelCheck(requestName: String, next: ActorRef, session: Session) extends WsAction
case class Close(requestName: String, next: ActorRef, session: Session) extends WsAction
case class Reconciliate(requestName: String, next: ActorRef, session: Session) extends WsAction

sealed trait WsMessage
case class BinaryMessage(message: Array[Byte]) extends WsMessage
case class TextMessage(message: String) extends WsMessage
