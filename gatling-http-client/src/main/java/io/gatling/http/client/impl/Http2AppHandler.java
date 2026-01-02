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
import io.gatling.http.client.impl.request.WritableRequest;
import io.gatling.http.client.impl.request.WritableRequestBuilder;
import io.gatling.http.client.pool.ChannelPool;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http2.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Http2AppHandler extends ChannelDuplexHandler {

  public static final class StreamTimeout {
    private final int streamId;

    public StreamTimeout(int streamId) {
      this.streamId = streamId;
    }
  }

  public static final class GoAwayFrame {
    private final int lastStreamId;
    private final long errorCode;

    public GoAwayFrame(int lastStreamId, long errorCode) {
      this.lastStreamId = lastStreamId;
      this.errorCode = errorCode;
    }

    @Override
    public String toString() {
      return "GoAwayFrame{lastStreamId=" + lastStreamId + ", errorCode=" + errorCode + '}';
    }
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(Http2AppHandler.class);
  private static final IOException REMOTELY_CLOSED_EXCEPTION =
      new IOException("Channel was closed before handshake completed");

  private final DefaultHttpClient client;
  private final Http2ConnectionHandler http2ConnectionHandler;
  private final ChannelPool channelPool;

  // mutable state
  private boolean writeReached = false;
  private int nextStreamId = 1;
  private final Map<Integer, HttpTx> txByStreamId = new HashMap<>();

  Http2AppHandler(
      DefaultHttpClient client,
      Http2ConnectionHandler http2ConnectionHandler,
      ChannelPool channelPool) {
    this.client = client;
    this.http2ConnectionHandler = http2ConnectionHandler;
    this.channelPool = channelPool;
  }

  @Override
  public boolean isSharable() {
    return false;
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
    writeReached = true;
    HttpTx tx = (HttpTx) msg;

    if (tx.requestTimeout.isDone()) {
      channelPool.offer(ctx.channel());
      return;
    }
    nextStreamId += 2; // that's how Netty works, 1 is reserved for the connection itself
    int thisStreamId = nextStreamId;
    txByStreamId.put(thisStreamId, tx);

    try {
      WritableRequest request =
          WritableRequestBuilder.buildRequest(tx.request, ctx.alloc(), true, tx.listener);
      LOGGER.debug("Write request {}", request);
      tx.listener.onWrite(ctx.channel());

      request
          .getRequest()
          .headers()
          .setInt(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text(), thisStreamId);

      ChannelFuture whenWrite;
      if (HttpUtil.is100ContinueExpected(request.getRequest())) {
        LOGGER.debug("Delaying body write");
        tx.pendingRequestExpectingContinue = request;
        whenWrite = request.writeWithoutContent(ctx);
      } else {
        whenWrite = request.write(ctx);
      }

      whenWrite.addListener(
          f -> {
            if (f.isSuccess()) {
              if (tx.requestTimeout.isDone()) {
                resetStream(ctx, thisStreamId);
              } else {
                tx.requestTimeout.setStreamId(thisStreamId);
              }

            } else {
              tx.requestTimeout.cancel();
              tx.listener.onThrowable(f.cause());
            }
          });
    } catch (Exception e) {
      crash(ctx, e, tx.listener, true);
    }
  }

  private void channelReadHttpResponse(ChannelHandlerContext ctx, HttpResponse response) {
    Integer streamId =
        response.headers().getInt(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text());
    HttpTx tx = txByStreamId.get(streamId);

    if (tx.requestTimeout.isDone()) {
      tx.releasePendingRequestExpectingContinue();
      resetStream(ctx, streamId);
      return;
    }

    HttpResponseStatus status = response.status();

    if (tx.pendingRequestExpectingContinue != null) {
      if (status.equals(HttpResponseStatus.CONTINUE)) {
        LOGGER.debug("Received 100-Continue");
        return;

      } else {
        // TODO implement 417 support
        LOGGER.debug(
            "Request was sent with Expect:100-Continue but received response with status {}, dropping",
            status);
        tx.releasePendingRequestExpectingContinue();
      }
    }

    tx.listener.onHttpResponse(status, response.headers());
  }

  private void channelReadHttp2Content(ChannelHandlerContext ctx, Http2Content content) {
    int streamId = content.streamId;
    HttpTx tx = txByStreamId.get(streamId);

    if (tx.requestTimeout.isDone()) {
      resetStream(ctx, streamId);
      return;
    }

    boolean last = content.last;

    if (tx.pendingRequestExpectingContinue != null) {
      if (last) {
        LOGGER.debug("Received 100-Continue' LastHttpContent, sending body");
        tx.pendingRequestExpectingContinue.writeContent(ctx);
        tx.pendingRequestExpectingContinue = null;
      }
      return;
    }

    tx.listener.onHttpResponseBodyChunk(content.httpContent.content(), last);
    if (last) {
      tx.requestTimeout.cancel();
      closeStream(ctx, streamId);
    }
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (msg instanceof DefaultHttpResponse) {
      // fast path
      channelReadHttpResponse(ctx, (DefaultHttpResponse) msg);

    } else if (msg instanceof Http2Content) {
      // fast path
      channelReadHttp2Content(ctx, (Http2Content) msg);

    } else if (msg instanceof HttpResponse) {
      // slow path
      channelReadHttpResponse(ctx, (HttpResponse) msg);
    }
  }

  private void crash(
      ChannelHandlerContext ctx,
      Throwable cause,
      HttpListener nonActiveStreamListener,
      boolean close) {
    try {
      if (nonActiveStreamListener != null) {
        nonActiveStreamListener.onThrowable(cause);
      }
      txByStreamId.forEach(
          (id, tx) -> {
            tx.releasePendingRequestExpectingContinue();
            tx.listener.onThrowable(cause);
          });
    } finally {
      if (close) {
        // FIXME shouldn't we close the connection?
        ctx.close();
      }
    }

    if (cause instanceof Error) {
      LOGGER.error("Fatal error", cause);
      System.exit(1);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    crash(ctx, cause, null, true);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
    if (!writeReached) {
      crash(ctx, REMOTELY_CLOSED_EXCEPTION, null, false);
    }
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
    if (evt instanceof GoAwayFrame) {
      GoAwayFrame goAway = (GoAwayFrame) evt;

      LOGGER.debug("Received GOAWAY frame: {}", goAway);
      ChannelPool.markAsGoAway(ctx.channel());

      List<HttpTx> retryTxs = new ArrayList<>(3);

      List<Map.Entry<Integer, HttpTx>> droppedStreams =
          txByStreamId.entrySet().stream()
              .filter(entry -> entry.getKey() > goAway.lastStreamId)
              .collect(Collectors.toList());

      droppedStreams.forEach(
          entry -> {
            txByStreamId.remove(entry.getKey());
            HttpTx tx = entry.getValue();
            if (goAway.errorCode == Http2Error.NO_ERROR.code() && client.canRetry(tx)) {
              retryTxs.add(tx);
            } else {
              tx.listener.onThrowable(REMOTELY_CLOSED_EXCEPTION);
            }
          });

      if (!retryTxs.isEmpty()) {
        client.retryHttp2(retryTxs, ctx.channel().eventLoop());
      }

    } else if (evt instanceof StreamTimeout) {
      resetStream(ctx, ((StreamTimeout) evt).streamId);
    }
  }

  private void closeStream(ChannelHandlerContext ctx, int streamId) {
    txByStreamId.remove(streamId);
    http2ConnectionHandler.connection().stream(streamId).close();
    channelPool.offer(ctx.channel());
  }

  private void resetStream(ChannelHandlerContext ctx, int streamId) {
    txByStreamId.remove(streamId);
    http2ConnectionHandler
        .resetStream(ctx, streamId, Http2Error.CANCEL.code(), ctx.newPromise())
        .addListener((ChannelFutureListener) future -> channelPool.offer(ctx.channel()));
  }
}
