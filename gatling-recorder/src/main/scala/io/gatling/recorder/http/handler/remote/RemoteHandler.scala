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
package io.gatling.recorder.http.handler.remote

import java.net.InetSocketAddress

import io.gatling.commons.util.TimeHelper.nowMillis
import io.gatling.recorder.controller.RecorderController
import io.gatling.recorder.http.channel.BootstrapFactory._
import io.gatling.recorder.http.handler.ScalaChannelHandler
import io.gatling.recorder.http.handler.user.SslHandlerSetter
import io.gatling.recorder.http.model.{ SafeHttpRequest, SafeHttpResponse }
import io.gatling.recorder.http.ssl.{ SslClientContext, SslServerContext }

import com.typesafe.scalalogging.StrictLogging
import io.netty.channel._
import io.netty.handler.codec.http._
import io.netty.handler.ssl.SslHandler
import io.netty.util.concurrent.Future

private[recorder] case class TimedHttpRequest(httpRequest: SafeHttpRequest, sendTime: Long = nowMillis)

private[handler] class RemoteHandler(
    controller:         RecorderController,
    sslServerContext:   SslServerContext,
    userChannel:        Channel,
    var performConnect: Boolean,
    reconnect:          Boolean
) extends ChannelInboundHandlerAdapter with ScalaChannelHandler with StrictLogging {

  override def channelRead(ctx: ChannelHandlerContext, msg: AnyRef): Unit = {

      def handleConnect(response: SafeHttpResponse): Unit = {

          def upgradeRemotePipeline(remotePipeline: ChannelPipeline, clientSslHandler: SslHandler): Unit = {
            // the HttpClientCodec has to be regenerated, don't ask me why...
            remotePipeline.replace(CodecHandlerName, CodecHandlerName, new HttpClientCodec)
            remotePipeline.addFirst(SslHandlerName, clientSslHandler)
          }

        if (response.status == HttpResponseStatus.OK) {
          performConnect = false
          val remoteSslHandler = new SslHandler(SslClientContext.createSSLEngine)
          upgradeRemotePipeline(ctx.channel.pipeline, remoteSslHandler)

          // if we're reconnecting, server channel is already set up
          if (!reconnect)
            remoteSslHandler.handshakeFuture.addListener { handshakeFuture: Future[Channel] =>
              if (handshakeFuture.isSuccess) {
                val inetSocketAddress = handshakeFuture.get.remoteAddress.asInstanceOf[InetSocketAddress]
                userChannel.pipeline.addFirst(SslHandlerSetterName, new SslHandlerSetter(inetSocketAddress.getHostString, sslServerContext))
                userChannel.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK))
              } else {
                logger.error(s"Handshake failure", handshakeFuture.cause)
              }
            }
        } else
          throw new UnsupportedOperationException(s"Outgoing proxy refused to connect: ${response.status}")
      }

      def handleResponse(response: SafeHttpResponse): Unit =
        ctx.attr(TimedHttpRequestAttribute).getAndSet(null) match {
          case request: TimedHttpRequest =>
            controller.receiveResponse(request, response)

            if (userChannel.isActive) {
              logger.debug(s"Write response $response to user channel $userChannel")
              userChannel.writeAndFlush(response.toNettyResponse)

            } else
              logger.error(s"Can't write response to disconnected user channel $userChannel, aborting request:${request.httpRequest.uri}")

          case _ => throw new IllegalStateException("Couldn't find request attribute")
        }

    msg match {
      case response: FullHttpResponse =>

        val safeResponse = SafeHttpResponse.fromNettyResponse(response)

        if (performConnect)
          handleConnect(safeResponse)
        else
          handleResponse(safeResponse)

      case unknown => logger.warn(s"Received unknown message: $unknown")
    }
  }
}
