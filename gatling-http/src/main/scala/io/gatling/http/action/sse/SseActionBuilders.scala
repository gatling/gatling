package io.gatling.http.action.sse

import akka.actor.ActorDSL._
import akka.actor.ActorRef
import io.gatling.core.config.Protocols
import io.gatling.core.session.Expression
import io.gatling.http.action.HttpActionBuilder
import io.gatling.http.check.sse.SseCheck
import io.gatling.http.check.ws.WsCheck
import io.gatling.http.request.builder.sse.SseGetRequestBuilder

/**
 * @author ctranxuan
 */
class SseGetActionBuilder(
    requestName: Expression[String],
    sseName: String,
    requestBuilder: SseGetRequestBuilder,
    check: Option[SseCheck] = None) extends HttpActionBuilder {

  def check(check: SseCheck) = new SseGetActionBuilder(requestName, sseName, requestBuilder, Some(check))

  def check(wsCheck: WsCheck): SseGetActionBuilder = check(SseCheck(wsCheck, wsCheck.blocking, wsCheck.timeout, wsCheck.expectation))

  override def build(next: ActorRef, protocols: Protocols): ActorRef = {
    val request = requestBuilder.build(httpProtocol(protocols))
    val protocol = httpProtocol(protocols)

    actor(new SseGetAction(requestName, sseName, request, check, next, protocol))
  }
}

class SseReconciliateActionBuilder(requestName: Expression[String], sseName: String) extends HttpActionBuilder {

  override def build(next: ActorRef,
                     protocols: Protocols): ActorRef = actor(new SseReconciliateAction(requestName, sseName, next))
}

class SseCloseActionBuilder(requestName: Expression[String], sseName: String) extends HttpActionBuilder {

  override def build(next: ActorRef, protocols: Protocols): ActorRef = actor(new SseCloseAction(requestName, sseName, next))
}