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

import io.gatling.http.HeaderNames
import io.gatling.recorder.controller.RecorderController
import io.gatling.recorder.http.HttpProxy
import io.gatling.recorder.http.channel.BootstrapFactory._
import io.gatling.recorder.http.handler.remote.{ TimedHttpRequest, RemoteHandler }
import io.gatling.recorder.http.model.SafeHttpRequest

import com.typesafe.scalalogging.StrictLogging
import org.asynchttpclient.util.Base64
import org.asynchttpclient.uri.Uri
import io.netty.channel._
import io.netty.handler.codec.http.FullHttpRequest

private[user] abstract class UserHandler(proxy: HttpProxy) extends ChannelInboundHandlerAdapter with StrictLogging {

  @volatile var _remoteChannel: Option[Channel] = None

  override def channelRead(ctx: ChannelHandlerContext, msg: AnyRef): Unit =
    msg match {
      case request: FullHttpRequest =>

        logger.debug(s"Received request on user channel ${ctx.channel} remote peer is ${_remoteChannel}")

        proxy.outgoingProxy match {
          case None =>
            // remove Proxy-Connection header if it's not significant
            request.headers.remove("Proxy-Connection")

          case _ =>
            for {
              username <- proxy.outgoingUsername
              password <- proxy.outgoingPassword
            } {
              val proxyAuth = "Basic " + Base64.encode((username + ":" + password).getBytes)
              request.headers.set(HeaderNames.ProxyAuthorization, proxyAuth)
            }
        }

        val safeRequest = SafeHttpRequest.fromNettyRequest(request)

        propagateRequest(ctx.channel, safeRequest)

        proxy.controller.receiveRequest(safeRequest)

      case unknown => logger.warn(s"Received unknown message: $unknown")
    }

  def propagateRequest(userChannel: Channel, request: SafeHttpRequest): Unit

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    if (logger.underlying.isDebugEnabled)
      logger.error(s"Exception caught on user channel ${ctx.channel}", cause.getCause)
    else
      logger.error(s"Exception caught on user channel ${ctx.channel}: ${cause.getCause.getClass.getSimpleName} ${cause.getCause.getMessage}")

    if (ctx.channel.isActive) {
      logger.debug(s"Exception, closing user channel ${ctx.channel}")
      ctx.channel.close()
    }
    _remoteChannel.foreach { remoteChannel =>
      if (remoteChannel.isActive) {
        logger.debug(s"Exception, closing remote channel ${ctx.channel} too")
        remoteChannel.close()
      }
    }
  }

  def defaultPort(uri: Uri): Int =
    uri.getPort match {
      case -1 => uri.getScheme match {
        case "https" | "wss" => 443
        case _               => 80
      }
      case p => p
    }

  def computeInetSocketAddress(uri: Uri): InetSocketAddress =
    new InetSocketAddress(uri.getHost, defaultPort(uri))

  def writeRequestToRemote(userChannel: Channel, remoteRequest: SafeHttpRequest, loggedRequest: SafeHttpRequest): Unit =
    _remoteChannel.foreach { remoteChannel =>
      remoteChannel.pipeline.context(GatlingHandlerName).attr(TimedHttpRequestAttribute).set(TimedHttpRequest(loggedRequest))
      logger.debug(s"Propagating request from user channel $userChannel to remote channel $remoteChannel active=${remoteChannel.isActive}")
      remoteChannel.writeAndFlush(remoteRequest.toNettyRequest)
    }

  def setupRemoteChannel(userChannel: Channel, remoteChannel: Channel, controller: RecorderController, performConnect: Boolean, reconnect: Boolean): Unit = {
    logger.debug(s"Attaching user channel $userChannel and remote peer $remoteChannel")
    _remoteChannel = Some(remoteChannel)
    remoteChannel.pipeline.addLast(GatlingHandlerName, new RemoteHandler(controller, proxy.sslServerContext, userChannel, performConnect, reconnect))
  }

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    logger.debug(s"User channel ${ctx.channel} was closed, remote peer is ${_remoteChannel}")
    super.channelInactive(ctx)
    _remoteChannel.foreach { remoteChannel =>
      _remoteChannel = None
      if (remoteChannel.isActive) {
        logger.debug(s"User channel ${ctx.channel} was closed, closing peer remote channel $remoteChannel)}")
        remoteChannel.close()
      }
    }
  }
}
