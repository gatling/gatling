/**
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.HashMap;
import java.util.Map;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

import com.excilys.ebi.gatling.recorder.configuration.ProxyConfig;
import com.excilys.ebi.gatling.recorder.http.ssl.NextRequestEnabledSslHandler;
import com.excilys.ebi.gatling.recorder.http.ssl.SSLEngineFactory;

public class BrowserHttpsRequestHandler extends AbstractBrowserRequestHandler {

	private Map<String, Integer> securedHosts = new HashMap<String, Integer>();

	public BrowserHttpsRequestHandler(ProxyConfig proxyConfig) {
		super(proxyConfig.getHost(), proxyConfig.getPort());
	}

	@Override
	protected ChannelFuture connectToServerOnBrowserRequestReceived(ChannelHandlerContext ctx, final HttpRequest request) throws Exception {

		if (request.getMethod().equals(HttpMethod.CONNECT)) {
			URI uri = new URI("https://" + request.getUri());
			securedHosts.put(uri.getHost(), uri.getPort());

			ctx.getPipeline().addFirst("ssl", new NextRequestEnabledSslHandler(SSLEngineFactory.newServerSSLEngine()));
			ctx.getChannel().write(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK));

			return null;

		} else {
			String host = request.getHeader(HttpHeaders.Names.HOST);
			int port = securedHosts.get(host);

			// set full uri so that it's correctly recorder
			String fullUri = new StringBuilder().append("https://").append(host).append(":").append(port).append(request.getUri()).toString();
			request.setUri(fullUri);

			ClientBootstrap bootstrap = getBootstrapFactory().newClientBootstrap(ctx, request, true);

			ChannelFuture future;
			if (outgoingProxyHost == null) {
				future = bootstrap.connect(new InetSocketAddress(host, port));

			} else {
				future = bootstrap.connect(new InetSocketAddress(outgoingProxyHost, outgoingProxyPort));
			}

			return future;
		}
	}
}
