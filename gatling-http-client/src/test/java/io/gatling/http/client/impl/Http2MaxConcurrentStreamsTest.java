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

import io.gatling.http.client.HttpListener;
import io.gatling.http.client.Request;
import io.gatling.http.client.RequestBuilder;
import io.gatling.http.client.pool.ChannelPool;
import io.gatling.http.client.pool.ChannelPoolKey;
import io.gatling.http.client.pool.RemoteKey;
import io.gatling.http.client.uri.Uri;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class Http2MaxConcurrentStreamsTest {

  private static final class RecordingListener implements HttpListener {
    private int status;
    private boolean completed;
    private Throwable failure;

    @Override
    public void onFinalClientRequest(Request request) {}

    @Override
    public void onHttpResponse(HttpResponseStatus status, HttpHeaders headers) {
      this.status = status.code();
    }

    @Override
    public void onHttpResponseBodyChunk(ByteBuf chunk, boolean last) {
      completed |= last;
    }

    private int failureCount;

    @Override
    public void onThrowable(Throwable e) {
      failure = e;
      failureCount++;
    }
  }

  /** Plays the server side: encodes frames for the client and decodes what the client sends. */
  private final EmbeddedChannel server = new EmbeddedChannel(new ChannelInboundHandlerAdapter());

  private final ChannelHandlerContext serverCtx = server.pipeline().firstContext();
  private final Http2FrameWriter serverWriter = new DefaultHttp2FrameWriter();
  private final Http2FrameReader serverReader = new DefaultHttp2FrameReader();
  private final EmbeddedChannel client = new EmbeddedChannel();

  @AfterEach
  void tearDown() {
    serverWriter.close();
    serverReader.close();
    server.finishAndReleaseAll();
    client.finishAndReleaseAll();
  }

  private void sendToClient() {
    serverCtx.flush();
    ByteBuf chunk;
    while ((chunk = server.readOutbound()) != null) {
      client.writeInbound(chunk);
    }
  }

  private List<Integer> requestStreamIdsSentByClient() throws Http2Exception {
    client.flush();
    // frames can be split over several writes, the reader expects whole frames
    ByteBuf written = Unpooled.buffer();
    ByteBuf chunk;
    while ((chunk = client.readOutbound()) != null) {
      written.writeBytes(chunk);
      chunk.release();
    }
    List<Integer> streamIds = new ArrayList<>();
    try {
      serverReader.readFrame(
          serverCtx,
          written,
          new Http2FrameAdapter() {
            @Override
            public void onHeadersRead(
                ChannelHandlerContext ctx,
                int streamId,
                Http2Headers headers,
                int padding,
                boolean endStream) {
              streamIds.add(streamId);
            }

            // HttpToHttp2ConnectionHandler sends HEADERS with the PRIORITY flag
            @Override
            public void onHeadersRead(
                ChannelHandlerContext ctx,
                int streamId,
                Http2Headers headers,
                int streamDependency,
                short weight,
                boolean exclusive,
                int padding,
                boolean endStream) {
              streamIds.add(streamId);
            }
          });
    } finally {
      written.release();
    }
    return streamIds;
  }

  private void respond(int streamId) {
    serverWriter.writeHeaders(
        serverCtx,
        streamId,
        new DefaultHttp2Headers().status("200"),
        0,
        true,
        serverCtx.newPromise());
    sendToClient();
  }

  private static HttpTx tx(Uri uri, HttpListener listener) {
    Request request = new RequestBuilder("request", HttpMethod.GET, uri, null).build();
    return new HttpTx(
        request,
        listener,
        RequestTimeout.requestTimeout(0, listener),
        new ChannelPoolKey(1, RemoteKey.newKey(uri, null)),
        null);
  }

  @Test
  void shouldQueueStreamsExceedingMaxConcurrentStreams() throws Exception {
    Uri uri = Uri.create("https://gatling.io/");
    connectWithMaxConcurrentStreams(uri, 1);

    RecordingListener listener1 = new RecordingListener();
    RecordingListener listener2 = new RecordingListener();
    client.write(tx(uri, listener1));
    client.write(tx(uri, listener2));

    List<Integer> firstStreamIds = requestStreamIdsSentByClient();
    assertNull(listener1.failure, "the first request must be sent");
    assertEquals(1, firstStreamIds.size(), "only 1 stream must be opened");
    assertNull(listener2.failure, "the second request must be queued, not failed");

    respond(firstStreamIds.get(0));
    assertEquals(200, listener1.status);
    assertTrue(listener1.completed);

    List<Integer> secondStreamIds = requestStreamIdsSentByClient();
    assertEquals(
        1, secondStreamIds.size(), "the queued stream must be opened once the first one is closed");

    respond(secondStreamIds.get(0));
    assertEquals(200, listener2.status);
    assertTrue(listener2.completed);
    assertNull(listener1.failure);
    assertNull(listener2.failure);
  }

  @Test
  void shouldNotifyQueuedStreamOnlyOnceOnGoAway() throws Exception {
    Uri uri = Uri.create("https://gatling.io/");
    connectWithMaxConcurrentStreams(uri, 1);

    RecordingListener listener1 = new RecordingListener();
    RecordingListener listener2 = new RecordingListener();
    client.write(tx(uri, listener1));
    client.write(tx(uri, listener2));
    List<Integer> streamIds = requestStreamIdsSentByClient();
    assertEquals(1, streamIds.size());

    // not NO_ERROR, so the dropped queued stream is failed instead of retried
    serverWriter.writeGoAway(
        serverCtx,
        streamIds.get(0),
        Http2Error.INTERNAL_ERROR.code(),
        Unpooled.EMPTY_BUFFER,
        serverCtx.newPromise());
    sendToClient();

    assertEquals(0, listener1.failureCount, "the stream processed by the server must go on");
    assertEquals(1, listener2.failureCount, "the queued stream must be failed exactly once");
  }

  private void connectWithMaxConcurrentStreams(Uri uri, int maxConcurrentStreams) {
    ChannelPool.registerPoolKey(client, new ChannelPoolKey(1, RemoteKey.newKey(uri, null)));
    DefaultHttpClient.installHttp2Handlers(
        client.pipeline(), null, new ChannelPool(), client.eventLoop().newPromise());

    // skip the client preface
    client.flush();
    ByteBuf chunk;
    while ((chunk = client.readOutbound()) != null) {
      chunk.release();
    }

    serverWriter.writeSettings(
        serverCtx,
        new Http2Settings().maxConcurrentStreams(maxConcurrentStreams),
        serverCtx.newPromise());
    sendToClient();
  }
}
