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

import org.jboss.netty.channel.{ Channel, ChannelFuture, ChannelHandlerContext, MessageEvent, SimpleChannelHandler }
import org.jboss.netty.handler.codec.http.{ HttpHeaders, HttpRequest, HttpResponse }

import com.typesafe.scalalogging.slf4j.StrictLogging

import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.recorder.controller.RecorderController
import io.gatling.recorder.http.channel.BootstrapFactory
import io.gatling.recorder.http.handler.ChannelFutures.function2ChannelFutureListener
import io.gatling.http.util.HttpHelper.OkCodes

case class TimedHttpRequest(httpRequest: HttpRequest, sendTime: Long = nowMillis)

// FIXME ugly
class ServerHttpResponseHandler(controller: RecorderController, clientChannel: Channel, @volatile var request: TimedHttpRequest = null, var expectConnect: Boolean = false) extends SimpleChannelHandler with StrictLogging {

  override def messageReceived(context: ChannelHandlerContext, event: MessageEvent): Unit = {

      def isKeepAlive(headers: HttpHeaders) = Option(headers.get(HttpHeaders.Names.CONNECTION)).map(HttpHeaders.Values.KEEP_ALIVE.equalsIgnoreCase).getOrElse(false)

    context.sendUpstream(event)

    val serverChannel = context.getChannel

    event.getMessage match {
      case response: HttpResponse =>

        if (expectConnect) {
          expectConnect = false
          BootstrapFactory.upgradeProtocol(context.getChannel.getPipeline)
          serverChannel.write(ClientRequestHandler.buildRequestWithRelativeURI(request.httpRequest))

        } else {
          val keepAlive = isKeepAlive(request.httpRequest.headers) && isKeepAlive(response.headers)

          controller.receiveResponse(request, response)

          // FIXME ugly
          request = null

          clientChannel.write(response).addListener { future: ChannelFuture =>

            if (keepAlive && OkCodes.contains(response.getStatus.getCode)) {
              logger.debug("Both request and response are willing to keep the connection alive, reusing channels")
            } else {
              logger.debug("Request and/or response is not willing to keep the connection alive, closing both channels")
              clientChannel.close
              serverChannel.close
            }
          }
        }
      case unknown => logger.warn(s"Received unknown message: $unknown")
    }
  }
}
