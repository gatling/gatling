package io.gatling.http.action.sse

import akka.actor.ActorRef
import io.gatling.core.session.Session
import io.gatling.http.ahc.SseTx

/**
 * @author ctranxuan
 */
sealed trait SseEvent
case class OnSend(tx: SseTx) extends SseEvent
case class OnFailedOpen(tx: SseTx, errorMessage: String, time: Long) extends SseEvent
case class OnMessage(message: String, time: Long, sseForwarder: SseForwarder) extends SseEvent
case class OnThrowable(tx: SseTx, errorMessage: String, time: Long) extends SseEvent
case class OnClose() extends SseEvent
case class OnSseSource(sseSource: SseSource) extends SseEvent

sealed trait SseUserAction extends SseEvent {
  def requestName: String
  def next: ActorRef
  def session: Session
}
case class Close(requestName: String, next: ActorRef, session: Session) extends SseUserAction
case class Reconciliate(requestName: String, next: ActorRef, session: Session) extends SseUserAction
