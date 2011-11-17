package com.excilys.ebi.gatling.recorder.http;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;

import com.excilys.ebi.gatling.recorder.core.HttpRequestHandler;
import com.excilys.ebi.gatling.recorder.http.event.ProxyAction;

public class GatlingHttpProxy extends ProxyAction {
	private int port;
	private ChannelFactory factory;
	private ServerBootstrap bootstrap;

	public GatlingHttpProxy(int port, String outgoingProxyHost, int outgoingProxyPort) {
		this.port = port;

		factory = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
		bootstrap = new ServerBootstrap(factory);

		final HttpRequestHandler rh = new HttpRequestHandler(this);

		/* Adding outgoing proxy configuration */
		if (outgoingProxyPort > 0) {
			rh.setOutgoingProxyHost(outgoingProxyHost);
			rh.setOutgoingProxyPort(outgoingProxyPort);
		}

		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				return Channels.pipeline(new HttpRequestDecoder(), new HttpResponseEncoder(), rh);
			}
		});

		bootstrap.setOption("tcpNoDelay", true);
		bootstrap.setOption("keepAlive", true);
	}

	public void start() {
		getGroup().add(bootstrap.bind(new InetSocketAddress(port)));
	}

	public void shutdown() {
		// Now close all channels
		getGroup().close().awaitUninterruptibly();
		// Now release resources
		factory.releaseExternalResources();
	}
}
