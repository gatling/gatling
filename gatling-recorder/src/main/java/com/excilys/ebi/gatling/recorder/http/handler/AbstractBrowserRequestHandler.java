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

import static com.excilys.ebi.gatling.recorder.http.event.RecorderEventBus.getEventBus;

import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.http.HttpRequest;

import com.excilys.ebi.gatling.recorder.http.event.MessageReceivedEvent;
import com.excilys.ebi.gatling.recorder.http.event.RequestReceivedEvent;

public abstract class AbstractBrowserRequestHandler extends SimpleChannelHandler {

	protected final String outgoingProxyHost;
	protected final int outgoingProxyPort;

	public AbstractBrowserRequestHandler(String outgoingProxyHost, int outgoingProxyPort) {
		this.outgoingProxyHost = outgoingProxyHost;
		this.outgoingProxyPort = outgoingProxyPort;
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent event) throws Exception {

		getEventBus().post(new MessageReceivedEvent(ctx.getChannel()));

		HttpRequest request = HttpRequest.class.cast(event.getMessage());
		
		// remove Proxy-Connection header if it's not significant
		if (outgoingProxyHost == null)
			request.removeHeader("Proxy-Connection");

		ChannelFuture future = connectToServerOnBrowserRequestReceived(ctx, request);
		
		getEventBus().post(new RequestReceivedEvent(request));
		
		sendRequestToServerAfterConnection(future, request);
	}

	protected abstract ChannelFuture connectToServerOnBrowserRequestReceived(ChannelHandlerContext ctx, HttpRequest request) throws Exception;

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		System.err.println("Exception caught = ");
		e.getCause().printStackTrace();

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

	private void sendRequestToServerAfterConnection(ChannelFuture future, final HttpRequest request) {
		if (future != null)
			future.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					future.getChannel().write(request);
				}
			});
	}
}
