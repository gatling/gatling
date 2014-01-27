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

import io.gatling.recorder.controller.RecorderController
import io.gatling.recorder.http.channel.BootstrapFactory
import io.gatling.recorder.http.handler.ChannelFutures.function2ChannelFutureListener

class ServerHttpResponseHandler(controller: RecorderController, clientChannel: Channel, @volatile var request: HttpRequest, var expectConnect: Boolean) extends SimpleChannelHandler with StrictLogging {

	override def messageReceived(context: ChannelHandlerContext, event: MessageEvent) {

		context.sendUpstream(event)

		val serverChannel = context.getChannel

		event.getMessage match {
			case response: HttpResponse =>
				if (expectConnect) {
					expectConnect = false
					BootstrapFactory.upgradeProtocol(context.getChannel.getPipeline)
					serverChannel.write(ClientRequestHandler.buildRequestWithRelativeURI(request))

				} else {
					controller.receiveResponse(request, response)

					val requestConnectionHeader = Option(request.headers.get(HttpHeaders.Names.CONNECTION))

					// FIXME not very clean
					request = null

					clientChannel.write(response).addListener { future: ChannelFuture =>

						val keepAlive = (for {
							requestKeepAlive <- requestConnectionHeader if (HttpHeaders.Values.KEEP_ALIVE.equalsIgnoreCase(requestKeepAlive))
							responseKeepAlive <- Option(response.headers.get(HttpHeaders.Names.CONNECTION))
						} yield HttpHeaders.Values.KEEP_ALIVE.equalsIgnoreCase(responseKeepAlive)).getOrElse(false)

						if (keepAlive) {
							logger.debug("Both request and response are willing to keep the connection alive, reusing channels")
						} else {
							logger.debug("Request and/or response is not willing to keep the connection alive, closing both channels")
							serverChannel.close
							clientChannel.close
						}
					}
				}
			case unknown => logger.warn(s"Received unknown message: $unknown")
		}
	}
}
