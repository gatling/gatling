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

package io.gatling.recorder.http

import io.gatling.commons.util.Clock
import io.gatling.recorder.http.flows.MitmMessage.{ ClientChannelException, ClientChannelInactive, ResponseReceived }

import akka.actor.ActorRef
import com.typesafe.scalalogging.StrictLogging
import io.netty.channel.{ ChannelHandlerContext, ChannelId, ChannelInboundHandlerAdapter }
import io.netty.handler.codec.http.FullHttpResponse

class ClientHandler(mitmActor: ActorRef, serverChannelId: ChannelId, trafficLogger: TrafficLogger, clock: Clock)
    extends ChannelInboundHandlerAdapter
    with StrictLogging {

  override def channelRead(ctx: ChannelHandlerContext, msg: AnyRef): Unit = {
    val receiveTimestamp = clock.nowMillis
    msg match {
      case response: FullHttpResponse =>
        trafficLogger.logResponse(serverChannelId, response, receiveTimestamp)
        mitmActor ! ResponseReceived(response)

      case unknown =>
        logger.warn(s"Received unknown message: $unknown")
    }
  }

  override def channelInactive(ctx: ChannelHandlerContext): Unit =
    mitmActor ! ClientChannelInactive(ctx.channel.id)

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit =
    mitmActor ! ClientChannelException(cause)
}
