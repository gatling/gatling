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

import com.ning.http.client.uri.UriComponents
import com.ning.http.util.Base64
import com.typesafe.scalalogging.slf4j.StrictLogging
import io.gatling.http.HeaderNames
import io.gatling.recorder.controller.RecorderController
import io.gatling.recorder.http.HttpProxy
import io.gatling.recorder.http.channel.BootstrapFactory._
import io.gatling.recorder.http.handler.client.{ TimedHttpRequest, ClientHandler }
import org.jboss.netty.channel.{ Channel, ChannelHandlerContext, ExceptionEvent, MessageEvent, SimpleChannelHandler }
import org.jboss.netty.handler.codec.http.HttpRequest

abstract class ServerHandler(proxy: HttpProxy) extends SimpleChannelHandler with StrictLogging {

  var _clientChannel: Option[Channel] = None

  override def messageReceived(ctx: ChannelHandlerContext, event: MessageEvent): Unit =

    event.getMessage match {
      case request: HttpRequest =>

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

      case unknown => logger.warn(s"Received unknown message: $unknown")
    }

  def propagateRequest(serverChannel: Channel, request: HttpRequest): Unit

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent): Unit = {
    logger.error("Exception caught", e.getCause)
    ctx.getChannel.close()
    _clientChannel.map(_.close())
  }

  def computeInetSocketAddress(uri: UriComponents): InetSocketAddress = {

    val host = uri.getHost
    val port = uri.getPort match {
      case -1 => uri.getScheme match {
        case "https" | "wss" => 443
        case _               => 80
      }
      case p => p
    }
    new InetSocketAddress(host, port)
  }

  def writeRequestToClient(clientChannel: Channel, clientRequest: HttpRequest, loggedRequest: HttpRequest): Unit = {
    clientChannel.getPipeline.getContext(GatlingHandlerName).setAttachment(TimedHttpRequest(loggedRequest))
    clientChannel.write(clientRequest)
  }

  def setupClientChannel(clientChannel: Channel, controller: RecorderController, serverChannel: Channel, performConnect: Boolean): Unit = {
    clientChannel.getPipeline.addLast(GatlingHandlerName, new ClientHandler(controller, serverChannel, performConnect))
    _clientChannel = Some(clientChannel)
  }
}
