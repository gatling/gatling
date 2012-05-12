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

import java.net.InetSocketAddress
import java.net.URI

import org.jboss.netty.channel.ChannelFuture
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.handler.codec.http.HttpRequest

import com.excilys.ebi.gatling.recorder.config.ProxyConfig
import com.excilys.ebi.gatling.recorder.http.channel.BootstrapFactory.bootstrapFactory

class BrowserHttpRequestHandler(proxyConfig: ProxyConfig) extends AbstractBrowserRequestHandler(proxyConfig.host, proxyConfig.port) {

	def connectToServerOnBrowserRequestReceived(ctx: ChannelHandlerContext, request: HttpRequest): ChannelFuture = {

		val bootstrap = bootstrapFactory.newClientBootstrap(ctx, request, false)

		if(outgoingProxyHost.isDefined && outgoingProxyPort.isDefined) {
			bootstrap.connect(new InetSocketAddress(outgoingProxyHost.get, outgoingProxyPort.get))
		} else {
			val uri = new URI(request.getUri)
			val port = if(uri.getPort == -1) 80 else uri.getPort
			bootstrap.connect(new InetSocketAddress(uri.getHost, port))
		}
	}
}
