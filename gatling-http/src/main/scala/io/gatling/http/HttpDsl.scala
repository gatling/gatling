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

package io.gatling.http

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.http.action.sse.Sse
import io.gatling.http.action.ws.Ws
import io.gatling.http.check.HttpCheckSupport
import io.gatling.http.check.sse.SseCheckSupport
import io.gatling.http.check.ws.WsCheckSupport
import io.gatling.http.cookie.CookieSupport
import io.gatling.http.feeder.SitemapFeederSupport
import io.gatling.http.protocol.{ HttpProtocolBuilder, ProxySupport }
import io.gatling.http.request.BodyPartSupport
import io.gatling.http.request.builder.Http
import io.gatling.http.request.builder.polling.Polling

trait HttpDsl
    extends HttpCheckSupport
    with WsCheckSupport
    with SseCheckSupport
    with SitemapFeederSupport
    with BodyPartSupport
    with CookieSupport
    with ProxySupport {

  def http(implicit configuration: GatlingConfiguration): HttpProtocolBuilder = HttpProtocolBuilder(configuration)

  def http(requestName: Expression[String]): Http = Http(requestName)

  val sse: Sse.type = Sse
  val ws: Ws.type = Ws
  def polling: Polling = Polling.Default

  val HttpHeaderNames: HeaderNames.type = HeaderNames
  val HttpHeaderValues: HeaderValues.type = HeaderValues
}
