/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.recorder.http.handler;

import org.jboss.netty.channel.{ SimpleChannelHandler, MessageEvent, ChannelHandlerContext }
import org.jboss.netty.handler.codec.http.{ HttpResponse, HttpRequest }

import com.excilys.ebi.gatling.recorder.controller.RecorderController
import com.excilys.ebi.gatling.recorder.http.GatlingHttpProxy

class ServerHttpResponseHandler(requestContext: ChannelHandlerContext, request: HttpRequest) extends SimpleChannelHandler {

	override def messageReceived(context: ChannelHandlerContext, event: MessageEvent) {

		GatlingHttpProxy.receiveMessage(context.getChannel)

		val response = event.getMessage.asInstanceOf[HttpResponse]

		RecorderController.receiveResponse(request, response)

		// Send back to client
		requestContext.getChannel.write(response)
	}
}
