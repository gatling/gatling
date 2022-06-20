/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.http.request.builder.sse

import io.gatling.core.action.Action
import io.gatling.core.session._
import io.gatling.core.structure.ScenarioContext
import io.gatling.http.action.HttpActionBuilder
import io.gatling.http.action.sse.{ SseAwaitActionBuilder, SseConnect, SseMessageCheckSequenceBuilder }
import io.gatling.http.request.builder.{ CommonAttributes, RequestBuilder }

import com.softwaremill.quicklens._
import io.netty.handler.codec.http.{ HttpHeaderNames, HttpHeaderValues, HttpMethod }

object SseConnectRequestBuilder {

  private val SseHeaderValueExpression = HttpHeaderValues.TEXT_EVENT_STREAM.toString.expressionSuccess
  private val CacheControlNoCacheValueExpression = HttpHeaderValues.NO_CACHE.toString.expressionSuccess

  def apply(requestName: Expression[String], url: Expression[String], sseName: Expression[String]): SseConnectRequestBuilder =
    new SseConnectRequestBuilder(CommonAttributes(requestName, HttpMethod.GET, Left(url)), sseName, Nil)
      .header(HttpHeaderNames.ACCEPT, SseHeaderValueExpression)
      .header(HttpHeaderNames.CACHE_CONTROL, CacheControlNoCacheValueExpression)
}

final case class SseConnectRequestBuilder(
    commonAttributes: CommonAttributes,
    sseName: Expression[String],
    checkSequences: List[SseMessageCheckSequenceBuilder]
) extends RequestBuilder[SseConnectRequestBuilder]
    with HttpActionBuilder
    with SseAwaitActionBuilder[SseConnectRequestBuilder] {

  override private[http] def newInstance(commonAttributes: CommonAttributes) = new SseConnectRequestBuilder(commonAttributes, sseName, checkSequences)

  override def build(ctx: ScenarioContext, next: Action): Action = {
    val httpComponents = lookUpHttpComponents(ctx.protocolComponentsRegistry)
    val request = new SseRequestExpressionBuilder(
      commonAttributes,
      httpComponents.httpCaches,
      httpComponents.httpProtocol,
      ctx.coreComponents.configuration
    ).build
    new SseConnect(
      commonAttributes.requestName,
      sseName,
      request,
      checkSequences,
      ctx.coreComponents,
      httpComponents,
      next
    )
  }

  @SuppressWarnings(Array("org.wartremover.warts.ListAppend"))
  override protected def appendCheckSequence(checkSequence: SseMessageCheckSequenceBuilder): SseConnectRequestBuilder =
    this.modify(_.checkSequences)(_ :+ checkSequence)
}
