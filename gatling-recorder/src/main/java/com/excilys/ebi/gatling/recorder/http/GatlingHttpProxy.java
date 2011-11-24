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

import static com.excilys.ebi.gatling.recorder.http.channel.BootstrapFactory.getBootstrapFactory;
import static com.excilys.ebi.gatling.recorder.http.event.RecorderEventBus.getEventBus;

import java.net.InetSocketAddress;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;

import com.excilys.ebi.gatling.recorder.http.event.MessageReceivedEvent;
import com.excilys.ebi.gatling.recorder.http.handler.HttpRequestHandler;
import com.google.common.eventbus.Subscribe;

public class GatlingHttpProxy {
	private final int port;
	private final ServerBootstrap bootstrap;
	private final ChannelGroup group = new DefaultChannelGroup("Gatling_Recorder");

	public GatlingHttpProxy(int port, String outgoingProxyHost, int outgoingProxyPort) {
		this.port = port;
		this.bootstrap = getBootstrapFactory().newServerBootstrap(new HttpRequestHandler(outgoingProxyHost, outgoingProxyPort));
	}

	public void start() {
		getEventBus().register(this);
		group.add(bootstrap.bind(new InetSocketAddress(port)));
	}

	public void shutdown() {
		getEventBus().unregister(this);
		group.close().awaitUninterruptibly();
	}

	@Subscribe
	public void onMessageReceived(MessageReceivedEvent event) {
		group.add(event.getChannel());
	}
}
