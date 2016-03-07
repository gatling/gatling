/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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

import io.gatling.core.CoreComponents
import io.gatling.core.session._
import io.gatling.http.{ HeaderValues, HeaderNames }
import io.gatling.http.action.async.sse._
import io.gatling.http.protocol.HttpComponents
import io.gatling.http.request.builder.{ RequestBuilder, CommonAttributes }

import org.asynchttpclient.Request

object SseOpenRequestBuilder {

  val SseHeaderValueExpression = HeaderValues.TextEventStream.expressionSuccess
  val CacheControlNoCacheValueExpression = HeaderValues.NoCache.expressionSuccess

  def apply(requestName: Expression[String], url: Expression[String], sseName: String) =
    new SseOpenRequestBuilder(CommonAttributes(requestName, "GET", Left(url)), sseName)
      .header(HeaderNames.Accept, SseHeaderValueExpression)
      .header(HeaderNames.CacheControl, CacheControlNoCacheValueExpression)

  implicit def toActionBuilder(requestBuilder: SseOpenRequestBuilder): SseOpenBuilder =
    new SseOpenBuilder(requestBuilder.commonAttributes.requestName, requestBuilder.sseName, requestBuilder)
}

case class SseOpenRequestBuilder(commonAttributes: CommonAttributes, sseName: String)
    extends RequestBuilder[SseOpenRequestBuilder] {

  override private[http] def newInstance(commonAttributes: CommonAttributes) = new SseOpenRequestBuilder(commonAttributes, sseName)

  def build(coreComponents: CoreComponents, httpComponents: HttpComponents): Expression[Request] =
    new SseRequestExpressionBuilder(commonAttributes, coreComponents, httpComponents).build
}
