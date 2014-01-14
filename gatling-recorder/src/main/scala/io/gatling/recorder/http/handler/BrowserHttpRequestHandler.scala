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

import java.net.{ InetSocketAddress, URI }

import org.jboss.netty.channel.{ Channel, ChannelFuture, ChannelHandlerContext }
import org.jboss.netty.handler.codec.http.HttpRequest

import io.gatling.recorder.config.RecorderConfiguration.configuration
import io.gatling.recorder.http.HttpProxy
import io.gatling.recorder.http.channel.BootstrapFactory
import io.gatling.recorder.http.handler.ChannelFutures.function2ChannelFutureListener
import io.gatling.recorder.util.URIHelper

class BrowserHttpRequestHandler(proxy: HttpProxy) extends AbstractBrowserRequestHandler(proxy.controller) {

	private def writeRequest(request: HttpRequest, channel: Channel) {
		val relativeRequest = configuration.proxy.outgoing.host.map(_ => request).getOrElse(AbstractBrowserRequestHandler.buildRequestWithRelativeURI(request))
		channel.write(relativeRequest)
	}

	def propagateRequest(requestContext: ChannelHandlerContext, request: HttpRequest) {

		_clientChannel match {
			case Some(channel) if channel.isConnected => writeRequest(request, channel)
			case _ =>
				_clientChannel = None

				val (host, port) = (for {
					proxyHost <- configuration.proxy.outgoing.host
					proxyPort <- configuration.proxy.outgoing.port
				} yield (proxyHost, proxyPort))
					.getOrElse {
						// the URI sometimes contains invalid characters, so we truncate as we only need the host and port
						val (schemeHostPort, _) = URIHelper.splitURI(request.getUri)
						val uri = new URI(schemeHostPort)
						(uri.getHost, if (uri.getPort == -1) 80 else uri.getPort)
					}

				proxy.clientBootstrap
					.connect(new InetSocketAddress(host, port))
					.addListener { future: ChannelFuture =>
						future.getChannel.getPipeline.addLast(BootstrapFactory.GATLING_HANDLER_NAME, new ServerHttpResponseHandler(proxy.controller, requestContext, request, false))
						_clientChannel = Some(future.getChannel)
						writeRequest(request, future.getChannel)
					}
		}
	}
}
