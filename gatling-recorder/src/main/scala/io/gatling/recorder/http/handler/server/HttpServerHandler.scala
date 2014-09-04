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

import java.net.InetSocketAddress

import com.ning.http.client.uri.Uri
import io.gatling.recorder.http.HttpProxy
import io.gatling.recorder.http.handler.ScalaChannelHandler
import org.jboss.netty.channel.{ Channel, ChannelFuture }
import org.jboss.netty.handler.codec.http.{ DefaultHttpResponse, HttpRequest, HttpResponseStatus, HttpVersion }

class HttpServerHandler(proxy: HttpProxy) extends ServerHandler(proxy) with ScalaChannelHandler {

  private def buildRequestWithRelativeURI(request: HttpRequest): HttpRequest = {
    val relative = Uri.create(request.getUri).toRelativeUrl
    copyRequestWithNewUri(request, relative)
  }

  private def writeRequest(clientChannel: Channel, request: HttpRequest): Unit = {
    val clientRequest = proxy.outgoingProxy match {
      case None => buildRequestWithRelativeURI(request)
      case _    => request
    }

    writeRequestToClient(clientChannel, clientRequest, request)
  }

  private def writeRequestWithNewChannel(serverChannel: Channel, request: HttpRequest): Unit = {
    _clientChannel = None

    val inetSocketAddress = proxy.outgoingProxy match {
      case Some((host, port)) => new InetSocketAddress(host, port)
      case _ =>
        try {
          computeInetSocketAddress(Uri.create(request.getUri))
        } catch {
          case e: Exception =>
            throw new RuntimeException(s"Could not build address requestURI='${request.getUri}'", e)
        }
    }

    proxy.clientBootstrap
      .connect(inetSocketAddress)
      .addListener { future: ChannelFuture =>
        if (future.isSuccess) {
          val clientChannel = future.getChannel
          setupClientChannel(clientChannel, proxy.controller, serverChannel, performConnect = false)
          writeRequest(clientChannel, request)
        } else {
          val t = future.getCause
          logger.error(t.getMessage, t)
          // FIXME could be 404 or 500 depending on exception
          val response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND)
          serverChannel.write(response)
        }
      }
  }

  def propagateRequest(serverChannel: Channel, request: HttpRequest): Unit =
    _clientChannel match {
      case Some(clientChannel) if clientChannel.isConnected && clientChannel.isOpen =>

        val remoteAddress = clientChannel.getRemoteAddress.asInstanceOf[InetSocketAddress]
        val requestUri = Uri.create(request.getUri)

        if (remoteAddress.getHostString != requestUri.getHost || remoteAddress.getPort != defaultPort(requestUri)) {
          // not connected to the proper remote
          logger.debug("Client counterpart is not connected to the proper remote, closing it")
          clientChannel.close()
          writeRequestWithNewChannel(serverChannel, request)

        } else
          writeRequest(clientChannel, request)

      case _ =>
        writeRequestWithNewChannel(serverChannel, request)
    }
}
