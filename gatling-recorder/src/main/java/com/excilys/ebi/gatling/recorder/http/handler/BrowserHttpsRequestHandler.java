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

import static com.excilys.ebi.gatling.recorder.http.channel.BootstrapFactory.getBootstrapFactory;
import static com.excilys.ebi.gatling.recorder.http.event.RecorderEventBus.getEventBus;

import java.net.InetSocketAddress;
import java.net.URI;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.excilys.ebi.gatling.recorder.configuration.ProxyConfig;
import com.excilys.ebi.gatling.recorder.http.event.SecuredHostConnectionEvent;

public class BrowserHttpsRequestHandler extends AbstractBrowserRequestHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(BrowserHttpsRequestHandler.class);

	private URI targetHostURI;

	public BrowserHttpsRequestHandler(ProxyConfig proxyConfig) {
		super(proxyConfig.getHost(), proxyConfig.getPort());
	}

	@Override
	protected ChannelFuture connectToServerOnBrowserRequestReceived(ChannelHandlerContext ctx, final HttpRequest request) throws Exception {

		LOGGER.info("Received {} on {}", request.getMethod(), request.getUri());

		if (request.getMethod().equals(HttpMethod.CONNECT)) {

			targetHostURI = new URI("https://" + request.getUri());

			LOGGER.warn("Trying to connect to {}, make sure you've accepted the recorder certificate for this site", targetHostURI);
			getEventBus().post(new SecuredHostConnectionEvent(targetHostURI));

			ctx.getChannel().write(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK));

			return null;

		} else {
			// set full uri so that it's correctly recorded
			String fullUri = new StringBuilder().append(targetHostURI).append(request.getUri()).toString();
			request.setUri(fullUri);

			ClientBootstrap bootstrap = getBootstrapFactory().newClientBootstrap(ctx, request, true);

			ChannelFuture future;
			if (outgoingProxyHost == null) {
				future = bootstrap.connect(new InetSocketAddress(targetHostURI.getHost(), targetHostURI.getPort()));

			} else {
				future = bootstrap.connect(new InetSocketAddress(outgoingProxyHost, outgoingProxyPort));
			}

			return future;
		}
	}
}
