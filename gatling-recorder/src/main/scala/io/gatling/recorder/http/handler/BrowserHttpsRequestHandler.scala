/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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

import java.net.{ InetSocketAddress, URI }

import org.jboss.netty.channel.{ ChannelFuture, ChannelHandlerContext }
import org.jboss.netty.handler.codec.http.{ DefaultHttpResponse, HttpMethod, HttpRequest, HttpResponseStatus, HttpVersion }
import org.jboss.netty.handler.ssl.SslHandler

import com.typesafe.scalalogging.slf4j.Logging

import io.gatling.recorder.config.RecorderConfiguration.configuration
import io.gatling.recorder.controller.RecorderController
import io.gatling.recorder.http.channel.BootstrapFactory
import io.gatling.recorder.http.channel.BootstrapFactory.newClientBootstrap

class BrowserHttpsRequestHandler(controller: RecorderController) extends AbstractBrowserRequestHandler(controller) with Logging {

	@volatile var targetHostURI: URI = _

	def propagateRequest(requestContext: ChannelHandlerContext, request: HttpRequest) {

		def handleConnect {
			targetHostURI = new URI("https://" + request.getUri)
			logger.warn(s"Trying to connect to $targetHostURI, make sure you've accepted the recorder certificate for this site")
			controller.secureConnection(targetHostURI)
			requestContext.getChannel.write(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK))
		}

		def handlePropagatableRequest {
			// set full uri so that it's correctly recorded FIXME ugly
			request.setUri(targetHostURI + request.getUri)

			val bootstrap = newClientBootstrap(controller, requestContext, request, true)

			val (host, port) = (for {
				host <- configuration.proxy.outgoing.host
				port <- configuration.proxy.outgoing.port
			} yield (host, port)).getOrElse(targetHostURI.getHost, targetHostURI.getPort)

			bootstrap
				.connect(new InetSocketAddress(host, port))
				.addListener { connectFuture: ChannelFuture =>
					connectFuture.getChannel.getPipeline.get(BootstrapFactory.SSL_HANDLER_NAME).asInstanceOf[SslHandler].handshake.addListener { handshakeFuture: ChannelFuture =>
						handshakeFuture.getChannel.write(buildRequestWithRelativeURI(request))
					}
				}
		}

		logger.info(s"Received ${request.getMethod} on ${request.getUri}")
		if (request.getMethod == HttpMethod.CONNECT) handleConnect
		else handlePropagatableRequest
	}
}
