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
import java.net.{ InetSocketAddress, URI }
import javax.net.ssl.SSLException

import com.typesafe.scalalogging.slf4j.StrictLogging
import io.gatling.recorder.http.HttpProxy
import io.gatling.recorder.http.channel.BootstrapFactory._
import io.gatling.recorder.http.handler.ScalaChannelHandler
import io.gatling.recorder.http.ssl.SSLEngineFactory
import org.jboss.netty.channel.{ Channel, ChannelFuture, ChannelHandlerContext, ExceptionEvent }
import org.jboss.netty.handler.codec.http.{ DefaultHttpRequest, DefaultHttpResponse, HttpMethod, HttpRequest, HttpResponseStatus, HttpVersion }
import org.jboss.netty.handler.ssl.SslHandler

import scala.collection.JavaConversions.asScalaBuffer

class HttpsServerHandler(proxy: HttpProxy) extends ServerHandler(proxy) with ScalaChannelHandler with StrictLogging {

  var targetHostURI: URI = _

  def propagateRequest(serverChannel: Channel, request: HttpRequest): Unit = {

      def handleConnect(): Unit = {
        targetHostURI = new URI("https://" + request.getUri)
        serverChannel.write(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK))
        serverChannel.getPipeline.addFirst(SslHandlerName, new SslHandler(SSLEngineFactory.newServerSSLEngine))
      }

      def buildConnectRequest: HttpRequest = {
        val connect = new DefaultHttpRequest(request.getProtocolVersion, HttpMethod.CONNECT, s"${targetHostURI.getHost}:${targetHostURI.getPort}")
        for (header <- request.headers.entries) connect.headers.add(header.getKey, header.getValue)
        connect
      }

      def handlePropagatableRequest(): Unit = {

          def onceConnected(clientChannel: Channel, clientRequest: HttpRequest, performConnect: Boolean): Unit = {
            setupClientChannel(clientChannel, proxy.controller, serverChannel, performConnect)
            writeRequestToClient(clientChannel, clientRequest, request)
          }

          def newChannelConnect(address: InetSocketAddress): Unit =
            proxy.clientBootstrap
              .connect(address)
              .addListener { connectFuture: ChannelFuture =>
                onceConnected(connectFuture.getChannel, buildConnectRequest, performConnect = true)
              }

          def newChannelDirect(address: InetSocketAddress): Unit =
            proxy.secureClientBootstrap
              .connect(address)
              .addListener { connectFuture: ChannelFuture =>
                connectFuture.getChannel.getPipeline.get(SslHandlerName).asInstanceOf[SslHandler].handshake
                  .addListener { handshakeFuture: ChannelFuture =>
                    onceConnected(handshakeFuture.getChannel, buildRequestWithRelativeURI(request), performConnect = false)
                  }
              }

        // set full uri so that it's correctly recorded FIXME ugly
        request.setUri(targetHostURI.resolve(request.getUri).toString)

        _clientChannel match {
          case Some(clientChannel) if clientChannel.isConnected && clientChannel.isOpen =>
            writeRequestToClient(clientChannel, buildRequestWithRelativeURI(request), request)

          case _ =>
            _clientChannel = None
            proxy.outgoingProxy match {
              case Some((proxyHost, proxyPort)) => newChannelConnect(new InetSocketAddress(proxyHost, proxyPort))
              case _                            => newChannelDirect(computeInetSocketAddress(targetHostURI))
            }
        }
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
        ctx.getChannel.close
        _clientChannel.map(_.close)
      }

    e.getCause match {
      case ioe: IOException if ioe.getMessage == "Broken pipe" => handleSslException(ioe)
      case ssle: SSLException => handleSslException(ssle)
      case _ => super.exceptionCaught(ctx, e)
    }
  }
}
