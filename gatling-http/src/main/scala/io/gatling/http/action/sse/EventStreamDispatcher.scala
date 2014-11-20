package io.gatling.http.action.sse

/**
 * @author ctranxuan
 */
trait EventStreamDispatcher {
  def dispatchEventStream(sse: ServerSentEvent): Unit
}
