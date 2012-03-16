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
package com.excilys.ebi.gatling.recorder.http.handler;

import static com.excilys.ebi.gatling.recorder.http.event.RecorderEventBus.getEventBus;
import static org.apache.commons.lang.StringUtils.EMPTY;

import java.io.UnsupportedEncodingException;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.excilys.ebi.gatling.recorder.configuration.Configuration;
import com.excilys.ebi.gatling.recorder.http.event.MessageReceivedEvent;
import com.excilys.ebi.gatling.recorder.http.event.ResponseReceivedEvent;

public class ServerHttpResponseHandler extends SimpleChannelHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServerHttpResponseHandler.class);

	private final HttpRequest request;
	private final ChannelHandlerContext requestContext;

	public ServerHttpResponseHandler(ChannelHandlerContext context, HttpRequest request) {
		this.request = request;
		requestContext = context;
	}

	@Override
	public void messageReceived(ChannelHandlerContext context, MessageEvent event) throws Exception {

		getEventBus().post(new MessageReceivedEvent(context.getChannel()));

		HttpResponse response = HttpResponse.class.cast(event.getMessage());

		String requestContent = decodeContent(request.getContent());
		String responseContent = decodeContent(response.getContent());

		getEventBus().post(new ResponseReceivedEvent(request, response, requestContent, responseContent));

		// Send back to client
		requestContext.getChannel().write(response);
	}

	private String decodeContent(ChannelBuffer channelBuffer) {
		int read = channelBuffer.readableBytes();
		int index = channelBuffer.readerIndex();

		byte[] rb = new byte[read];
		channelBuffer.readBytes(rb);
		channelBuffer.readerIndex(index);

		try {
			return new String(rb, Configuration.getInstance().getEncoding());
		} catch (UnsupportedEncodingException e) {
			LOGGER.error("Couldn't decode content", e);
			return EMPTY;
		}
	}
}
