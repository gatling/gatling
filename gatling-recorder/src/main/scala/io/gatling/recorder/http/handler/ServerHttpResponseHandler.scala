/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import scala.collection.JavaConversions.asScalaBuffer

import org.jboss.netty.channel.{ ChannelHandlerContext, MessageEvent, SimpleChannelHandler }
import org.jboss.netty.handler.codec.http.{ DefaultHttpRequest, HttpRequest, HttpResponse }

import com.typesafe.scalalogging.slf4j.StrictLogging

import io.gatling.recorder.controller.RecorderController
import io.gatling.recorder.http.channel.BootstrapFactory
import io.gatling.recorder.util.URIHelper

class ServerHttpResponseHandler(controller: RecorderController, requestContext: ChannelHandlerContext, request: HttpRequest, var expectConnect: Boolean) extends SimpleChannelHandler with StrictLogging {

	def buildRequestWithRelativeURI(request: HttpRequest) = {

		val (_, pathQuery) = URIHelper.splitURI(request.getUri)
		val newRequest = new DefaultHttpRequest(request.getProtocolVersion, request.getMethod, pathQuery)
		newRequest.setChunked(request.isChunked)
		newRequest.setContent(request.getContent)
		for (header <- request.headers.entries) newRequest.headers.add(header.getKey, header.getValue)
		newRequest
	}

	override def messageReceived(context: ChannelHandlerContext, event: MessageEvent) {

		context.sendUpstream(event)

		event.getMessage match {
			case response: HttpResponse =>
				if (expectConnect) {
					expectConnect = false
					BootstrapFactory.upgradeProtocol(context.getChannel.getPipeline, controller, context, request)
					context.getChannel.write(buildRequestWithRelativeURI(request))

				} else {
					controller.receiveResponse(request, response)
					requestContext.getChannel.write(response)
				}
			case unknown => logger.warn(s"Received unknown message: $unknown")
		}
	}
}
