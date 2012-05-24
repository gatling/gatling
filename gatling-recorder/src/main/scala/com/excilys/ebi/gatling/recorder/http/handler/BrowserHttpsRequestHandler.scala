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

import java.net.{ URI, InetSocketAddress }

import org.jboss.netty.channel.{ ChannelHandlerContext, ChannelFuture }
import org.jboss.netty.handler.codec.http.{ HttpVersion, HttpResponseStatus, HttpRequest, HttpMethod, DefaultHttpResponse }

import com.excilys.ebi.gatling.recorder.config.ProxyConfig
import com.excilys.ebi.gatling.recorder.controller.RecorderController
import com.excilys.ebi.gatling.recorder.http.channel.BootstrapFactory.newClientBootstrap

import grizzled.slf4j.Logging

class BrowserHttpsRequestHandler(proxyConfig: ProxyConfig) extends AbstractBrowserRequestHandler(proxyConfig: ProxyConfig) with Logging {

	@volatile var targetHostURI: URI = _

	def connectToServerOnBrowserRequestReceived(ctx: ChannelHandlerContext, request: HttpRequest): ChannelFuture = {

		info("Received " + request.getMethod + " on " + request.getUri)

		if (request.getMethod == HttpMethod.CONNECT) {

			targetHostURI = new URI("https://" + request.getUri());

			warn("Trying to connect to " + targetHostURI + ", make sure you've accepted the recorder certificate for this site")

			RecorderController.secureConnection(targetHostURI)

			ctx.getChannel.write(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK))

			null

		} else {
			// set full uri so that it's correctly recorded
			val fullUri = new StringBuilder().append(targetHostURI).append(request.getUri).toString
			request.setUri(fullUri)

			val bootstrap = newClientBootstrap(ctx, request, true)

			val (host, port) = (for {
				host <- proxyConfig.host
				port <- proxyConfig.port
			} yield (host, port)).getOrElse(targetHostURI.getHost, targetHostURI.getPort)

			bootstrap.connect(new InetSocketAddress(host, port))
		}
	}
}
