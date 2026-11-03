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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
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

  public static final class RstStreamFrame {
    private final int streamId;
    private final long errorCode;

    public RstStreamFrame(int streamId, long errorCode) {
      this.streamId = streamId;
      this.errorCode = errorCode;
    }

    @Override
    public String toString() {
      return "RstStreamFrame{streamId=" + streamId + ", errorCode=" + errorCode + '}';
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
  private static final IOException CONNECTION_CLOSED_EXCEPTION =
      new IOException("Connection was closed while the stream was in flight");

  private final BiConsumer<List<HttpTx>, EventLoop> retry;
  private final Http2ConnectionHandler http2ConnectionHandler;
  private final ChannelPool channelPool;

  // mutable state
  private boolean writeReached = false;
  // like Netty, client streams start at 3, as 1 is reserved for the HTTP/1.1 upgrade
  private int lastStreamId = 1;
  private final Map<Integer, HttpTx> txByStreamId = new HashMap<>();

  Http2AppHandler(
      BiConsumer<List<HttpTx>, EventLoop> retry,
      Http2ConnectionHandler http2ConnectionHandler,
      ChannelPool channelPool) {
    this.retry = retry;
    this.http2ConnectionHandler = http2ConnectionHandler;
    this.channelPool = channelPool;
  }

  // streams dropped by a GOAWAY or refused with REFUSED_STREAM were not processed by the server, so
  // they can be retried whatever the request (RFC 9113 section 8.7), but only once to avoid
  // looping, and only if the body can be replayed
  private static boolean canRetryUnprocessedStream(HttpTx tx) {
    return tx.channelState != HttpTx.ChannelState.RETRY && tx.isBodyReplayable();
  }

  @Override
  public boolean isSharable() {
    return false;
  }

  boolean isResponseBodyDecompressionRequired(int streamId) {
    HttpTx tx = txByStreamId.get(streamId);
    return tx == null || tx.listener.isResponseBodyDecompressionRequired();
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
    writeReached = true;
    HttpTx tx = (HttpTx) msg;

    if (tx.requestTimeout.isDone()) {
      channelPool.offer(ctx.channel());
      return;
    }
    // we can't use Http2Connection.Endpoint#incrementAndGetNextStreamId as
    // HttpToHttp2ConnectionHandler already calls it for every request, even with an explicit stream
    // id, but never use an id Netty already created a stream for
    lastStreamId =
        Math.max(lastStreamId, http2ConnectionHandler.connection().local().lastStreamCreated());
    if (lastStreamId > Integer.MAX_VALUE - 2) {
      // after 2^30 streams, this connection can't open new ones: the request was never sent so it
      // can be sent on another connection, while the in-flight streams complete before closing
      LOGGER.debug("Stream ids exhausted, closing connection and retrying on a new one");
      ChannelPool.markAsGoAway(ctx.channel());
      retry.accept(Collections.singletonList(tx), ctx.channel().eventLoop());
      ctx.close();
      return;
    }
    lastStreamId += 2;
    int thisStreamId = lastStreamId;
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

            } else if (txByStreamId.remove(thisStreamId) != null) {
              // when a GOAWAY is received, StreamBufferingEncoder fails the promises of the
              // queued streams after we've already retried or failed them in userEventTriggered
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
    if (tx == null) {
      LOGGER.debug("Ignoring response headers for unknown stream {}", streamId);
      return;
    }

    if (tx.requestTimeout.isDone()) {
      tx.releasePendingRequestExpectingContinue();
      resetStream(ctx, streamId);
      return;
    }

    HttpResponseStatus status = response.status();

    if (tx.pendingRequestExpectingContinue != null) {
      if (status.equals(HttpResponseStatus.CONTINUE)) {
        // unlike with HTTP/1, 100-Continue is a HEADERS frame without END_STREAM: there's no
        // content to wait for
        LOGGER.debug("Received 100-Continue, sending body");
        WritableRequest request = tx.pendingRequestExpectingContinue;
        tx.pendingRequestExpectingContinue = null;
        request
            .writeContent(ctx)
            .addListener(
                f -> {
                  if (!f.isSuccess() && txByStreamId.remove(streamId) != null) {
                    tx.requestTimeout.cancel();
                    tx.listener.onThrowable(f.cause());
                  }
                });
        return;

      } else if (status.codeClass() == HttpStatusClass.INFORMATIONAL) {
        LOGGER.debug("Ignoring interim response {} while waiting for 100-Continue", status);
        return;

      } else {
        // eg a 417, Gatling then sends the request again without expectation, see ResponseProcessor
        // the stream is reset once the response completes, as we didn't send the body
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
    if (tx == null) {
      LOGGER.debug("Ignoring response content for unknown stream {}", streamId);
      return;
    }

    if (tx.requestTimeout.isDone()) {
      resetStream(ctx, streamId);
      return;
    }

    boolean last = content.last;
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
      // clear before notifying so that late write failures and channelInactive don't notify again
      List<HttpTx> txs = new ArrayList<>(txByStreamId.values());
      txByStreamId.clear();
      for (HttpTx tx : txs) {
        tx.releasePendingRequestExpectingContinue();
        tx.requestTimeout.cancel();
        tx.listener.onThrowable(cause);
      }
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
    crash(ctx, writeReached ? CONNECTION_CLOSED_EXCEPTION : REMOTELY_CLOSED_EXCEPTION, null, false);
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
            tx.releasePendingRequestExpectingContinue();
            if (canRetryUnprocessedStream(tx)) {
              retryTxs.add(tx);
            } else {
              tx.requestTimeout.cancel();
              tx.listener.onThrowable(REMOTELY_CLOSED_EXCEPTION);
            }
          });

      if (!retryTxs.isEmpty()) {
        retry.accept(retryTxs, ctx.channel().eventLoop());
      }

    } else if (evt instanceof RstStreamFrame) {
      RstStreamFrame rstStream = (RstStreamFrame) evt;
      HttpTx tx = txByStreamId.remove(rstStream.streamId);
      if (tx != null) {
        LOGGER.debug("Received RST_STREAM frame: {}", rstStream);
        // Netty closes the stream after notifying us
        tx.releasePendingRequestExpectingContinue();
        if (rstStream.errorCode == Http2Error.REFUSED_STREAM.code()
            && canRetryUnprocessedStream(tx)) {
          retry.accept(Collections.singletonList(tx), ctx.channel().eventLoop());
        } else {
          tx.requestTimeout.cancel();
          Http2Error error = Http2Error.valueOf(rstStream.errorCode);
          tx.listener.onThrowable(
              new IOException(
                  "Stream was reset by the server with error code "
                      + (error != null ? error : rstStream.errorCode)));
        }
        channelPool.offer(ctx.channel());
      }

    } else if (evt instanceof StreamTimeout) {
      resetStream(ctx, ((StreamTimeout) evt).streamId);
    }
  }

  private void closeStream(ChannelHandlerContext ctx, int streamId) {
    Http2Stream stream = http2ConnectionHandler.connection().stream(streamId);
    if (stream != null && stream.state() == Http2Stream.State.OPEN) {
      // the server sent a complete response before we sent the whole request, eg it didn't send
      // 100-Continue: closing the stream locally would leave it half-open on the server side
      resetStream(ctx, streamId);
      return;
    }
    txByStreamId.remove(streamId);
    if (stream != null) {
      stream.close();
    }
    channelPool.offer(ctx.channel());
  }

  private void resetStream(ChannelHandlerContext ctx, int streamId) {
    txByStreamId.remove(streamId);
    // the frame must be written from the HTTP/2 handler's context, otherwise it would go through
    // the outbound handlers in between and be wrapped as a DATA frame by
    // ByteBufToHttpContentEncoder
    ChannelHandlerContext http2Ctx = ctx.pipeline().context(http2ConnectionHandler);
    http2ConnectionHandler
        .resetStream(http2Ctx, streamId, Http2Error.CANCEL.code(), ctx.newPromise())
        .addListener((ChannelFutureListener) future -> channelPool.offer(ctx.channel()));
  }
}
