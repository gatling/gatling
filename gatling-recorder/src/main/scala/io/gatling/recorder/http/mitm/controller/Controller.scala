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

package io.gatling.recorder.http.mitm.controller

import io.gatling.commons.util.Clock
import io.gatling.recorder.http.OutgoingProxy
import io.gatling.recorder.http.mitm.TrafficLogger
import io.gatling.recorder.http.ssl.SslServerContext

import io.netty.bootstrap.Bootstrap
import io.netty.channel.{ Channel, ChannelId }
import io.netty.handler.codec.http.{ FullHttpRequest, FullHttpResponse, HttpClientCodec }

object Controller {
  sealed trait Message
  object Message {
    case object ServerChannelInactive extends Message
    final case class RequestReceived(request: FullHttpRequest) extends Message
    final case class ClientChannelActive(channel: Channel) extends Message
    final case class ClientChannelException(t: Throwable) extends Message
    final case class ClientChannelInactive(channelId: ChannelId) extends Message
    final case class ResponseReceived(response: FullHttpResponse) extends Message
  }

  def apply(
      outgoingProxy: Option[OutgoingProxy],
      clientBootstrap: Bootstrap,
      sslServerContext: SslServerContext,
      trafficLogger: TrafficLogger,
      httpClientCodecFactory: () => HttpClientCodec,
      channel: Channel,
      https: Boolean,
      clock: Clock
  ): Controller =
    if (https) {
      outgoingProxy match {
        case Some(proxy) => new SecuredWithProxyController(channel, clientBootstrap, sslServerContext, proxy, trafficLogger, httpClientCodecFactory, clock)
        case _           => new SecuredNoProxyController(channel, clientBootstrap, sslServerContext, trafficLogger, clock)
      }
    } else {
      outgoingProxy match {
        case Some(proxy) => new PlainWithProxyController(channel, clientBootstrap, proxy, trafficLogger, clock)
        case _           => new PlainNoProxyController(channel, clientBootstrap, trafficLogger, clock)
      }
    }
}

trait Controller {
  def !(message: Controller.Message): Unit
}
