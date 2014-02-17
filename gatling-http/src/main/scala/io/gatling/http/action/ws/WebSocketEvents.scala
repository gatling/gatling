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
import io.gatling.core.session.Session
import akka.actor.ActorRef

sealed trait WebSocketEvents
case class OnOpen(requestName: String, webSocket: WebSocket, started: Long, ended: Long, next: ActorRef, session: Session) extends WebSocketEvents
case class OnFailedOpen(requestName: String, message: String, started: Long, ended: Long, next: ActorRef, session: Session) extends WebSocketEvents
case class OnMessage(message: String) extends WebSocketEvents
case object OnClose extends WebSocketEvents
case class OnError(t: Throwable) extends WebSocketEvents

case class SendMessage(requestName: String, message: String, next: ActorRef, session: Session) extends WebSocketEvents
case class Close(requestName: String, next: ActorRef, session: Session) extends WebSocketEvents
