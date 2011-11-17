package com.excilys.ebi.gatling.recorder.core;

import org.jboss.netty.channel.SimpleChannelHandler;

import com.excilys.ebi.gatling.recorder.http.event.ProxyAction;

class HttpMessageHandler extends SimpleChannelHandler {

	public final ProxyAction proxyAction;

	public HttpMessageHandler(ProxyAction proxyAction) {
		this.proxyAction = proxyAction;
	}
}
