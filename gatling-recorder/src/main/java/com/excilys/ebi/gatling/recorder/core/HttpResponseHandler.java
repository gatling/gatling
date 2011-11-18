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
package com.excilys.ebi.gatling.recorder.core;

import static com.excilys.ebi.gatling.recorder.http.event.RecorderEventBus.getEventBus;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

import com.excilys.ebi.gatling.recorder.http.event.MessageReceivedEvent;
import com.excilys.ebi.gatling.recorder.http.event.ResponseReceivedEvent;

public class HttpResponseHandler extends SimpleChannelHandler {
	private final HttpRequest request;
	private final ChannelHandlerContext requestContext;

	public HttpResponseHandler(ChannelHandlerContext context, HttpRequest request) {
		this.request = request;
		requestContext = context;
	}

	@Override
	public void messageReceived(ChannelHandlerContext context, MessageEvent e) throws Exception {

		getEventBus().post(new MessageReceivedEvent(context.getChannel()));

		HttpResponse response = (HttpResponse) e.getMessage();

		getEventBus().post(new ResponseReceivedEvent(request, response));

		// Send back to client
		requestContext.getChannel().write(response);
		requestContext.sendUpstream(e);
	}
}
