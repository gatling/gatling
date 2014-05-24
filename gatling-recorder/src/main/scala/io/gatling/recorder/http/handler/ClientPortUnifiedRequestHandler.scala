/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.recorder.http.handler

import org.jboss.netty.channel.{ Channels, SimpleChannelHandler, ChannelPipeline, ChannelHandlerContext, MessageEvent }
import org.jboss.netty.handler.codec.http.{ HttpMethod, HttpRequest }
import com.typesafe.scalalogging.slf4j.StrictLogging
import io.gatling.recorder.http.HttpProxy
import io.gatling.recorder.http.channel.BootstrapFactory

class ClientPortUnifiedRequestHandler(proxy: HttpProxy, pipeline: ChannelPipeline) extends SimpleChannelHandler with StrictLogging {

  override def messageReceived(requestContext: ChannelHandlerContext, event: MessageEvent): Unit =
    try {
      event.getMessage match {
        case request: HttpRequest =>
          if (request.getMethod.toString == HttpMethod.CONNECT.getName)
            BootstrapFactory.setGatlingProtocolHandler(pipeline, new ClientHttpsRequestHandler(proxy))
          else
            BootstrapFactory.setGatlingProtocolHandler(pipeline, new ClientHttpRequestHandler(proxy))

        case unknown => logger.warn("Received unknown message: $unknown , in event : " + event)
      }

    } finally {
      Channels.fireMessageReceived(requestContext, event.getMessage)
    }
}
