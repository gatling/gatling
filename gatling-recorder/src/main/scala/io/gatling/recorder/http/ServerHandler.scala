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
package io.gatling.recorder.http

import io.gatling.recorder.http.Netty._
import io.gatling.recorder.http.flows.MitmMessage.{ RequestReceived, ServerChannelInactive }
import io.gatling.recorder.http.flows._
import io.gatling.recorder.http.ssl.SslServerContext

import akka.actor.{ ActorRef, ActorSystem, Props }
import com.typesafe.scalalogging.StrictLogging
import io.netty.bootstrap.Bootstrap
import io.netty.channel.{ ChannelHandlerContext, ChannelInboundHandlerAdapter }
import io.netty.handler.codec.http.{ FullHttpRequest, HttpClientCodec, HttpMethod }

class ServerHandler(
    system:                 ActorSystem,
    outgoingProxy:          Option[OutgoingProxy],
    clientBootstrap:        Bootstrap,
    sslServerContext:       SslServerContext,
    trafficLogger:          TrafficLogger,
    httpClientCodecFactory: () => HttpClientCodec
) extends ChannelInboundHandlerAdapter with StrictLogging {

  @volatile private var https = false
  @volatile private var remote: Remote = _
  @volatile private var mitmActor: ActorRef = _

  override def channelRead(ctx: ChannelHandlerContext, msg: AnyRef): Unit =
    msg match {
      case request: FullHttpRequest =>
        if (mitmActor == null) {
          https = request.getMethod == HttpMethod.CONNECT
          remote = {
            val firstRequestUriWithScheme = Remote.missingScheme(request.getUri, https) match {
              case Some(scheme) => s"$scheme://${request.getUri}"
              case _            => request.getUri
            }
            Remote.fromAbsoluteUri(firstRequestUriWithScheme)
          }
          mitmActor =
            if (https) {
              outgoingProxy match {
                case Some(proxy) => system.actorOf(Props(new SecuredWithProxyMitmActor(ctx.channel, clientBootstrap, sslServerContext, proxy, trafficLogger, httpClientCodecFactory)))
                case _           => system.actorOf(Props(new SecuredNoProxyMitmActor(ctx.channel, clientBootstrap, sslServerContext, trafficLogger)))
              }
            } else {
              outgoingProxy match {
                case Some(proxy) => system.actorOf(Props(new PlainWithProxyMitmActor(ctx.channel, clientBootstrap, proxy, trafficLogger)))
                case _           => system.actorOf(Props(new PlainNoProxyMitmActor(ctx.channel, clientBootstrap, trafficLogger)))
              }
            }
        }

        trafficLogger.logRequest(ctx.channel.id, request, remote, https)
        mitmActor ! RequestReceived(request.retain())

      case unknown => logger.warn(s"Received unknown message: $unknown")
    }

  override def channelInactive(ctx: ChannelHandlerContext): Unit =
    if (mitmActor != null) {
      // this can happen on unknown message (see above)
      trafficLogger.clear(ctx.channel.id)
      mitmActor ! ServerChannelInactive
    }
}
