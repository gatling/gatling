/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.recorder.http.handler.remote

import io.gatling.recorder.http.handler.user.SslHandlerSetter

import com.typesafe.scalalogging.slf4j.StrictLogging
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.recorder.controller.RecorderController
import io.gatling.recorder.http.channel.BootstrapFactory._
import io.gatling.recorder.http.handler.ScalaChannelHandler
import io.gatling.recorder.http.ssl.SSLEngineFactory
import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.ssl.SslHandler

case class TimedHttpRequest(httpRequest: HttpRequest, sendTime: Long = nowMillis)

class RemoteHandler(controller: RecorderController, userChannel: Channel, var performConnect: Boolean, reconnect: Boolean)
    extends SimpleChannelHandler with ScalaChannelHandler with StrictLogging {

  override def messageReceived(ctx: ChannelHandlerContext, event: MessageEvent): Unit = {

      def handleConnect(response: HttpResponse): Unit = {

          def upgradeRemotePipeline(remotePipeline: ChannelPipeline, clientSslHandler: SslHandler): Unit = {
            // the HttpClientCodec has to be regenerated, don't ask me why...
            remotePipeline.replace(CodecHandlerName, CodecHandlerName, new HttpClientCodec)
            remotePipeline.addFirst(SslHandlerName, clientSslHandler)
          }

        if (response.getStatus == HttpResponseStatus.OK) {
          performConnect = false
          val remoteSslHandler = new SslHandler(SSLEngineFactory.newClientSSLEngine)
          upgradeRemotePipeline(ctx.getChannel.getPipeline, remoteSslHandler)

          // if we're reconnecting, server channel is already set up
          if (!reconnect)
            remoteSslHandler.handshake.addListener { handshakeFuture: ChannelFuture =>
              // TODO here, we could generate a certificate for this given peer, even based on Session principal if it could be authenticated
              userChannel.getPipeline.addFirst(SslHandlerName, new SslHandlerSetter)
              userChannel.write(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK))
            }
        } else
          throw new UnsupportedOperationException(s"Outgoing proxy refused to connect: ${response.getStatus}")
      }

      def handleRequest(response: HttpResponse): Unit =
        ctx.getAttachment match {
          case request: TimedHttpRequest =>
            controller.receiveResponse(request, response)

            ctx.setAttachment(null)

            if (userChannel.isConnected) {
              logger.debug(s"Write response to user channel ${userChannel.getId}")
              userChannel.write(response)

            } else
              logger.error(s"Can't write response to disconnected user channel ${userChannel.getId}, aborting request:${request.httpRequest.getUri}")

          case _ => throw new IllegalStateException("Couldn't find request attachment")
        }

    event.getMessage match {
      case response: HttpResponse =>
        if (performConnect)
          handleConnect(response)
        else
          handleRequest(response)

      case unknown => logger.warn(s"Received unknown message: $unknown")
    }
  }
}
