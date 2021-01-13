/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.http.MissingNettyHttpHeaderValues
import io.gatling.http.action.sse.SseConnectBuilder
import io.gatling.http.client.Request
import io.gatling.http.protocol.HttpComponents
import io.gatling.http.request.builder.{ CommonAttributes, RequestBuilder }

import io.netty.handler.codec.http.{ HttpHeaderNames, HttpHeaderValues, HttpMethod }

object SseConnectRequestBuilder {

  private val SseHeaderValueExpression = MissingNettyHttpHeaderValues.TextEventStream.toString.expressionSuccess
  private val CacheControlNoCacheValueExpression = HttpHeaderValues.NO_CACHE.toString.expressionSuccess

  def apply(requestName: Expression[String], url: Expression[String], sseName: Expression[String]): SseConnectRequestBuilder =
    new SseConnectRequestBuilder(CommonAttributes(requestName, HttpMethod.GET, Left(url)), sseName)
      .header(HttpHeaderNames.ACCEPT, SseHeaderValueExpression)
      .header(HttpHeaderNames.CACHE_CONTROL, CacheControlNoCacheValueExpression)

  implicit def toActionBuilder(requestBuilder: SseConnectRequestBuilder): SseConnectBuilder =
    SseConnectBuilder(requestBuilder.commonAttributes.requestName, requestBuilder, Nil)
}

final class SseConnectRequestBuilder(val commonAttributes: CommonAttributes, val sseName: Expression[String]) extends RequestBuilder[SseConnectRequestBuilder] {

  override private[http] def newInstance(commonAttributes: CommonAttributes) = new SseConnectRequestBuilder(commonAttributes, sseName)

  def build(httpComponents: HttpComponents, configuration: GatlingConfiguration): Expression[Request] =
    new SseRequestExpressionBuilder(commonAttributes, httpComponents.httpCaches, httpComponents.httpProtocol, configuration).build
}
