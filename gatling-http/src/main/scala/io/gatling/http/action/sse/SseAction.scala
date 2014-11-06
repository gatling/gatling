package io.gatling.http.action.sse

import akka.actor.ActorRef
import io.gatling.core.session.Session

/**
 * @author ctranxuan
 */
trait SseAction {

  def fetchSse(sseName: String, session: Session) = session(sseName).validate[ActorRef].mapError(m => s"Couldn't fetch open sse: $m")
}
