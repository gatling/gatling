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
package io.gatling.recorder.http.handler.user

import java.io.IOException
import java.net.InetSocketAddress
import javax.net.ssl.SSLException

import com.ning.http.client.uri.Uri
import com.typesafe.scalalogging.slf4j.StrictLogging
import io.gatling.recorder.http.HttpProxy
import io.gatling.recorder.http.channel.BootstrapFactory._
import io.gatling.recorder.http.handler.ScalaChannelHandler
import org.jboss.netty.channel.{ Channel, ChannelFuture, ChannelHandlerContext, ExceptionEvent }
import org.jboss.netty.handler.codec.http.{ DefaultHttpResponse, HttpMethod, HttpRequest, HttpResponseStatus, HttpVersion }
import org.jboss.netty.handler.ssl.SslHandler

class HttpsUserHandler(proxy: HttpProxy) extends UserHandler(proxy) with ScalaChannelHandler with StrictLogging {

  var targetHostUri: Uri = _

  def propagateRequest(userChannel: Channel, request: HttpRequest): Unit = {

      def handleConnect(reconnect: Boolean): Unit = {

          def connectRemoteChannelThroughProxy(proxyAddress: InetSocketAddress): Unit =
            proxy.remoteBootstrap
              .connect(proxyAddress)
              .addListener { connectFuture: ChannelFuture =>
                val remoteChannel = connectFuture.getChannel
                setupRemoteChannel(userChannel, remoteChannel, proxy.controller, performConnect = true, reconnect = reconnect)
                remoteChannel.write(request)
              }

          def connectRemoteChannelDirect(address: InetSocketAddress): Unit =
            proxy.secureRemoteBootstrap
              .connect(address)
              .addListener { connectFuture: ChannelFuture =>

                connectFuture.getChannel.getPipeline.get(SslHandlerName) match {
                  case sslHandler: SslHandler =>
                    sslHandler.handshake
                      .addListener { handshakeFuture: ChannelFuture =>
                        val remoteChannel = handshakeFuture.getChannel
                        // TODO build certificate for peer
                        setupRemoteChannel(userChannel, remoteChannel, proxy.controller, performConnect = false, reconnect = reconnect)
                        if (!reconnect) {
                          userChannel.getPipeline.addFirst(SslHandlerName, new SslHandlerSetter)
                          userChannel.write(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK))

                        } else
                          remoteChannel.write(request)
                      }

                  case _ => throw new IllegalStateException("SslHandler missing from secureClientBootstrap")
                }
              }

        targetHostUri = Uri.create("https://" + request.getUri)

        proxy.outgoingProxy match {
          case Some((proxyHost, proxyPort)) => connectRemoteChannelThroughProxy(new InetSocketAddress(proxyHost, proxyPort))
          case _                            => connectRemoteChannelDirect(computeInetSocketAddress(targetHostUri))
        }
      }

      def handlePropagatableRequest(): Unit =
        _remoteChannel match {
          case Some(remoteChannel) if remoteChannel.isConnected =>
            // set full uri so that it's correctly recorded
            val absoluteUri = Uri.create(targetHostUri, request.getUri).toString
            val loggedRequest = copyRequestWithNewUri(request, absoluteUri)
            writeRequestToRemote(userChannel, request, loggedRequest)

          case _ =>
            _remoteChannel = None
            handleConnect(reconnect = true)
        }

    logger.info(s"Received ${request.getMethod} on ${request.getUri}")
    request.getMethod match {
      case HttpMethod.CONNECT => handleConnect(reconnect = false)
      case _                  => handlePropagatableRequest()
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent): Unit = {

      def handleSslException(e: Exception): Unit = {
        logger.error(s"${e.getClass.getSimpleName} ${e.getMessage}, did you accept the certificate for $targetHostUri?")
        proxy.controller.secureConnection(targetHostUri)
        if (ctx.getChannel.isReadable) {
          logger.debug(s"SSL exception, closing user channel ${ctx.getChannel.getId}")
          ctx.getChannel.close()
        }
        _remoteChannel.foreach { remoteChannel =>
          if (remoteChannel.isReadable) {
            logger.debug(s"SSL exception, closing remote channel ${ctx.getChannel.getId} too")
            remoteChannel.close()
          }
        }
      }

    e.getCause match {
      case ioe: IOException if ioe.getMessage == "Broken pipe" => handleSslException(ioe)
      case ssle: SSLException => handleSslException(ssle)
      case _ => super.exceptionCaught(ctx, e)
    }
  }
}
