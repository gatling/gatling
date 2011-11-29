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

public abstract class AbstractRequestHandler extends SimpleChannelHandler {

	protected final String outgoingProxyHost;
	protected final int outgoingProxyPort;

	public AbstractRequestHandler(String outgoingProxyHost, int outgoingProxyPort) {
		this.outgoingProxyHost = outgoingProxyHost;
		this.outgoingProxyPort = outgoingProxyPort;
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent event) throws Exception {

		getEventBus().post(new MessageReceivedEvent(ctx.getChannel()));

		final HttpRequest request = HttpRequest.class.cast(event.getMessage());

		requestReceived(ctx, request);

		ctx.sendUpstream(event);
	}

	protected abstract void requestReceived(ChannelHandlerContext ctx, HttpRequest request) throws Exception;

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
}
