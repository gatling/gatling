/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import java.net.InetSocketAddress

import scala.util.control.NonFatal

import io.gatling.recorder.http.HttpProxy
import io.gatling.recorder.http.handler.ScalaChannelHandler

import org.asynchttpclient.uri.Uri
import org.jboss.netty.channel.{ Channel, ChannelFuture }
import org.jboss.netty.handler.codec.http.{ DefaultHttpResponse, HttpRequest, HttpResponseStatus, HttpVersion }

private[user] class HttpUserHandler(proxy: HttpProxy) extends UserHandler(proxy) with ScalaChannelHandler {

  private def buildRequestWithRelativeURI(request: HttpRequest): HttpRequest = {
    val relative = Uri.create(request.getUri).toRelativeUrl
    copyRequestWithNewUri(request, relative)
  }

  private def writeRequest(userChannel: Channel, request: HttpRequest): Unit = {
    val remoteRequest = proxy.outgoingProxy match {
      case None => buildRequestWithRelativeURI(request)
      case _    => request
    }

    writeRequestToRemote(userChannel, remoteRequest, request)
  }

  private def writeRequestWithNewChannel(userChannel: Channel, request: HttpRequest): Unit = {
    _remoteChannel = None

    val inetSocketAddress = proxy.outgoingProxy match {
      case Some((host, port)) =>
        new InetSocketAddress(host, port)

      case None if request.getUri.startsWith("/") =>
        throw new IllegalArgumentException(s"Request url ${request.getUri} is relative, you're probably directly hitting the proxy")

      case None =>
        try {
          computeInetSocketAddress(Uri.create(request.getUri))
        } catch {
          case NonFatal(e) =>
            throw new RuntimeException(s"Could not build address requestURI='${request.getUri}'", e)
        }
    }

    proxy.remoteBootstrap
      .connect(inetSocketAddress)
      .addListener { future: ChannelFuture =>
        if (future.isSuccess) {
          val remoteChannel = future.getChannel
          setupRemoteChannel(userChannel, remoteChannel, proxy.controller, performConnect = false, reconnect = false)
          writeRequest(userChannel, request)
        } else {
          val t = future.getCause
          logger.error(t.getMessage, t)
          // FIXME could be 404 or 500 depending on exception
          userChannel.write(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND))
        }
      }
  }

  def propagateRequest(userChannel: Channel, request: HttpRequest): Unit =
    _remoteChannel match {
      case Some(remoteChannel) if remoteChannel.isConnected =>

        val remoteAddress = remoteChannel.getRemoteAddress.asInstanceOf[InetSocketAddress]
        val requestUri = Uri.create(request.getUri)

        if (remoteAddress.getHostString != requestUri.getHost || remoteAddress.getPort != defaultPort(requestUri)) {
          // not connected to the proper remote
          logger.debug(s"User channel ${userChannel.getId} remote peer ${remoteChannel.getId} is not connected to the proper host, closing it")
          remoteChannel.close()
          writeRequestWithNewChannel(userChannel, request)

        } else
          writeRequest(userChannel, request)

      case _ =>
        writeRequestWithNewChannel(userChannel, request)
    }
}
