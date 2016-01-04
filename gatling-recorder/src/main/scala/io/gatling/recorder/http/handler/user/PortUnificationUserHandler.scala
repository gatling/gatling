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
package io.gatling.recorder.http.handler.user

import io.gatling.recorder.http.HttpProxy
import io.gatling.recorder.http.channel.BootstrapFactory._

import com.typesafe.scalalogging.StrictLogging
import io.netty.channel.{ ChannelHandlerContext, ChannelInboundHandlerAdapter, ChannelPipeline }
import io.netty.handler.codec.http.{ HttpMethod, HttpRequest }

private[http] class PortUnificationUserHandler(proxy: HttpProxy, pipeline: ChannelPipeline) extends ChannelInboundHandlerAdapter with StrictLogging {

  override def channelRead(ctx: ChannelHandlerContext, msg: AnyRef): Unit =
    try
      msg match {
        case request: HttpRequest =>
          val serverHandler =
            if (request.getMethod.toString == HttpMethod.CONNECT.name)
              new HttpsUserHandler(proxy)
            else
              new HttpUserHandler(proxy)
          pipeline.addLast(GatlingHandlerName, serverHandler)
          pipeline.remove(PortUnificationServerHandler)

        case unknown => logger.warn(s"Received unknown message: $unknown")
      }

    finally
      super.channelRead(ctx, msg)
}
