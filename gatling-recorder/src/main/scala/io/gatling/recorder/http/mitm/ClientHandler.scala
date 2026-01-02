/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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
import io.gatling.recorder.http.mitm.controller.Controller

import com.typesafe.scalalogging.StrictLogging
import io.netty.channel.{ ChannelHandlerContext, ChannelId, ChannelInboundHandlerAdapter }
import io.netty.handler.codec.http.FullHttpResponse

private[mitm] final class ClientHandler(controller: Controller, serverChannelId: ChannelId, trafficLogger: TrafficLogger, clock: Clock)
    extends ChannelInboundHandlerAdapter
    with StrictLogging {
  override def channelRead(ctx: ChannelHandlerContext, msg: AnyRef): Unit = {
    val receiveTimestamp = clock.nowMillis
    msg match {
      case response: FullHttpResponse =>
        trafficLogger.logResponse(serverChannelId, response, receiveTimestamp)
        controller ! Controller.Message.ResponseReceived(response)

      case unknown =>
        logger.warn(s"Received unknown message: $unknown")
    }
  }

  override def channelInactive(ctx: ChannelHandlerContext): Unit =
    controller ! Controller.Message.ClientChannelInactive(ctx.channel.id)

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit =
    controller ! Controller.Message.ClientChannelException(cause)
}
