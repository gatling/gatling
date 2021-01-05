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
import io.gatling.core.session._
import io.gatling.http.cache.{ BaseUrlSupport, HttpCaches }
import io.gatling.http.client.RequestBuilder
import io.gatling.http.protocol.HttpProtocol
import io.gatling.http.request.builder.{ CommonAttributes, RequestExpressionBuilder }
import io.gatling.http.request.builder.RequestExpressionBuilder.{ ConfigureIdentity, RequestBuilderConfigure }
import io.gatling.http.util.HttpHelper

class WsRequestExpressionBuilder(
    commonAttributes: CommonAttributes,
    httpCaches: HttpCaches,
    httpProtocol: HttpProtocol,
    configuration: GatlingConfiguration,
    subprotocol: Option[Expression[String]]
) extends RequestExpressionBuilder(commonAttributes, httpCaches, httpProtocol, configuration) {

  override protected def protocolBaseUrl: Session => Option[String] =
    BaseUrlSupport.wsBaseUrl(httpProtocol)

  override protected def protocolBaseUrls: List[String] =
    httpProtocol.wsPart.wsBaseUrls

  override protected def isAbsoluteUrl(url: String): Boolean =
    HttpHelper.isAbsoluteWsUrl(url)

  override protected def configureRequestTimeout(requestBuilder: RequestBuilder): Unit =
    requestBuilder.setRequestTimeout(configuration.http.requestTimeout.toMillis)

  override protected val configureRequestBuilderForProtocol: RequestBuilderConfigure =
    subprotocol match {
      case Some(sub) =>
        session =>
          requestBuilder =>
            for {
              resolvedSubProtocol <- sub(session)
            } yield requestBuilder.setWsSubprotocol(resolvedSubProtocol)
      case _ => ConfigureIdentity
    }
}
