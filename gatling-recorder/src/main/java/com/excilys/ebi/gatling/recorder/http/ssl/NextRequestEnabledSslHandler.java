package com.excilys.ebi.gatling.recorder.http.ssl;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLEngine;

import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.ssl.SslHandler;

public class NextRequestEnabledSslHandler extends SslHandler {

	private AtomicBoolean sslEnabled = new AtomicBoolean(false);

	public NextRequestEnabledSslHandler(SSLEngine sslEngine) {
		super(sslEngine, false);
	}

	@Override
	public void handleUpstream(ChannelHandlerContext context, ChannelEvent evt) throws Exception {
		sslEnabled.set(true);
		super.handleUpstream(context, evt);
	}

	@Override
	public void handleDownstream(ChannelHandlerContext context, ChannelEvent evt) throws Exception {

		if (sslEnabled.get()) {
			super.handleDownstream(context, evt);
		} else {
			context.sendDownstream(evt);
		}
	}
}
