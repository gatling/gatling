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

import java.io.IOException
import java.net.InetSocketAddress

import io.gatling.recorder.http.HttpProxy
import io.gatling.recorder.http.channel.BootstrapFactory._
import io.gatling.recorder.http.handler.ScalaChannelHandler
import io.gatling.recorder.http.model.SafeHttpRequest

import com.typesafe.scalalogging.StrictLogging
import io.netty.util.concurrent.Future
import org.asynchttpclient.uri.Uri
import io.netty.channel.{ Channel, ChannelFuture, ChannelHandlerContext }
import io.netty.handler.codec.http._
import io.netty.handler.ssl.SslHandler

private[user] class HttpsUserHandler(proxy: HttpProxy) extends UserHandler(proxy) with ScalaChannelHandler with StrictLogging {

  var targetHostUri: Uri = _

  def propagateRequest(userChannel: Channel, request: SafeHttpRequest): Unit = {

      def handleConnect(reconnectRemote: Boolean): Unit = {

          def connectRemoteChannelThroughProxy(proxyAddress: InetSocketAddress): Unit =
            proxy.remoteBootstrap
              .connect(proxyAddress)
              .addListener { connectFuture: ChannelFuture =>
                val remoteChannel = connectFuture.channel
                setupRemoteChannel(userChannel, remoteChannel, proxy.controller, performConnect = true, reconnect = reconnectRemote)
                logger.debug(s"Write request ${request.toNettyRequest} to remoteChannel $remoteChannel")
                remoteChannel.writeAndFlush(request.toNettyRequest)
              }

          def connectRemoteChannelDirect(address: InetSocketAddress): Unit =
            proxy.secureRemoteBootstrap
              .connect(address)
              .addListener { connectFuture: ChannelFuture =>
                if (connectFuture.isSuccess) {
                  connectFuture.channel.pipeline.get(SslHandlerName) match {
                    case sslHandler: SslHandler =>
                      sslHandler.handshakeFuture.addListener { handshakeFuture: Future[Channel] =>

                        if (handshakeFuture.isSuccess) {
                          val remoteChannel = handshakeFuture.get
                          val inetSocketAddress = remoteChannel.remoteAddress.asInstanceOf[InetSocketAddress]
                          setupRemoteChannel(userChannel, remoteChannel, proxy.controller, performConnect = false, reconnect = reconnectRemote)
                          if (!reconnectRemote) {
                            userChannel.pipeline.addFirst(SslHandlerSetterName, new SslHandlerSetter(inetSocketAddress.getHostString, proxy.sslServerContext))
                            logger.debug(s"Write OK response to userChannel $userChannel")
                            userChannel.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK))
                          } else
                            handlePropagatableRequest()

                        } else
                          logger.error(s"Handshake failure with $address", handshakeFuture.cause)
                      }

                    case _ => throw new IllegalStateException("SslHandler missing from secureClientBootstrap")
                  }
                } else
                  logger.error(s"Could not connect to $address", connectFuture.cause)
              }

        // only real CONNECT has an absolute url with the host, not reconnection
        if (!reconnectRemote)
          targetHostUri = Uri.create("https://" + request.uri)

        proxy.outgoingProxy match {
          case Some((proxyHost, proxyPort)) => connectRemoteChannelThroughProxy(new InetSocketAddress(proxyHost, proxyPort))
          case _                            => connectRemoteChannelDirect(computeInetSocketAddress(targetHostUri))
        }
      }

      def handlePropagatableRequest(): Unit =
        _remoteChannel match {
          case Some(remoteChannel) if remoteChannel.isActive =>
            // set full uri so that it's correctly recorded
            val loggedRequest = request.copy(uri = Uri.create(targetHostUri, request.uri).toString)
            writeRequestToRemote(userChannel, request, loggedRequest)

          case _ =>
            _remoteChannel = None
            handleConnect(reconnectRemote = true)
        }

    logger.info(s"Received ${request.method} on ${request.uri}")
    request.method match {
      case HttpMethod.CONNECT => handleConnect(reconnectRemote = false)
      case _                  => handlePropagatableRequest()
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {

    cause match {
      case e: IOException =>
        logger.error(s"SslException, did you accept the certificate for $targetHostUri?")
        proxy.controller.secureConnection(targetHostUri)
      case _ =>
    }

    super.exceptionCaught(ctx, cause)
  }
}
