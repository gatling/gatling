/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

package io.gatling.recorder.http.mitm

import io.gatling.commons.util.Clock
import io.gatling.recorder.http.{ OutgoingProxy, Remote }
import io.gatling.recorder.http.mitm.controller.Controller
import io.gatling.recorder.http.ssl.SslServerContext

import com.typesafe.scalalogging.StrictLogging
import io.netty.bootstrap.Bootstrap
import io.netty.channel.{ ChannelHandlerContext, ChannelInboundHandlerAdapter }
import io.netty.handler.codec.http.{ FullHttpRequest, HttpClientCodec, HttpMethod }

private[mitm] final class ServerHandler(
    outgoingProxy: Option[OutgoingProxy],
    clientBootstrap: Bootstrap,
    sslServerContext: SslServerContext,
    trafficLogger: TrafficLogger,
    httpClientCodecFactory: () => HttpClientCodec,
    clock: Clock
) extends ChannelInboundHandlerAdapter
    with StrictLogging {
  @volatile private var https = false
  @volatile private var remote: Remote = _
  @volatile private var controller: Controller = _

  override def channelRead(ctx: ChannelHandlerContext, msg: AnyRef): Unit = {
    val sendTimestamp = clock.nowMillis
    msg match {
      case request: FullHttpRequest =>
        if (controller == null) {
          if (request.method == HttpMethod.CONNECT) {
            assert(!request.uri.startsWith("http"), s"Invalid HTTPS Proxy request: URI '${request.uri}' shouldn't have a scheme.")
            https = true
            remote = Remote.fromAbsoluteUri(s"$https://${request.uri}")
          } else {
            assert(
              request.uri.startsWith("http://"),
              s"Invalid HTTP Proxy request: URI '${request.uri}' should be absolute with http scheme. You're probably confusing Recorder proxy url and target system url."
            )
            https = false
            remote = Remote.fromAbsoluteUri(request.uri)
          }
          val clientBootstrapOnServerChannelEventLoop = clientBootstrap.clone(ctx.channel.eventLoop)
          controller = Controller(
            outgoingProxy,
            clientBootstrapOnServerChannelEventLoop,
            sslServerContext,
            trafficLogger,
            httpClientCodecFactory,
            ctx.channel,
            https,
            clock
          )
        }

        trafficLogger.logRequest(ctx.channel.id, request, remote, https, sendTimestamp)
        controller ! Controller.Message.RequestReceived(request)

      case unknown => logger.warn(s"Received unknown message: $unknown")
    }
  }

  override def channelInactive(ctx: ChannelHandlerContext): Unit =
    if (controller != null) {
      // this can happen on unknown message (see above)
      trafficLogger.clear(ctx.channel.id)
      controller ! Controller.Message.ServerChannelInactive
    }
}
