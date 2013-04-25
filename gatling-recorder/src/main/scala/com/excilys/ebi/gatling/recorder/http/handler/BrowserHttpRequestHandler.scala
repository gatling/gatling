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
package com.excilys.ebi.gatling.recorder.http.handler

import java.net.{ URI, InetSocketAddress }

import org.jboss.netty.channel.{ ChannelHandlerContext, ChannelFuture }
import org.jboss.netty.handler.codec.http.HttpRequest

import com.excilys.ebi.gatling.recorder.config.ProxyConfig
import com.excilys.ebi.gatling.recorder.controller.RecorderController
import com.excilys.ebi.gatling.recorder.http.channel.BootstrapFactory.newClientBootstrap

class BrowserHttpRequestHandler(controller: RecorderController, proxyConfig: ProxyConfig) extends AbstractBrowserRequestHandler(controller, proxyConfig) {

	def propagateRequest(requestContext: ChannelHandlerContext, request: HttpRequest) {

		val bootstrap = newClientBootstrap(controller, requestContext, request, false)

		val (proxyHost, proxyPort) = (for {
			host <- proxyConfig.host
			port <- proxyConfig.port
		} yield (host, port))
			.getOrElse {
				val uri = new URI(request.getUri)
				val port = if (uri.getPort == -1) 80 else uri.getPort
				(uri.getHost, port)
			}

		bootstrap
			.connect(new InetSocketAddress(proxyHost, proxyPort))
			.addListener { future: ChannelFuture => future.getChannel.write(buildRequestWithRelativeURI(request)) }
	}
}
