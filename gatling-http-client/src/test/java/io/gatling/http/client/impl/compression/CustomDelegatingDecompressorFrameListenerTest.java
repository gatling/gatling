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

package io.gatling.http.client.impl.compression;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http2.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;

class CustomDelegatingDecompressorFrameListenerTest {

  private static final String BODY = "Hello World, Hello World, Hello World";

  private static byte[] gzip(String s) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
      gzip.write(s.getBytes(UTF_8));
    }
    return baos.toByteArray();
  }

  private static final class CapturingListener extends Http2FrameAdapter {
    private final Map<Integer, ByteArrayOutputStream> dataByStreamId = new HashMap<>();
    private final Map<Integer, CharSequence> contentEncodingByStreamId = new HashMap<>();

    @Override
    public void onHeadersRead(
        ChannelHandlerContext ctx,
        int streamId,
        Http2Headers headers,
        int padding,
        boolean endStream) {
      contentEncodingByStreamId.put(streamId, headers.get(HttpHeaderNames.CONTENT_ENCODING));
    }

    @Override
    public int onDataRead(
        ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream) {
      dataByStreamId
          .computeIfAbsent(streamId, id -> new ByteArrayOutputStream())
          .writeBytes(ByteBufUtil.getBytes(data));
      return data.readableBytes() + padding;
    }
  }

  @Test
  void shouldOnlyDecompressStreamsThatRequireIt() throws Exception {
    byte[] gzipped = gzip(BODY);

    Http2Connection connection = new DefaultHttp2Connection(false);
    connection.local().flowController(new DefaultHttp2LocalFlowController(connection));
    CapturingListener delegate = new CapturingListener();
    CustomDelegatingDecompressorFrameListener listener =
        new CustomDelegatingDecompressorFrameListener(connection, delegate);
    // stream 3 doesn't require decompression, stream 5 does
    listener.setDecompressionRequired(streamId -> streamId != 3);

    EmbeddedChannel channel = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
    ChannelHandlerContext ctx = channel.pipeline().firstContext();
    connection.local().flowController().channelHandlerContext(ctx);

    // interleaved streams on the same connection
    for (int streamId : new int[] {3, 5}) {
      connection.local().createStream(streamId, false);
      Http2Headers headers =
          new DefaultHttp2Headers().status("200").add(HttpHeaderNames.CONTENT_ENCODING, "gzip");
      listener.onHeadersRead(ctx, streamId, headers, 0, false);
    }
    for (int streamId : new int[] {3, 5}) {
      ByteBuf data = Unpooled.wrappedBuffer(gzipped);
      try {
        // what DefaultHttp2ConnectionDecoder does before notifying the listener
        connection
            .local()
            .flowController()
            .receiveFlowControlledFrame(connection.stream(streamId), data, 0, true);
        listener.onDataRead(ctx, streamId, data, 0, true);
      } finally {
        data.release();
      }
    }

    assertArrayEquals(gzipped, delegate.dataByStreamId.get(3).toByteArray());
    assertEquals(BODY, delegate.dataByStreamId.get(5).toString(UTF_8));
    assertEquals("gzip", delegate.contentEncodingByStreamId.get(3).toString());
    assertEquals("gzip", delegate.contentEncodingByStreamId.get(5).toString());

    channel.finishAndReleaseAll();
  }
}
