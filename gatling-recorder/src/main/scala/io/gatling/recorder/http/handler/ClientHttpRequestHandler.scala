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

import java.net.{ InetSocketAddress, URI }

import org.jboss.netty.channel.{ Channel, ChannelFuture, ChannelHandlerContext }
import org.jboss.netty.handler.codec.http.{ DefaultHttpResponse, HttpRequest, HttpResponseStatus, HttpVersion }

import io.gatling.recorder.http.HttpProxy
import io.gatling.recorder.http.channel.BootstrapFactory
import io.gatling.recorder.http.handler.ChannelFutures.function2ChannelFutureListener
import io.gatling.recorder.util.URIHelper

class ClientHttpRequestHandler(proxy: HttpProxy) extends ClientRequestHandler(proxy) {

  private def writeRequest(request: HttpRequest, serverChannel: Channel) {
    serverChannel.getPipeline.get(classOf[ServerHttpResponseHandler]).request = request
    val relativeRequest = proxy.outgoingHost.map(_ => request).getOrElse(ClientRequestHandler.buildRequestWithRelativeURI(request))
    serverChannel.write(relativeRequest)
  }

  def propagateRequest(requestContext: ChannelHandlerContext, request: HttpRequest) {

    _serverChannel match {
      case Some(serverChannel) if serverChannel.isConnected && serverChannel.isOpen =>
        writeRequest(request, serverChannel)
      case _ =>
        _serverChannel = None

        val (host, port) = (for {
          proxyHost <- proxy.outgoingHost
          proxyPort <- proxy.outgoingPort
        } yield (proxyHost, proxyPort))
          .getOrElse {
            // the URI sometimes contains invalid characters, so we truncate as we only need the host and port
            val (schemeHostPort, _) = URIHelper.splitURI(request.getUri)
            val uri = new URI(schemeHostPort)
            (uri.getHost, if (uri.getPort == -1) 80 else uri.getPort)
          }

        proxy.clientBootstrap
          .connect(new InetSocketAddress(host, port))
          .addListener { future: ChannelFuture =>
            if (future.isSuccess) {
              val serverChannel = future.getChannel
              serverChannel.getPipeline.addLast(BootstrapFactory.GATLING_HANDLER_NAME, new ServerHttpResponseHandler(proxy.controller, requestContext.getChannel, request, false))
              _serverChannel = Some(serverChannel)
              writeRequest(request, serverChannel)
            } else {
              val t = future.getCause
              logger.error(t.getMessage, t)
              // FIXME could be 404 or 500 depending on exception
              val response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND)

              requestContext.getChannel.write(response)
            }
          }
    }
  }
}
