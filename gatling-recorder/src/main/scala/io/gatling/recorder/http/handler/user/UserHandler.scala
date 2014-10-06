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

import java.net.InetSocketAddress

import com.ning.http.client.uri.Uri
import com.ning.http.util.Base64
import com.typesafe.scalalogging.slf4j.StrictLogging
import io.gatling.http.HeaderNames
import io.gatling.recorder.controller.RecorderController
import io.gatling.recorder.http.HttpProxy
import io.gatling.recorder.http.channel.BootstrapFactory._
import io.gatling.recorder.http.handler.remote.{ TimedHttpRequest, RemoteHandler }
import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http.{ HttpChunk, HttpRequest }

abstract class UserHandler(proxy: HttpProxy) extends SimpleChannelHandler with StrictLogging {

  @volatile var _remoteChannel: Option[Channel] = None

  override def messageReceived(ctx: ChannelHandlerContext, event: MessageEvent): Unit =
    event.getMessage match {
      case request: HttpRequest =>

        logger.debug(s"Received request on user channel ${ctx.getChannel.getId} remote peer is ${_remoteChannel.map(_.getId)}")

        proxy.outgoingProxy match {
          case None =>
            // remove Proxy-Connection header if it's not significant
            request.headers.remove("Proxy-Connection")

          case _ =>
            for {
              username <- proxy.outgoingUsername
              password <- proxy.outgoingPassword
              proxyAuth = "Basic " + Base64.encode((username + ":" + password).getBytes)
            } request.headers.set(HeaderNames.ProxyAuthorization, proxyAuth)
        }

        propagateRequest(ctx.getChannel, request)

        proxy.controller.receiveRequest(request)

      case chunk: HttpChunk =>
        _remoteChannel.foreach { remoteChannel =>
          if (remoteChannel.isConnected)
            remoteChannel.write(chunk)
        }

      case unknown => logger.warn(s"Received unknown message: $unknown")
    }

  def propagateRequest(userChannel: Channel, request: HttpRequest): Unit

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent): Unit = {
    logger.error(s"Exception caught on user channel ${ctx.getChannel.getId}", e.getCause)

    if (ctx.getChannel.isReadable) {
      logger.debug(s"Exception, closing user channel ${ctx.getChannel.getId}")
      ctx.getChannel.close()
    }
    _remoteChannel.foreach { remoteChannel =>
      if (remoteChannel.isReadable) {
        logger.debug(s"Exception, closing remote channel ${ctx.getChannel.getId} too")
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

  def writeRequestToRemote(userChannel: Channel, remoteRequest: HttpRequest, loggedRequest: HttpRequest): Unit =
    _remoteChannel.foreach { remoteChannel =>
      remoteChannel.getPipeline.getContext(GatlingHandlerName).setAttachment(TimedHttpRequest(loggedRequest))
      logger.debug(s"Propagating request from user channel ${userChannel.getId} to remote channel ${remoteChannel.getId} connected=${remoteChannel.isConnected}")
      remoteChannel.write(remoteRequest)
    }

  def setupRemoteChannel(userChannel: Channel, remoteChannel: Channel, controller: RecorderController, performConnect: Boolean, reconnect: Boolean): Unit = {
    logger.debug(s"Attaching user channel ${userChannel.getId} and remote peer ${remoteChannel.getId}")
    _remoteChannel = Some(remoteChannel)
    remoteChannel.getPipeline.addLast(GatlingHandlerName, new RemoteHandler(controller, userChannel, performConnect, reconnect))
  }

  override def channelDisconnected(ctx: ChannelHandlerContext, event: ChannelStateEvent): Unit = {
    logger.debug(s"User channel ${ctx.getChannel.getId} was closed, remote peer is ${_remoteChannel.map(_.getId)}")
    super.channelDisconnected(ctx, event)
    _remoteChannel.foreach { remoteChannel =>
      _remoteChannel = None
      if (remoteChannel.isReadable) {
        logger.debug(s"User channel ${ctx.getChannel.getId} was closed, closing peer remote channel ${remoteChannel.getId}")
        remoteChannel.close()
      }
    }
  }
}
