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

import java.net.URI

import scala.collection.JavaConversions.asScalaBuffer

import org.jboss.netty.channel.{ ChannelFuture, ChannelFutureListener, ChannelHandlerContext, ExceptionEvent, MessageEvent, SimpleChannelHandler }
import org.jboss.netty.handler.codec.http.{ DefaultHttpRequest, HttpRequest }

import com.ning.http.util.Base64
import com.typesafe.scalalogging.slf4j.Logging

import io.gatling.http.Headers
import io.gatling.recorder.config.RecorderConfiguration.configuration
import io.gatling.recorder.controller.RecorderController

abstract class AbstractBrowserRequestHandler(controller: RecorderController) extends SimpleChannelHandler with Logging {

	implicit def function2ChannelFutureListener(thunk: ChannelFuture => Any) = new ChannelFutureListener {
		def operationComplete(future: ChannelFuture) { thunk(future) }
	}

	override def messageReceived(ctx: ChannelHandlerContext, event: MessageEvent) {

		event.getMessage match {
			case request: HttpRequest =>
				configuration.proxy.outgoing.host.map { _ =>
					for {
						username <- configuration.proxy.outgoing.username
						password <- configuration.proxy.outgoing.password
					} {
						val proxyAuth = "Basic " + Base64.encode((username + ":" + password).getBytes)
						request.setHeader(Headers.Names.PROXY_AUTHORIZATION, proxyAuth)
					}
				}.getOrElse(request.removeHeader("Proxy-Connection")) // remove Proxy-Connection header if it's not significant

				propagateRequest(ctx, request)

				controller.receiveRequest(request)

			case unknown => logger.warn(s"Received unknown message: $unknown")
		}
	}

	def propagateRequest(requestContext: ChannelHandlerContext, request: HttpRequest)

	override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
		logger.error("Exception caught", e.getCause)

		// Properly closing
		val future = ctx.getChannel.getCloseFuture
		future.addListener(ChannelFutureListener.CLOSE)
		ctx.sendUpstream(e)
	}

	def buildRequestWithRelativeURI(request: HttpRequest) = {
		val originalURI = new URI(request.getUri)
		val relativeURI = new URI(null, null, originalURI.getPath, originalURI.getQuery, originalURI.getFragment)
		val relativeURIString = relativeURI.toString match {
			case s if s.startsWith("/") => s
			case s => "/" + s
		}

		val newRequest = new DefaultHttpRequest(request.getProtocolVersion, request.getMethod, relativeURIString)
		newRequest.setChunked(request.isChunked)
		newRequest.setContent(request.getContent)
		for (header <- request.getHeaders) newRequest.addHeader(header.getKey, header.getValue)
		newRequest
	}
}
