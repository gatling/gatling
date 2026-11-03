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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * An HTTP/2 client channel with the same handlers as {@link DefaultHttpClient} installs, and a fake
 * server side that encodes frames for the client and decodes what the client sends.
 */
final class Http2ClientHarness implements AutoCloseable {

  static final class RecordingListener implements HttpListener {
    int status;
    boolean completed;
    Throwable failure;
    int failureCount;

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

    @Override
    public void onThrowable(Throwable e) {
      failure = e;
      failureCount++;
    }
  }

  private static final Uri URI = Uri.create("https://gatling.io/");

  final EmbeddedChannel client = new EmbeddedChannel();

  /** transactions the client retried, in order */
  final List<HttpTx> retried = new ArrayList<>();

  private final EmbeddedChannel server = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
  private final ChannelHandlerContext serverCtx = server.pipeline().firstContext();
  private final Http2FrameWriter serverWriter = new DefaultHttp2FrameWriter();
  private final Http2FrameReader serverReader = new DefaultHttp2FrameReader();

  Http2ClientHarness(int maxConcurrentStreams) {
    ChannelPool.registerPoolKey(client, new ChannelPoolKey(1, RemoteKey.newKey(URI, null)));
    DefaultHttpClient.installHttp2Handlers(
        client.pipeline(),
        // like DefaultHttpClient#retryHttp2, without sending
        (txs, eventLoop) ->
            txs.forEach(
                tx -> {
                  tx.channelState = HttpTx.ChannelState.RETRY;
                  retried.add(tx);
                }),
        new ChannelPool(),
        client.eventLoop().newPromise());

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

  @Override
  public void close() {
    serverWriter.close();
    serverReader.close();
    server.finishAndReleaseAll();
    client.finishAndReleaseAll();
  }

  RecordingListener sendRequest() {
    return sendRequest(new RequestBuilder("request", HttpMethod.GET, URI, null));
  }

  static RequestBuilder requestBuilder(HttpMethod method) {
    return new RequestBuilder("request", method, URI, null);
  }

  RecordingListener sendRequest(RequestBuilder requestBuilder) {
    RecordingListener listener = new RecordingListener();
    Request request = requestBuilder.build();
    client.write(
        new HttpTx(
            request,
            listener,
            RequestTimeout.requestTimeout(0, listener),
            new ChannelPoolKey(1, RemoteKey.newKey(URI, null)),
            null));
    return listener;
  }

  /** Sends a retried transaction again, on this same connection. */
  void resend(HttpTx tx) {
    client.write(tx);
  }

  private void sendToClient() {
    serverCtx.flush();
    ByteBuf chunk;
    while ((chunk = server.readOutbound()) != null) {
      client.writeInbound(chunk);
    }
  }

  /** What the client sent since the last read. */
  static final class ClientFrames {
    final List<Integer> openedStreamIds = new ArrayList<>();
    // streams for which the client sent END_STREAM, with HEADERS or DATA
    final List<Integer> endedStreamIds = new ArrayList<>();
    final StringBuilder data = new StringBuilder();
    final List<Integer> resetStreamIds = new ArrayList<>();
  }

  /**
   * @return the ids of the streams the client opened since the last read
   */
  List<Integer> streamIdsOpenedByClient() throws Http2Exception {
    return clientFrames().openedStreamIds;
  }

  ClientFrames clientFrames() throws Http2Exception {
    client.flush();
    // frames can be split over several writes, the reader expects whole frames
    ByteBuf written = Unpooled.buffer();
    ByteBuf chunk;
    while ((chunk = client.readOutbound()) != null) {
      written.writeBytes(chunk);
      chunk.release();
    }
    ClientFrames frames = new ClientFrames();
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
              frames.openedStreamIds.add(streamId);
              if (endStream) {
                frames.endedStreamIds.add(streamId);
              }
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
              onHeadersRead(ctx, streamId, headers, padding, endStream);
            }

            @Override
            public int onDataRead(
                ChannelHandlerContext ctx,
                int streamId,
                ByteBuf data,
                int padding,
                boolean endOfStream) {
              frames.data.append(data.toString(StandardCharsets.UTF_8));
              if (endOfStream) {
                frames.endedStreamIds.add(streamId);
              }
              return data.readableBytes() + padding;
            }

            @Override
            public void onRstStreamRead(ChannelHandlerContext ctx, int streamId, long errorCode) {
              frames.resetStreamIds.add(streamId);
            }
          });
    } finally {
      written.release();
    }
    return frames;
  }

  void sendInterimResponse(int streamId, int status) {
    serverWriter.writeHeaders(
        serverCtx,
        streamId,
        new DefaultHttp2Headers().status(String.valueOf(status)),
        0,
        false,
        serverCtx.newPromise());
    sendToClient();
  }

  void respond(int streamId, int status) {
    serverWriter.writeHeaders(
        serverCtx,
        streamId,
        new DefaultHttp2Headers().status(String.valueOf(status)),
        0,
        true,
        serverCtx.newPromise());
    sendToClient();
  }

  void respond(int streamId) {
    respond(streamId, 200);
  }

  void goAway(int lastStreamId, Http2Error error) {
    serverWriter.writeGoAway(
        serverCtx, lastStreamId, error.code(), Unpooled.EMPTY_BUFFER, serverCtx.newPromise());
    sendToClient();
  }

  /**
   * Like the transport does when the peer closes the socket. Closing the channel would go through
   * {@link Http2ConnectionHandler#close}, which waits for active streams to complete.
   */
  void serverDisconnects() {
    client.unsafe().close(client.voidPromise());
    client.runPendingTasks();
  }

  void resetStream(int streamId, Http2Error error) {
    serverWriter.writeRstStream(serverCtx, streamId, error.code(), serverCtx.newPromise());
    sendToClient();
  }
}
