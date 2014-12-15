/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.request.builder.sse

import akka.actor.ActorDSL._
import akka.actor.ActorRef
import com.ning.http.client.Request
import io.gatling.core.config.Protocols
import io.gatling.core.session._
import io.gatling.http.action.HttpActionBuilder
import io.gatling.http.check.ws._
import io.gatling.http.{ HeaderValues, HeaderNames }
import io.gatling.http.action.sse._
import io.gatling.http.config.HttpProtocol
import io.gatling.http.request.builder.{ RequestBuilder, CommonAttributes }

object SseOpenRequestBuilder {

  val SseHeaderValueExpression = HeaderValues.TextEventStream.expression
  val CacheControlNoCacheValueExpression = HeaderValues.NoCache.expression

  def apply(requestName: Expression[String], url: Expression[String], sseName: String) =
    new SseOpenRequestBuilder(CommonAttributes(requestName, "GET", Left(url)), sseName)
      .header(HeaderNames.Accept, SseHeaderValueExpression)
      .header(HeaderNames.CacheControl, CacheControlNoCacheValueExpression)

  implicit def toActionBuilder(requestBuilder: SseOpenRequestBuilder): SseOpenActionBuilder =
    new SseOpenActionBuilder(requestBuilder.commonAttributes.requestName, requestBuilder.sseName, requestBuilder)
}

class SseOpenRequestBuilder(commonAttributes: CommonAttributes, val sseName: String) extends RequestBuilder[SseOpenRequestBuilder](commonAttributes) {

  override private[http] def newInstance(commonAttributes: CommonAttributes) = new SseOpenRequestBuilder(commonAttributes, sseName)

  def build(protocol: HttpProtocol): Expression[Request] = new SseRequestExpressionBuilder(commonAttributes, protocol).build
}

class SseSetCheckActionBuilder(requestName: Expression[String], checkBuilder: WsCheckBuilder, sseName: String) extends HttpActionBuilder {

  def build(next: ActorRef, protocols: Protocols): ActorRef = actor(actorName("sseSetCheck"))(new SseSetCheckAction(requestName, checkBuilder, sseName, next))
}
