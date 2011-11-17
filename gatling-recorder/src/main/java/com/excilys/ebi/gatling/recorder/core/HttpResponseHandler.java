package com.excilys.ebi.gatling.recorder.core;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

import com.excilys.ebi.gatling.recorder.http.event.ProxyAction;
import com.excilys.ebi.gatling.recorder.http.event.ProxyEvent;

public class HttpResponseHandler extends HttpMessageHandler {
	private final ChannelHandlerContext context;
	private final HttpRequest request;

	public HttpResponseHandler(ProxyAction proxyAction, ChannelHandlerContext ctx, HttpRequest request) {
		super(proxyAction);
		this.context = ctx;
		this.request = request;
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {

		proxyAction.getGroup().add(ctx.getChannel());

		HttpResponse response = (HttpResponse) e.getMessage();

		// Notify listeners (and set corresponding request)
		proxyAction.notifyListeners(new ProxyEvent(response, request));

		// Send back to client
		context.getChannel().write(response);

		ctx.sendUpstream(e);
	}
}
