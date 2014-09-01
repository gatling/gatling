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
package io.gatling.recorder.http.handler.server

import java.io.IOException
import java.net.InetSocketAddress
import javax.net.ssl.SSLException

import com.ning.http.client.uri.UriComponents
import com.typesafe.scalalogging.slf4j.StrictLogging
import io.gatling.recorder.http.HttpProxy
import io.gatling.recorder.http.channel.BootstrapFactory._
import io.gatling.recorder.http.handler.ScalaChannelHandler
import org.jboss.netty.channel.{ Channel, ChannelFuture, ChannelHandlerContext, ExceptionEvent }
import org.jboss.netty.handler.codec.http.{ DefaultHttpResponse, HttpMethod, HttpRequest, HttpResponseStatus, HttpVersion }
import org.jboss.netty.handler.ssl.SslHandler

class HttpsServerHandler(proxy: HttpProxy) extends ServerHandler(proxy) with ScalaChannelHandler with StrictLogging {

  var targetHostURI: UriComponents = _

  def propagateRequest(serverChannel: Channel, request: HttpRequest): Unit = {

      def handleConnect(): Unit = {

          def connectClientChannelThroughProxy(proxyAddress: InetSocketAddress): Unit =
            proxy.clientBootstrap
              .connect(proxyAddress)
              .addListener { connectFuture: ChannelFuture =>
                val clientChannel = connectFuture.getChannel
                setupClientChannel(clientChannel, proxy.controller, serverChannel, performConnect = true)
                clientChannel.write(request)
              }

          def connectClientChannelDirect(address: InetSocketAddress): Unit =
            proxy.secureClientBootstrap
              .connect(address)
              .addListener { connectFuture: ChannelFuture =>

                connectFuture.getChannel.getPipeline.get(SslHandlerName) match {
                  case sslHandler: SslHandler =>
                    sslHandler.handshake
                      .addListener { handshakeFuture: ChannelFuture =>
                        val clientChannel = handshakeFuture.getChannel
                        // TODO build certificate for peer
                        setupClientChannel(clientChannel, proxy.controller, serverChannel, performConnect = false)
                        serverChannel.getPipeline.addFirst(SslHandlerName, new SslHandlerSetter)
                        serverChannel.write(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK))
                      }

                  case _ => throw new IllegalStateException("SslHandler missing from secureClientBootstrap")
                }
              }

        targetHostURI = UriComponents.create("https://" + request.getUri)

        proxy.outgoingProxy match {
          case Some((proxyHost, proxyPort)) => connectClientChannelThroughProxy(new InetSocketAddress(proxyHost, proxyPort))
          case _                            => connectClientChannelDirect(computeInetSocketAddress(targetHostURI))
        }
      }

      def handlePropagatableRequest(): Unit =
        _clientChannel match {
          case Some(clientChannel) if clientChannel.isConnected && clientChannel.isOpen =>
            // set full uri so that it's correctly recorded
            val absoluteUri = UriComponents.create(targetHostURI, request.getUri).toString
            val loggedRequest = copyRequestWithNewUri(request, absoluteUri)
            writeRequestToClient(clientChannel, request, loggedRequest)

          case _ =>
            _clientChannel = None
            throw new IllegalStateException("Server channel is open but client channel is closed?!")
        }

    logger.info(s"Received ${request.getMethod} on ${request.getUri}")
    request.getMethod match {
      case HttpMethod.CONNECT => handleConnect()
      case _                  => handlePropagatableRequest()
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent): Unit = {

      def handleSslException(e: Exception): Unit = {
        logger.error(s"${e.getClass.getSimpleName} ${e.getMessage}, did you accept the certificate for $targetHostURI?")
        proxy.controller.secureConnection(targetHostURI)
        if (ctx.getChannel.isReadable)
          ctx.getChannel.close()
        _clientChannel.foreach { clientChannel =>
          if (clientChannel.isReadable)
            clientChannel.close()
        }
      }

    e.getCause match {
      case ioe: IOException if ioe.getMessage == "Broken pipe" => handleSslException(ioe)
      case ssle: SSLException => handleSslException(ssle)
      case _ => super.exceptionCaught(ctx, e)
    }
  }
}
