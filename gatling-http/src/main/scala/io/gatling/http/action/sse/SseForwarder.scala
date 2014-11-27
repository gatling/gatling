package io.gatling.http.action.sse

/**
 * @author ctranxuan
 */
trait SseForwarder {
  def stopForward(): Unit;
}
