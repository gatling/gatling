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
package com.excilys.ebi.gatling.recorder.http.channel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;

public class BootstrapFactory {

	private static final BootstrapFactory INSTANCE = new BootstrapFactory();

	private static final int MAX_CONTENT_LENGTH = 1024 * 1024;

	private final ExecutorService threadPool = Executors.newCachedThreadPool();

	private final ChannelFactory clientChannelFactory = new NioClientSocketChannelFactory(threadPool, threadPool);

	private final ChannelFactory serverChannelFactory = new NioServerSocketChannelFactory(threadPool, threadPool);

	private BootstrapFactory() {
	}

	public static BootstrapFactory getBootstrapFactory() {
		return INSTANCE;
	}

	public ClientBootstrap newClientBootstrap(final ChannelHandler handler) {
		ClientBootstrap bootstrap = new ClientBootstrap(clientChannelFactory);
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				return Channels.pipeline(new HttpRequestEncoder(), new HttpResponseDecoder(), new HttpChunkAggregator(MAX_CONTENT_LENGTH), handler);
			}
		});
		return bootstrap;
	}

	public ServerBootstrap newServerBootstrap(final ChannelHandler handler) {

		ServerBootstrap bootstrap = new ServerBootstrap(serverChannelFactory);

		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				return Channels.pipeline(new HttpRequestDecoder(), new HttpResponseEncoder(), handler);
			}
		});

		bootstrap.setOption("tcpNoDelay", true);
		bootstrap.setOption("keepAlive", true);

		return bootstrap;
	}
}
