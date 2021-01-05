/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

import io.gatling.http.client.HttpClientConfig;
import io.gatling.http.client.HttpListener;
import io.gatling.http.client.impl.request.WritableRequest;
import io.gatling.http.client.impl.request.WritableRequestBuilder;
import io.gatling.http.client.pool.ChannelPool;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Http2AppHandler extends ChannelDuplexHandler {

  public static final class GoAwayFrame {
    private final int lastStreamId;
    private final long errorCode;

    public GoAwayFrame(int lastStreamId, long errorCode) {
      this.lastStreamId = lastStreamId;
      this.errorCode = errorCode;
    }

    @Override
    public String toString() {
      return "GoAwayFrame{lastStreamId=" + lastStreamId +
        ", errorCode=" + errorCode +
        '}';
    }
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(Http2AppHandler.class);
  private static final IOException REMOTELY_CLOSED_EXCEPTION = new IOException("Channel was closed before handshake completed");

  private final DefaultHttpClient client;
  private final Http2Connection connection;
  private final Http2Connection.PropertyKey propertyKey;
  private final Http2ConnectionHandler http2ConnectionHandler;
  private final ChannelPool channelPool;
  private final HttpClientConfig config;

  // mutable state
  private int nextStreamId = 1;

  Http2AppHandler(DefaultHttpClient client, Http2Connection connection, Http2ConnectionHandler http2ConnectionHandler, ChannelPool channelPool, HttpClientConfig config) {
    this.client = client;
    this.connection = connection;
    this.propertyKey = connection.newKey();
    this.http2ConnectionHandler = http2ConnectionHandler;
    this.channelPool = channelPool;
    this.config = config;
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
    HttpTx tx = (HttpTx) msg;
    nextStreamId += 2;

    if (tx.requestTimeout.isDone()) {
      channelPool.offer(ctx.channel());
      return;
    }

    try {
      WritableRequest request = WritableRequestBuilder.buildRequest(tx.request, ctx.alloc(), config, true);
      LOGGER.debug("Write request {}", request);

      tx.listener.onWrite(ctx.channel());

      ChannelFuture whenWrite;
      if (HttpUtil.is100ContinueExpected(request.getRequest())) {
        LOGGER.debug("Delaying body write");
        tx.pendingRequestExpectingContinue = request;
        whenWrite = request.writeWithoutContent(ctx);
      } else {
        whenWrite = request.write(ctx);
      }

      whenWrite.addListener(f -> {
        if (f.isSuccess()) {
          Http2Stream stream = connection.stream(nextStreamId);
          stream.setProperty(propertyKey, tx);
        } else {
          crash(ctx, f.cause(), tx.listener, true);
        }
      });
    } catch (Exception e) {
      crash(ctx, e, tx.listener, true);
    }
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (msg instanceof HttpResponse) {
      HttpResponse response = (HttpResponse) msg;
      Integer streamId = response.headers().getInt(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text());
      Http2Stream stream = connection.stream(streamId);
      HttpTx tx = stream.getProperty(propertyKey);

      if (tx.requestTimeout.isDone()) {
        tx.releasePendingRequestExpectingContinue();
        http2ConnectionHandler.resetStream(ctx, streamId, 8, ctx.newPromise());
        channelPool.offer(ctx.channel());
        return;
      }

      HttpResponseStatus status = response.status();

      if (tx.pendingRequestExpectingContinue != null) {
        if (status.equals(HttpResponseStatus.CONTINUE)) {
          LOGGER.debug("Received 100-Continue");
          return;

        } else {
          // TODO implement 417 support
          LOGGER.debug("Request was sent with Expect:100-Continue but received response with status {}, dropping", status);
          tx.releasePendingRequestExpectingContinue();
        }
      }

      tx.listener.onHttpResponse(status, response.headers());

    } else if (msg instanceof Http2Content) {
      Http2Content content = (Http2Content) msg;
      int streamId = content.getStreamId();
      Http2Stream stream = connection.stream(streamId);
      HttpTx tx = stream.getProperty(propertyKey);

      if (tx.requestTimeout.isDone()) {
        http2ConnectionHandler.resetStream(ctx, streamId, 8, ctx.newPromise());
        channelPool.offer(ctx.channel());
        return;
      }

      HttpContent httpContent = content.getHttpContent();
      boolean last = httpContent instanceof LastHttpContent;

      if (tx.pendingRequestExpectingContinue != null) {
        if (last) {
          LOGGER.debug("Received 100-Continue' LastHttpContent, sending body");
          tx.pendingRequestExpectingContinue.writeContent(ctx);
          tx.pendingRequestExpectingContinue = null;
        }
        return;
      }

      tx.listener.onHttpResponseBodyChunk(httpContent.content(), last);
      if (last) {
        tx.requestTimeout.cancel();
        channelPool.offer(ctx.channel());
      }
    } else if (msg instanceof GoAwayFrame) {
      GoAwayFrame goAway = (GoAwayFrame) msg;

      LOGGER.debug("Received GOAWAY frame: {}", goAway);
      List<HttpTx> retryTxs = new ArrayList<>(3);

      try {
        connection.forEachActiveStream(stream -> {
          if (stream.id() > goAway.lastStreamId) {
            HttpTx tx = stream.getProperty(propertyKey);
            tx.releasePendingRequestExpectingContinue();
            if (goAway.errorCode == 0 && client.canRetry(tx)) {
              retryTxs.add(tx);
            } else {
              tx.listener.onThrowable(REMOTELY_CLOSED_EXCEPTION);
            }
          }
          return true;
        });
      } catch (Http2Exception e) {
        LOGGER.error("Failed to close active streams on GOAWAY", e);
      }

      if (!retryTxs.isEmpty()) {
        client.retryHttp2(retryTxs, ctx.channel().eventLoop());
      }

      ctx.close();
    }
  }

  private void crash(ChannelHandlerContext ctx, Throwable cause, HttpListener nonActiveStreamListener, boolean close) {
    try {
      if (nonActiveStreamListener != null) {
        nonActiveStreamListener.onThrowable(cause);
      }
      connection.forEachActiveStream(stream -> {
        HttpTx tx = stream.getProperty(propertyKey);
        tx.releasePendingRequestExpectingContinue();
        tx.listener.onThrowable(cause);
        return true;
      });

    } catch (Http2Exception e) {
      LOGGER.error("Failed to close active streams", e);
    } finally {
      if (close) {
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
    // FIXME retry?
    crash(ctx, REMOTELY_CLOSED_EXCEPTION, null, false);
  }
}
