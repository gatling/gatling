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

import static org.junit.jupiter.api.Assertions.*;

import io.gatling.http.client.Http2FlowControlWindows;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class Http2PushTest {

  @Test
  void clientPrefaceShouldDisablePush() throws Exception {
    EmbeddedChannel client =
        new EmbeddedChannel(
            new HttpToHttp2ConnectionHandlerBuilder()
                .initialSettings(
                    DefaultHttpClient.initialHttp2Settings(Http2FlowControlWindows.DEFAULT))
                .connection(new DefaultHttp2Connection(false))
                .frameListener(new Http2FrameAdapter())
                .build());

    ByteBuf written = Unpooled.buffer();
    ByteBuf chunk;
    while ((chunk = client.readOutbound()) != null) {
      written.writeBytes(chunk);
      chunk.release();
    }

    ByteBuf preface = Http2CodecUtil.connectionPrefaceBuf();
    try {
      assertEquals(
          preface,
          written.readSlice(preface.readableBytes()),
          "client must send the preface first");
    } finally {
      preface.release();
    }

    List<Http2Settings> settings = new ArrayList<>();
    EmbeddedChannel dummy = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
    try (Http2FrameReader reader = new DefaultHttp2FrameReader()) {
      reader.readFrame(
          dummy.pipeline().firstContext(),
          written,
          new Http2FrameAdapter() {
            @Override
            public void onSettingsRead(ChannelHandlerContext ctx, Http2Settings s) {
              settings.add(s);
            }
          });
    } finally {
      written.release();
      dummy.finishAndReleaseAll();
      client.finishAndReleaseAll();
    }

    assertEquals(1, settings.size());
    assertEquals(Boolean.FALSE, settings.get(0).pushEnabled());
  }

  @Test
  void shouldIgnoreFramesForUnknownStreams() {
    EmbeddedChannel channel = new EmbeddedChannel(new Http2AppHandler(null, null, null, null));

    DefaultHttpResponse response =
        new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
    response.headers().setInt(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text(), 2);
    channel.writeInbound(response);
    channel.writeInbound(new Http2Content(new DefaultHttpContent(Unpooled.EMPTY_BUFFER), 2, true));

    assertTrue(channel.isActive(), "frames for unknown streams must not close the connection");
    channel.finishAndReleaseAll();
  }
}
