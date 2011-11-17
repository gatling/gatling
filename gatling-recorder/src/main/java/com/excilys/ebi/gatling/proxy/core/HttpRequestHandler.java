package com.excilys.ebi.gatling.proxy.core;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.*;

import com.excilys.ebi.gatling.proxy.event.ProxyAction;
import com.excilys.ebi.gatling.proxy.event.ProxyEvent;

/**
 * Class forwarding an HTTP request to a remote endpoint
 * 
 * @author nmaupu
 */
public class HttpRequestHandler extends HttpMessageHandler {
	private String outgoingProxyHost;
	private int outgoingProxyPort;

	public HttpRequestHandler(ProxyAction proxyAction) {
		super(proxyAction);
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {

		proxyAction.getGroup().add(ctx.getChannel());

		final HttpRequest request = (HttpRequest) e.getMessage();

		// Notify all listeners
		super.proxyAction.notifyListeners(new ProxyEvent(request));

		URI uri = new URI(request.getUri());
		final String host = uri.getHost();
		int port = uri.getPort() == -1 ? 80 : uri.getPort();

		// Discard if host is null
		if (host == null)
			return;

		ChannelFactory factory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
		ClientBootstrap bootstrap = new ClientBootstrap(factory);

		// Create responseHandler and set listeners
		final HttpResponseHandler rh = new HttpResponseHandler(proxyAction, ctx, request);

		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				return Channels.pipeline(new HttpRequestEncoder(), new HttpResponseDecoder(), new HttpChunkAggregator(1048576), rh);
			}
		});

		ChannelFuture future;
		if (outgoingProxyHost == null)
			future = bootstrap.connect(new InetSocketAddress(host, port));
		else
			future = bootstrap.connect(new InetSocketAddress(outgoingProxyHost, outgoingProxyPort));

		future.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				future.getChannel().write(request);
			}
		});

		ctx.sendUpstream(e);
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

	public void setOutgoingProxyHost(String outgoingProxyHost) {
		this.outgoingProxyHost = outgoingProxyHost;
	}

	public void setOutgoingProxyPort(int outgoingProxyPort) {
		this.outgoingProxyPort = outgoingProxyPort;
	}
}
