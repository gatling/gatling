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

import org.jboss.netty.channel.ChannelFuture
import org.jboss.netty.channel.ChannelFutureListener
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.ExceptionEvent
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.channel.SimpleChannelHandler
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.handler.codec.http.DefaultHttpRequest

import com.excilys.ebi.gatling.recorder.controller.RecorderController
import com.excilys.ebi.gatling.recorder.http.GatlingHttpProxy

import scala.collection.JavaConversions._

import grizzled.slf4j.Logging

import java.net.URI

abstract class AbstractBrowserRequestHandler(val outgoingProxyHost: Option[String], val outgoingProxyPort: Option[Int]) 
	extends SimpleChannelHandler with Logging {

	override def messageReceived(ctx: ChannelHandlerContext, event: MessageEvent) {

		GatlingHttpProxy.receiveMessage(ctx.getChannel)

		val request = event.getMessage.asInstanceOf[HttpRequest]

		// remove Proxy-Connection header if it's not significant
		if(outgoingProxyHost.isEmpty){
			request.removeHeader("Proxy-Connection")
		}

		val future = connectToServerOnBrowserRequestReceived(ctx, request)

		RecorderController.receiveRequest(request)

		sendRequestToServerAfterConnection(future, request);
	}

	def connectToServerOnBrowserRequestReceived(ctx: ChannelHandlerContext, request: HttpRequest): ChannelFuture

	override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
		error("Exception caught", e.getCause)

		// Properly closing
		val future = ctx.getChannel.getCloseFuture
		future.addListener(new ChannelFutureListener() {
			def operationComplete(future: ChannelFuture) = future.getChannel.close
		})
		ctx.sendUpstream(e)
	}

	private def sendRequestToServerAfterConnection(future: ChannelFuture, request: HttpRequest) {
		if (future != null)
			future.addListener(new ChannelFutureListener() {
				def operationComplete(future: ChannelFuture) = future.getChannel.write(buildRequestWithRelativeURI(request))
			})
	}
	
	private def buildRequestWithRelativeURI(request: HttpRequest) = {
		val uri = new URI(request.getUri)
		val newUri = new URI(null, null, null, -1, uri.getPath, uri.getQuery, uri.getFragment).toString
		val newRequest = new DefaultHttpRequest(request.getProtocolVersion, request.getMethod, newUri)
		newRequest.setChunked(request.isChunked)
		newRequest.setContent(request.getContent)
		for (header <- request.getHeaders)
			newRequest.addHeader(header.getKey, header.getValue)
		newRequest
	}
}
