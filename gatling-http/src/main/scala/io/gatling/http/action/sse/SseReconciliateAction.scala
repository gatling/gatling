package io.gatling.http.action.sse

import akka.actor.ActorRef
import io.gatling.core.session.{ Session, Expression }
import io.gatling.core.validation.Validation
import io.gatling.http.action.RequestAction

/**
 * @author ctranxuan
 */
class SseReconciliateAction(val requestName: Expression[String], sseName: String, val next: ActorRef)
    extends RequestAction
    with SseAction {

  override def sendRequest(requestName: String,
                           session: Session): Validation[Unit] = {
    for {
      sseActor <- fetchSse(sseName, session)
    } yield sseActor ! Reconciliate(requestName, next, session)
  }
}
