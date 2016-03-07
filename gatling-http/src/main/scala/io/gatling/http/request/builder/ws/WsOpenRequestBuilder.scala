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
package io.gatling.http.request.builder.ws

import io.gatling.core.CoreComponents
import io.gatling.core.session.Expression
import io.gatling.http.action.async.ws.WsOpenBuilder
import io.gatling.http.protocol.HttpComponents
import io.gatling.http.request.builder.{ RequestBuilder, CommonAttributes }

import org.asynchttpclient.Request

object WsOpenRequestBuilder {

  implicit def toActionBuilder(requestBuilder: WsOpenRequestBuilder): WsOpenBuilder =
    new WsOpenBuilder(requestBuilder.commonAttributes.requestName, requestBuilder.wsName, requestBuilder)
}

case class WsOpenRequestBuilder(commonAttributes: CommonAttributes, wsName: String) extends RequestBuilder[WsOpenRequestBuilder] {

  private[http] def newInstance(commonAttributes: CommonAttributes) = new WsOpenRequestBuilder(commonAttributes, wsName)

  def build(coreComponents: CoreComponents, httpComponents: HttpComponents): Expression[Request] =
    new WsRequestExpressionBuilder(commonAttributes, coreComponents, httpComponents).build
}
