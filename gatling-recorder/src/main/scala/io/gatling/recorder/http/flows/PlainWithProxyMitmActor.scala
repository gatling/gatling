/*
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
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

package io.gatling.recorder.http.flows

import java.nio.charset.StandardCharsets.UTF_8

import io.gatling.recorder.http.{ OutgoingProxy, TrafficLogger }
import io.gatling.recorder.http.Netty._

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.handler.codec.http._
import org.asynchttpclient.util.Base64

/**
 * Standard flow:
 * <ul>
 * <li>received request with absolute url</li>
 * <li>connect to proxy</li>
 * <li>propagate request with absolute url (ie original one)</li>
 * <li>receive response and propagate it to serverChannel</li>
 * </ul>
 *
 * @param serverChannel   the server channel connected to the user agent
 * @param clientBootstrap the bootstrap to establish client channels with the remote
 * @param proxy the outgoing proxy
 * @param trafficLogger log the traffic
 */
class PlainWithProxyMitmActor(
    serverChannel:   Channel,
    clientBootstrap: Bootstrap,
    proxy:           OutgoingProxy,
    trafficLogger:   TrafficLogger
)
  extends PlainMitmActor(serverChannel, clientBootstrap, trafficLogger) {

  private val proxyRemote = Remote(proxy.host, proxy.port)
  private val proxyBasicAuthHeader = proxy.credentials.map(credentials => "Basic " + Base64.encode((credentials.username + ":" + credentials.password).getBytes(UTF_8)))

  override protected def connectedRemote(requestRemote: Remote): Remote =
    proxyRemote

  override protected def propagatedRequest(originalRequest: FullHttpRequest): FullHttpRequest =
    (proxyBasicAuthHeader match {
      case Some(header) =>
        val requestWithCreds = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, originalRequest.method, originalRequest.uri, originalRequest.content)
        requestWithCreds
          .headers
          .set(originalRequest.headers)
          .set(HttpHeaderNames.PROXY_AUTHORIZATION, header)

        requestWithCreds

      case _ => originalRequest
    }).filterSupportedEncodings
}
