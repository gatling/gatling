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
package com.excilys.ebi.gatling.recorder.http.handler;

import static com.excilys.ebi.gatling.recorder.http.event.RecorderEventBus.getEventBus;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.util.CharsetUtil;

import com.excilys.ebi.gatling.recorder.http.event.MessageReceivedEvent;
import com.excilys.ebi.gatling.recorder.http.event.ResponseReceivedEvent;

public class ServerHttpResponseHandler extends SimpleChannelHandler {
	private final HttpRequest request;
	private final ChannelHandlerContext requestContext;
	private boolean readingChunks;

	public ServerHttpResponseHandler(ChannelHandlerContext context, HttpRequest request) {
		this.request = request;
		requestContext = context;
	}

	@Override
	public void messageReceived(ChannelHandlerContext context, MessageEvent event) throws Exception {
		
	     if (!readingChunks) {
	            HttpResponse response = (HttpResponse) event.getMessage();

	            System.out.println("STATUS: " + response.getStatus());
	            System.out.println("VERSION: " + response.getProtocolVersion());
	            System.out.println();

	            if (!response.getHeaderNames().isEmpty()) {
	                for (String name: response.getHeaderNames()) {
	                    for (String value: response.getHeaders(name)) {
	                        System.out.println("HEADER: " + name + " = " + value);
	                    }
	                }
	                System.out.println();
	            }

	            if (response.isChunked()) {
	                readingChunks = true;
	                System.out.println("CHUNKED CONTENT {");
	            } else {
	                ChannelBuffer content = response.getContent();
	                if (content.readable()) {
	                    System.out.println("CONTENT {");
	                    System.out.println(content.toString(CharsetUtil.UTF_8));
	                    System.out.println("} END OF CONTENT");
	                }
	            }
	        } else {
	            HttpChunk chunk = (HttpChunk) event.getMessage();
	            if (chunk.isLast()) {
	                readingChunks = false;
	                System.out.println("} END OF CHUNKED CONTENT");
	            } else {
	                System.out.print(chunk.getContent().toString(CharsetUtil.UTF_8));
	                System.out.flush();
	            }
	        }

		getEventBus().post(new MessageReceivedEvent(context.getChannel()));

		HttpResponse response = HttpResponse.class.cast(event.getMessage());

		getEventBus().post(new ResponseReceivedEvent(request, response));

		// Send back to client
		requestContext.getChannel().write(response);
		requestContext.sendUpstream(event);
		context.sendUpstream(event);
	}
}
