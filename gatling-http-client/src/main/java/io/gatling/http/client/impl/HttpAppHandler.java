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

import io.gatling.http.client.HttpListener;
import io.gatling.http.client.HttpClientConfig;
import io.gatling.http.client.ahc.util.HttpUtils;
import io.gatling.http.client.impl.request.WritableRequest;
import io.gatling.http.client.impl.request.WritableRequestBuilder;
import io.gatling.http.client.pool.ChannelPool;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.DecoderResultProvider;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

class HttpAppHandler extends ChannelDuplexHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpAppHandler.class);

  private static final IOException PREMATURE_CLOSE = new IOException("Premature close") {
    @Override
    public synchronized Throwable fillInStackTrace() {
      return this;
    }
  };

  private final DefaultHttpClient client;
  private final ChannelPool channelPool;
  private final HttpClientConfig config;
  private HttpTx tx;
  private boolean httpResponseReceived;

  HttpAppHandler(DefaultHttpClient client, ChannelPool channelPool, HttpClientConfig config) {
    this.client = client;
    this.channelPool = channelPool;
    this.config = config;
  }

  private void setActive(HttpTx tx) {
    this.tx = tx;
  }

  private void setInactive() {
    tx = null;
    httpResponseReceived = false;
  }

  private boolean isInactive() {
    return tx == null || tx.requestTimeout.isDone();
  }

  private void crash(ChannelHandlerContext ctx, Throwable cause, boolean close) {
    if (isInactive()) {
      return;
    }
    crash0(ctx, cause, close, tx);
  }

  private void crash0(ChannelHandlerContext ctx, Throwable cause, boolean close, HttpTx tx) {
    try {
      tx.requestTimeout.cancel();
      tx.listener.onThrowable(cause);
      setInactive();

    } catch (Exception e) {
      LOGGER.error("Exception while handling HTTP/1.1 crash, please report to Gatling maintainers", e);
    } finally {
      if (close) {
        ctx.close();
      }
    }
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {

    HttpTx tx = (HttpTx) msg;
    setActive(tx);

    if (tx.requestTimeout.isDone()) {
      setInactive();
      return;
    }

    try {
      WritableRequest request = WritableRequestBuilder.buildRequest(tx.request, ctx.alloc(), config, false);
      tx.closeConnection = HttpUtils.isConnectionClose(request.getRequest().headers());

      LOGGER.debug("Write request {}", request);

      request.write(ctx);
    } catch (Exception e) {
      crash(ctx, e, true);
    }
  }

  private boolean exitOnDecodingFailure(ChannelHandlerContext ctx, DecoderResultProvider message) {
    Throwable t = message.decoderResult().cause();
    if (t != null) {
      crash(ctx, t, true);
      return true;
    }
    return false;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (isInactive()) {
      return;
    }

    LOGGER.debug("Read msg='{}'", msg);

    try {
      if (msg instanceof HttpResponse) {
        httpResponseReceived = true;
        HttpResponse response = (HttpResponse) msg;
        if (exitOnDecodingFailure(ctx, response)) {
          return;
        }
        tx.listener.onHttpResponse(response.status(), response.headers());
        tx.closeConnection = tx.closeConnection && HttpUtils.isConnectionClose(response.headers());

      } else if (msg instanceof HttpContent) {
        HttpContent chunk = (HttpContent) msg;
        if (exitOnDecodingFailure(ctx, chunk)) {
          return;
        }
        boolean last = chunk instanceof LastHttpContent;

        // making a local copy because setInactive might be called (on last)
        HttpTx tx = this.tx;

        if (last) {
          tx.requestTimeout.cancel();
          setInactive();
          if (tx.closeConnection) {
            ctx.channel().close();
          } else {
            channelPool.offer(ctx.channel());
          }
        }

        try {
          tx.listener.onHttpResponseBodyChunk(chunk.content(), last);
        } catch (Exception e) {
          // can't let exceptionCaught handle this because setInactive might have been called (on last)
          crash0(ctx, e, true, tx);
          throw e;
        }
      }
    } finally {
      ReferenceCountUtil.release(msg);
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
    if (isInactive()) {
      return;
    }

    if (!httpResponseReceived && client.retry(tx, ctx.channel().eventLoop())) {
      // only retry when we haven't started receiving response
      setInactive();
    } else {
      crash(ctx, PREMATURE_CLOSE, false);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    crash(ctx, cause, true);
  }
}
