/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import com.ning.http.client.Request
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.http.{ HeaderValues, HeaderNames }
import io.gatling.http.action.sse._
import io.gatling.http.protocol.HttpComponents
import io.gatling.http.request.builder.{ RequestBuilder, CommonAttributes }

object SseOpenRequestBuilder {

  val SseHeaderValueExpression = HeaderValues.TextEventStream.expression
  val CacheControlNoCacheValueExpression = HeaderValues.NoCache.expression

  def apply(requestName: Expression[String], url: Expression[String], sseName: String)(implicit configuration: GatlingConfiguration) =
    new SseOpenRequestBuilder(CommonAttributes(requestName, "GET", Left(url)), sseName)
      .header(HeaderNames.Accept, SseHeaderValueExpression)
      .header(HeaderNames.CacheControl, CacheControlNoCacheValueExpression)

  implicit def toActionBuilder(requestBuilder: SseOpenRequestBuilder)(implicit configuration: GatlingConfiguration): SseOpenActionBuilder =
    new SseOpenActionBuilder(requestBuilder.commonAttributes.requestName, requestBuilder.sseName, requestBuilder)
}

case class SseOpenRequestBuilder(commonAttributes: CommonAttributes, sseName: String)
    extends RequestBuilder[SseOpenRequestBuilder] {

  override private[http] def newInstance(commonAttributes: CommonAttributes) = new SseOpenRequestBuilder(commonAttributes, sseName)

  def build(httpComponents: HttpComponents)(implicit configuration: GatlingConfiguration): Expression[Request] =
    new SseRequestExpressionBuilder(commonAttributes, httpComponents).build
}
