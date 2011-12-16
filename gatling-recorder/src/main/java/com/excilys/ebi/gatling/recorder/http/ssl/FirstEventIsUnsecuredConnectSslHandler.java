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
package com.excilys.ebi.gatling.recorder.http.ssl;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLEngine;

import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.ssl.SslHandler;

public class FirstEventIsUnsecuredConnectSslHandler extends SslHandler {

	private AtomicBoolean sslEnabled = new AtomicBoolean(false);

	public FirstEventIsUnsecuredConnectSslHandler(SSLEngine sslEngine) {
		super(sslEngine, false);
	}

	@Override
	public void handleUpstream(ChannelHandlerContext context, ChannelEvent evt) throws Exception {
		if (sslEnabled.get()) {
			super.handleUpstream(context, evt);
		} else {
			context.sendUpstream(evt);
		}
	}

	@Override
	public void handleDownstream(ChannelHandlerContext context, ChannelEvent evt) throws Exception {
		if (sslEnabled.get()) {
			super.handleDownstream(context, evt);
		} else {
			// enable SSL once the CONNECT request has been replied to
			sslEnabled.set(true);
			context.sendDownstream(evt);
		}
	}
}
