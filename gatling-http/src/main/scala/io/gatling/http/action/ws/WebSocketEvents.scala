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
