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

import java.net.InetSocketAddress

import scala.util.control.NonFatal

import io.gatling.recorder.http.HttpProxy
import io.gatling.recorder.http.handler.ScalaChannelHandler
import io.gatling.recorder.http.model.SafeHttpRequest

import com.softwaremill.quicklens._
import org.asynchttpclient.uri.Uri
import io.netty.channel.{ Channel, ChannelFuture }
import io.netty.handler.codec.http._

private[user] class HttpUserHandler(proxy: HttpProxy) extends UserHandler(proxy) with ScalaChannelHandler {

  private def writeRequest(userChannel: Channel, request: SafeHttpRequest): Unit = {
    val remoteRequest = proxy.outgoingProxy match {
      case None => request.modify(_.uri).using(uri => Uri.create(uri).toRelativeUrl)
      case _    => request
    }

    writeRequestToRemote(userChannel, remoteRequest, request)
  }

  private def writeRequestWithNewChannel(userChannel: Channel, request: SafeHttpRequest): Unit = {
    _remoteChannel = None

    val inetSocketAddress = proxy.outgoingProxy match {
      case Some((host, port)) =>
        new InetSocketAddress(host, port)

      case None if request.uri.startsWith("/") =>
        throw new IllegalArgumentException(s"Request url ${request.uri} is relative, you're probably directly hitting the proxy")

      case None =>
        try {
          computeInetSocketAddress(Uri.create(request.uri))
        } catch {
          case NonFatal(e) =>
            throw new RuntimeException(s"Could not build address requestURI='${request.uri}'", e)
        }
    }

    proxy.remoteBootstrap
      .connect(inetSocketAddress)
      .addListener { future: ChannelFuture =>
        if (future.isSuccess) {
          val remoteChannel = future.channel
          setupRemoteChannel(userChannel, remoteChannel, proxy.controller, performConnect = false, reconnect = false)
          writeRequest(userChannel, request)
        } else {
          val t = future.cause
          logger.error(t.getMessage, t)
          // FIXME could be 404 or 500 depending on exception
          userChannel.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND))
        }
      }
  }

  override def propagateRequest(userChannel: Channel, request: SafeHttpRequest): Unit =
    _remoteChannel match {
      case Some(remoteChannel) if remoteChannel.isActive =>

        val remoteAddress = remoteChannel.remoteAddress.asInstanceOf[InetSocketAddress]
        val requestUri = Uri.create(request.uri)

        if (remoteAddress.getHostString != requestUri.getHost || remoteAddress.getPort != defaultPort(requestUri)) {
          // not connected to the proper remote
          logger.debug(s"User channel $userChannel remote peer $remoteChannel is not connected to the proper host, closing it")
          remoteChannel.close()
          writeRequestWithNewChannel(userChannel, request)

        } else
          writeRequest(userChannel, request)

      case _ =>
        writeRequestWithNewChannel(userChannel, request)
    }
}
