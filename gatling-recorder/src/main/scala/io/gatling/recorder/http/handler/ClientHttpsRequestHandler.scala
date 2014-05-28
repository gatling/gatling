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
package io.gatling.recorder.http.handler

import java.io.IOException
import java.net.{ InetSocketAddress, URI }

import scala.collection.JavaConversions.asScalaBuffer

import org.jboss.netty.channel.{ ChannelFuture, ChannelHandlerContext, ExceptionEvent }
import org.jboss.netty.handler.codec.http.{ DefaultHttpRequest, DefaultHttpResponse, HttpMethod, HttpRequest, HttpResponseStatus, HttpVersion }
import org.jboss.netty.handler.ssl.SslHandler

import com.typesafe.scalalogging.slf4j.StrictLogging

import io.gatling.recorder.http.HttpProxy
import io.gatling.recorder.http.channel.BootstrapFactory
import io.gatling.recorder.http.handler.ChannelFutures.function2ChannelFutureListener
import io.gatling.recorder.http.ssl.SSLEngineFactory
import javax.net.ssl.SSLException

class ClientHttpsRequestHandler(proxy: HttpProxy) extends ClientRequestHandler(proxy) with StrictLogging {

  var targetHostURI: URI = _

  def propagateRequest(requestContext: ChannelHandlerContext, request: HttpRequest): Unit = {

      def handleConnect(): Unit = {
        targetHostURI = new URI("https://" + request.getUri)
        requestContext.getChannel.write(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK))
        requestContext.getPipeline.addFirst(BootstrapFactory.SslHandlerName, new SslHandler(SSLEngineFactory.newServerSSLEngine))
      }

      def buildConnectRequest = {
        val connect = new DefaultHttpRequest(request.getProtocolVersion, HttpMethod.CONNECT, s"${targetHostURI.getHost}:${targetHostURI.getPort}")
        for (header <- request.headers.entries) connect.headers.add(header.getKey, header.getValue)
        connect
      }

      def handlePropagatableRequest(): Unit = {

          def handleConnect(address: InetSocketAddress) {
            proxy.clientBootstrap
              .connect(address)
              .addListener { connectFuture: ChannelFuture =>
                val serverChannel = connectFuture.getChannel
                serverChannel.getPipeline.addLast(BootstrapFactory.GatlingHandlerName, new ServerHttpResponseHandler(proxy.controller, requestContext.getChannel, TimedHttpRequest(request), true))
                _serverChannel = Some(serverChannel)
                serverChannel.write(buildConnectRequest)
              }
          }

          def handleDirect(address: InetSocketAddress): Unit = {
            proxy.secureClientBootstrap
              .connect(address)
              .addListener { connectFuture: ChannelFuture =>
                connectFuture.getChannel.getPipeline.get(BootstrapFactory.SslHandlerName).asInstanceOf[SslHandler].handshake
                  .addListener { handshakeFuture: ChannelFuture =>
                    val serverChannel = handshakeFuture.getChannel
                    serverChannel.getPipeline.addLast(BootstrapFactory.GatlingHandlerName, new ServerHttpResponseHandler(proxy.controller, requestContext.getChannel, TimedHttpRequest(request), false))
                    _serverChannel = Some(serverChannel)
                    serverChannel.write(ClientRequestHandler.buildRequestWithRelativeURI(request))
                  }
              }
          }

        // set full uri so that it's correctly recorded FIXME ugly
        request.setUri(targetHostURI.resolve(request.getUri).toString)

        _serverChannel match {
          case Some(serverChannel) if serverChannel.isConnected && serverChannel.isOpen =>
            serverChannel.getPipeline.get(classOf[ServerHttpResponseHandler]).request = TimedHttpRequest(request)
            serverChannel.write(ClientRequestHandler.buildRequestWithRelativeURI(request))

          case _ =>
            _serverChannel = None

            proxy.outgoingProxy match {
              case Some((proxyHost, proxyPort)) => handleConnect(new InetSocketAddress(proxyHost, proxyPort))
              case _                            => handleDirect(computeInetSocketAddress(targetHostURI))
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
        _serverChannel.map(_.close)
      }

    e.getCause match {
      case ioe: IOException if ioe.getMessage == "Broken pipe" => handleSslException(ioe)
      case ssle: SSLException => handleSslException(ssle)
      case _ => super.exceptionCaught(ctx, e)
    }
  }
}
