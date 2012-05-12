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
import org.jboss.netty.handler.codec.http.DefaultHttpResponse
import org.jboss.netty.handler.codec.http.HttpMethod
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.jboss.netty.handler.codec.http.HttpVersion

import com.excilys.ebi.gatling.recorder.config.ProxyConfig
import com.excilys.ebi.gatling.recorder.controller.RecorderController
import com.excilys.ebi.gatling.recorder.http.channel.BootstrapFactory.bootstrapFactory

import grizzled.slf4j.Logging

class BrowserHttpsRequestHandler(proxyConfig: ProxyConfig) extends AbstractBrowserRequestHandler(proxyConfig.host, proxyConfig.port) with Logging {

	var targetHostURI: URI = null

	def connectToServerOnBrowserRequestReceived(ctx: ChannelHandlerContext, request: HttpRequest): ChannelFuture = {

		info("Received " + request.getMethod + " on " + request.getUri)

		if (request.getMethod == HttpMethod.CONNECT) {

			targetHostURI = new URI("https://" + request.getUri());

			warn("Trying to connect to " + targetHostURI + ", make sure you've accepted the recorder certificate for this site")
			
			RecorderController.secureConnection(targetHostURI)

			ctx.getChannel.write(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK))

			null

		} else {
			// set full uri so that it's correctly recorder
			val fullUri = new StringBuilder().append(targetHostURI).append(request.getUri).toString
			request.setUri(fullUri)

			val bootstrap = bootstrapFactory.newClientBootstrap(ctx, request, true)

			if(outgoingProxyHost.isDefined && outgoingProxyPort.isDefined){
				bootstrap.connect(new InetSocketAddress(outgoingProxyHost.get, outgoingProxyPort.get))
			} else {
				bootstrap.connect(new InetSocketAddress(targetHostURI.getHost, targetHostURI.getPort))
			}
		}
	}
}
