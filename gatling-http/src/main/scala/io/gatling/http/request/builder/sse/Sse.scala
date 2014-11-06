package io.gatling.http.request.builder.sse

import io.gatling.core.session.{ SessionPrivateAttributes, Expression }
import io.gatling.http.action.sse.{ SseReconciliateActionBuilder, SseCloseActionBuilder }
import io.gatling.http.request.builder.CommonAttributes

/**
 * @author ctranxuan
 */
object Sse {
  val DefaultSseName = SessionPrivateAttributes.PrivateAttributePrefix + "http.sse"

}

class Sse(requestName: Expression[String], sseName: String = Sse.DefaultSseName) {

  def sseName(sseName: String) = new Sse(requestName, sseName)

  /**
   * Open the sse stream and get the results of the stream
   */
  def get(url: Expression[String]) = new SseGetRequestBuilder(CommonAttributes(requestName, "GET", Left(url)), sseName)

  /**
   * Reconciliate the main state with the one of the sse flow.
   */
  def reconciliate() = new SseReconciliateActionBuilder(requestName, sseName)

  /**
   * Closes the sse stream.
   */
  def close() = new SseCloseActionBuilder(requestName, sseName)

}
