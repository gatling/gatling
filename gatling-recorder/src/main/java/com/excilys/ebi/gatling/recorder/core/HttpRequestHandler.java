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
package com.excilys.ebi.gatling.recorder.core;

import static com.excilys.ebi.gatling.recorder.http.event.RecorderEventBus.getEventBus;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;

import com.excilys.ebi.gatling.recorder.http.event.MessageReceivedEvent;

/**
 * Class forwarding an HTTP request to a remote endpoint
 * 
 * @author nmaupu
 */
public class HttpRequestHandler extends SimpleChannelHandler {

	private static final int MAX_CONTENT_LENGTH = 1024 * 1024;

	private final String outgoingProxyHost;
	private final int outgoingProxyPort;

	public HttpRequestHandler(String outgoingProxyHost, int outgoingProxyPort) {
		this.outgoingProxyHost = outgoingProxyPort > 0 ? outgoingProxyHost : null;
		this.outgoingProxyPort = outgoingProxyPort;
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent event) throws Exception {

		getEventBus().post(new MessageReceivedEvent(ctx.getChannel()));

		final HttpRequest request = HttpRequest.class.cast(event.getMessage());

		ChannelFactory factory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
		ClientBootstrap bootstrap = new ClientBootstrap(factory);

		final HttpResponseHandler responseHandler = new HttpResponseHandler(ctx, request);

		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				return Channels.pipeline(new HttpRequestEncoder(), new HttpResponseDecoder(), new HttpChunkAggregator(MAX_CONTENT_LENGTH), responseHandler);
			}
		});

		ChannelFuture future;
		if (outgoingProxyHost == null) {
			URI uri = new URI(request.getUri());
			int port = uri.getPort() == -1 ? 80 : uri.getPort();
			future = bootstrap.connect(new InetSocketAddress(uri.getHost(), port));
		} else {
			future = bootstrap.connect(new InetSocketAddress(outgoingProxyHost, outgoingProxyPort));
		}

		future.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				future.getChannel().write(request);
			}
		});

		ctx.sendUpstream(event);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		System.err.println("Exception caught = " + e.getCause().getMessage());

		// Properly closing
		ChannelFuture future = ctx.getChannel().getCloseFuture();
		future.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				future.getChannel().close();
			}
		});

		ctx.sendUpstream(e);
	}
}
