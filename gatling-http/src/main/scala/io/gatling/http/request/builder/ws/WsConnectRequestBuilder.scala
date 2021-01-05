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

package io.gatling.http.request.builder.ws

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Expression
import io.gatling.http.action.ws.WsConnectBuilder
import io.gatling.http.client.Request
import io.gatling.http.protocol.HttpComponents
import io.gatling.http.request.builder.{ CommonAttributes, RequestBuilder }

object WsConnectRequestBuilder {

  implicit def toActionBuilder(requestBuilder: WsConnectRequestBuilder): WsConnectBuilder =
    WsConnectBuilder(requestBuilder, Nil, None)
}

final class WsConnectRequestBuilder(val commonAttributes: CommonAttributes, val wsName: Expression[String], val subprotocol: Option[Expression[String]])
    extends RequestBuilder[WsConnectRequestBuilder] {

  def subprotocol(sub: Expression[String]): WsConnectRequestBuilder = new WsConnectRequestBuilder(commonAttributes, wsName, Some(sub))

  private[http] def newInstance(commonAttributes: CommonAttributes) = new WsConnectRequestBuilder(commonAttributes, wsName, subprotocol)

  def build(httpComponents: HttpComponents, configuration: GatlingConfiguration): Expression[Request] =
    new WsRequestExpressionBuilder(
      commonAttributes,
      httpComponents.httpCaches,
      httpComponents.httpProtocol,
      configuration,
      subprotocol
    ).build
}
