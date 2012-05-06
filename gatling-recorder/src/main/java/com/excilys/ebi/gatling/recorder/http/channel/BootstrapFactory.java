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
package com.excilys.ebi.gatling.recorder.http.channel;

import static org.jboss.netty.channel.Channels.pipeline;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpClientCodec;
import org.jboss.netty.handler.codec.http.HttpContentCompressor;
import org.jboss.netty.handler.codec.http.HttpContentDecompressor;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.logging.InternalLogLevel;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Slf4JLoggerFactory;

import com.excilys.ebi.gatling.recorder.configuration.ProxyConfig;
import com.excilys.ebi.gatling.recorder.http.handler.BrowserHttpRequestHandler;
import com.excilys.ebi.gatling.recorder.http.handler.BrowserHttpsRequestHandler;
import com.excilys.ebi.gatling.recorder.http.handler.ServerHttpResponseHandler;
import com.excilys.ebi.gatling.recorder.http.ssl.FirstEventIsUnsecuredConnectSslHandler;
import com.excilys.ebi.gatling.recorder.http.ssl.SSLEngineFactory;

public class BootstrapFactory {

	static {
		InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
	}

	private static final BootstrapFactory INSTANCE = new BootstrapFactory();

	private static final int CHUNK_MAX_SIZE = 100 * 1024 * 1024; // 1Mo

	private final ExecutorService threadPool = Executors.newCachedThreadPool();

	private final ChannelFactory clientChannelFactory = new NioClientSocketChannelFactory(threadPool, threadPool);

	private final ChannelFactory serverChannelFactory = new NioServerSocketChannelFactory(threadPool, threadPool);

	private BootstrapFactory() {
	}

	public static BootstrapFactory getBootstrapFactory() {
		return INSTANCE;
	}

	public ClientBootstrap newClientBootstrap(final ChannelHandlerContext browserCtx, final HttpRequest browserRequest, final boolean ssl) {
		ClientBootstrap bootstrap = new ClientBootstrap(clientChannelFactory);
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = pipeline();

				if (ssl)
					pipeline.addLast("ssl", new SslHandler(SSLEngineFactory.newClientSSLEngine()));
				pipeline.addLast("codec", new HttpClientCodec());
				pipeline.addLast("inflater", new HttpContentDecompressor());
				pipeline.addLast("aggregator", new HttpChunkAggregator(CHUNK_MAX_SIZE));
				pipeline.addLast("log", new LoggingHandler(InternalLogLevel.DEBUG));
				pipeline.addLast("gatling", new ServerHttpResponseHandler(browserCtx, browserRequest));

				return pipeline;
			}
		});

		bootstrap.setOption("child.tcpNoDelay", true);
		bootstrap.setOption("child.keepAlive", true);

		return bootstrap;
	}

	public ServerBootstrap newServerBootstrap(final ProxyConfig proxyConfig, final boolean ssl) {

		ServerBootstrap bootstrap = new ServerBootstrap(serverChannelFactory);

		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = pipeline();
				if (ssl)
					pipeline.addLast("ssl", new FirstEventIsUnsecuredConnectSslHandler(SSLEngineFactory.newServerSSLEngine()));
				pipeline.addLast("decoder", new HttpRequestDecoder());
				pipeline.addLast("aggregator", new HttpChunkAggregator(CHUNK_MAX_SIZE));
				pipeline.addLast("encoder", new HttpResponseEncoder());
				pipeline.addLast("deflater", new HttpContentCompressor());
				pipeline.addLast("log", new LoggingHandler(InternalLogLevel.DEBUG));
				if (ssl) {
					pipeline.addLast("gatling", new BrowserHttpsRequestHandler(proxyConfig));
				} else {
					pipeline.addLast("gatling", new BrowserHttpRequestHandler(proxyConfig));
				}

				return pipeline;
			}
		});

		bootstrap.setOption("child.tcpNoDelay", true);
		bootstrap.setOption("child.keepAlive", true);

		return bootstrap;
	}
}
