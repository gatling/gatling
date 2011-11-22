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
package com.excilys.ebi.gatling.recorder.http;

import static com.excilys.ebi.gatling.recorder.http.event.RecorderEventBus.getEventBus;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;

import com.excilys.ebi.gatling.recorder.core.HttpRequestHandler;
import com.excilys.ebi.gatling.recorder.http.event.MessageReceivedEvent;
import com.google.common.eventbus.Subscribe;

public class GatlingHttpProxy {
	private int port;
	private ChannelFactory factory;
	private ServerBootstrap bootstrap;
	private final ChannelGroup group = new DefaultChannelGroup("Gatling_Recorder");

	public GatlingHttpProxy(int port, String outgoingProxyHost, int outgoingProxyPort) {
		this.port = port;

		factory = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
		bootstrap = new ServerBootstrap(factory);

		final HttpRequestHandler requestHandler = new HttpRequestHandler(outgoingProxyHost, outgoingProxyPort);

		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				return Channels.pipeline(new HttpRequestDecoder(), new HttpResponseEncoder(), requestHandler);
			}
		});

		bootstrap.setOption("tcpNoDelay", true);
		bootstrap.setOption("keepAlive", true);
	}

	public void start() {
		getEventBus().register(this);
		group.add(bootstrap.bind(new InetSocketAddress(port)));
	}

	public void shutdown() {
		getEventBus().unregister(this);
		group.close().awaitUninterruptibly();
		factory.releaseExternalResources();
	}

	@Subscribe
	public void onMessageReceived(MessageReceivedEvent event) {
		group.add(event.getChannel());
	}
}
