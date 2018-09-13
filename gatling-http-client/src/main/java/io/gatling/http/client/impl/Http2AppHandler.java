/*
 * Copyright 2011-2018 GatlingCorp (https://gatling.io)
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
import io.gatling.http.client.ahc.util.HttpUtils;
import io.gatling.http.client.impl.request.WritableRequest;
import io.gatling.http.client.impl.request.WritableRequestBuilder;
import io.gatling.http.client.pool.ChannelPool;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Http2AppHandler extends ChannelDuplexHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(Http2AppHandler.class);
  private static final IOException REMOTELY_CLOSED_EXCEPTION = new IOException("Channel was closed before handshake completed");

  private final Http2Connection connection;
  private final Http2Connection.PropertyKey propertyKey;
  private final Http2ConnectionHandler http2ConnectionHandler;
  private final ChannelPool channelPool;
  private final HttpClientConfig config;

  // mutable state
  private int nextStreamId = 1;

  Http2AppHandler(Http2Connection connection, Http2ConnectionHandler http2ConnectionHandler, ChannelPool channelPool, HttpClientConfig config) {
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
      tx.closeConnection = HttpUtils.isConnectionClose(request.getRequest().headers());
      LOGGER.debug("Write request {}", request);

      request.write(ctx).addListener(f -> {
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
        http2ConnectionHandler.resetStream(ctx, streamId, 8, ctx.newPromise());
        channelPool.offer(ctx.channel());
        return;
      }

      tx.listener.onHttpResponse(response.status(), response.headers());

    } else if (msg instanceof Http2Content){
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
      tx.listener.onHttpResponseBodyChunk(httpContent.content(), last);
      if (last) {
        tx.requestTimeout.cancel();
        channelPool.offer(ctx.channel());
      }
    }
  }

  private void crash(ChannelHandlerContext ctx, Throwable cause, HttpListener nonActiveStreamListener, boolean close) {
      try {
        if (nonActiveStreamListener != null) {
          nonActiveStreamListener.onThrowable(cause);
        }
        connection.forEachActiveStream(stream -> {
          HttpTx tx = stream.getProperty(propertyKey);
          tx.listener.onThrowable(cause);
          return true;
        });
      } catch (Http2Exception e) {
        LOGGER.error("Can't properly close active streams");
      }

    if (close) {
      ctx.close();
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    crash(ctx, cause, null,true);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
    // FIXME retry?
    crash(ctx, REMOTELY_CLOSED_EXCEPTION, null, false);
  }
}
