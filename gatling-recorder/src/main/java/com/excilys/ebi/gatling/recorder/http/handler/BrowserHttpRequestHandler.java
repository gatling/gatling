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

import java.net.InetSocketAddress;
import java.net.URI;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpRequest;

import com.excilys.ebi.gatling.recorder.configuration.ProxyConfig;

public class BrowserHttpRequestHandler extends AbstractBrowserRequestHandler {

	public BrowserHttpRequestHandler(ProxyConfig proxyConfig) {
		super(proxyConfig.getHost(), proxyConfig.getPort());
	}

	@Override
	protected ChannelFuture connectToServerOnBrowserRequestReceived(ChannelHandlerContext ctx, final HttpRequest request) throws Exception {

		ClientBootstrap bootstrap = getBootstrapFactory().newClientBootstrap(ctx, request, false);

		ChannelFuture future;
		if (outgoingProxyHost == null) {
			URI uri = new URI(request.getUri());
			int port = uri.getPort() == -1 ? 80 : uri.getPort();
			future = bootstrap.connect(new InetSocketAddress(uri.getHost(), port));

		} else {
			future = bootstrap.connect(new InetSocketAddress(outgoingProxyHost, outgoingProxyPort));
		}

		return future;
	}
}
