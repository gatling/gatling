/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.http.client.impl;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http2.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotAggregatingInboundHttp2ToHttpAdapterTest {

  private static final int STREAM_ID = 3;

  private static final class CapturingHandler extends ChannelInboundHandlerAdapter {
    private final List<HttpResponse> responses = new ArrayList<>();
    private final StringBuilder body = new StringBuilder();
    private final List<Boolean> contentLastFlags = new ArrayList<>();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
      if (msg instanceof HttpResponse) {
        responses.add((HttpResponse) msg);
      } else if (msg instanceof Http2Content) {
        Http2Content content = (Http2Content) msg;
        assertEquals(STREAM_ID, content.streamId);
        // content is released by the decoder once we return
        body.append(content.httpContent.content().toString(UTF_8));
        contentLastFlags.add(content.last);
      }
    }
  }

  /** Encodes frames as a server would send them. */
  private static ByteBuf serverFrames(FrameWriting writing) throws Exception {
    EmbeddedChannel server = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
    ChannelHandlerContext ctx = server.pipeline().firstContext();
    try (Http2FrameWriter writer = new DefaultHttp2FrameWriter()) {
      writing.write(writer, ctx);
    }
    ctx.flush();
    ByteBuf frames = Unpooled.buffer();
    ByteBuf chunk;
    while ((chunk = server.readOutbound()) != null) {
      frames.writeBytes(chunk);
      chunk.release();
    }
    server.finishAndReleaseAll();
    return frames;
  }

  @FunctionalInterface
  private interface FrameWriting {
    void write(Http2FrameWriter writer, ChannelHandlerContext ctx) throws Exception;
  }

  @Test
  void shouldNotCrashConnectionOnTrailers() throws Exception {
    Http2Connection connection = new DefaultHttp2Connection(false);
    EmbeddedChannel client = new EmbeddedChannel();
    NotAggregatingInboundHttp2ToHttpAdapter adapter =
        new NotAggregatingInboundHttp2ToHttpAdapter(connection, client.eventLoop().newPromise());
    HttpToHttp2ConnectionHandler http2Handler =
        new HttpToHttp2ConnectionHandlerBuilder()
            .connection(connection)
            .frameListener(adapter)
            .build();
    CapturingHandler capturing = new CapturingHandler();
    client.pipeline().addLast(http2Handler, capturing);

    // as if the request had been sent
    connection.local().createStream(STREAM_ID, true);

    ByteBuf frames =
        serverFrames(
            (writer, ctx) -> {
              writer.writeSettings(ctx, new Http2Settings(), ctx.newPromise());
              writer.writeHeaders(
                  ctx,
                  STREAM_ID,
                  new DefaultHttp2Headers().status("200"),
                  0,
                  false,
                  ctx.newPromise());
              writer.writeData(
                  ctx,
                  STREAM_ID,
                  Unpooled.copiedBuffer("Hello", UTF_8),
                  0,
                  false,
                  ctx.newPromise());
              writer.writeHeaders(
                  ctx,
                  STREAM_ID,
                  new DefaultHttp2Headers().add("grpc-status", "0"),
                  0,
                  true,
                  ctx.newPromise());
            });

    client.writeInbound(frames);

    assertTrue(client.isActive(), "connection must not be closed because of trailers");
    assertEquals(1, capturing.responses.size());
    assertEquals(200, capturing.responses.get(0).status().code());
    assertEquals("Hello", capturing.body.toString());
    assertEquals(List.of(false, true), capturing.contentLastFlags);

    client.finishAndReleaseAll();
  }
}
