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
package io.gatling.http.action.async.ws

import io.gatling.http.action.async.{ AsyncEvent, AsyncTx, UserAction }
import io.gatling.http.check.async.AsyncCheck
import io.gatling.core.session.Session
import akka.actor.ActorRef
import io.gatling.core.action.Action
import org.asynchttpclient.ws.WebSocket

sealed trait WsEvent extends AsyncEvent
case class OnOpen(tx: AsyncTx, webSocket: WebSocket, time: Long) extends WsEvent
case class OnTextMessage(message: String, time: Long) extends WsEvent
case class OnByteMessage(message: Array[Byte], time: Long) extends WsEvent
case class OnClose(status: Int, reason: String, time: Long) extends WsEvent

sealed trait WsUserAction extends UserAction with WsEvent
case class Send(requestName: String, message: WsMessage, check: Option[AsyncCheck], next: Action, session: Session) extends WsUserAction

sealed trait WsMessage
case class BinaryMessage(message: Array[Byte]) extends WsMessage
case class TextMessage(message: String) extends WsMessage
