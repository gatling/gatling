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
import io.gatling.http.client.WebSocketListener;
import io.gatling.http.client.impl.request.WritableRequest;
import io.gatling.http.client.impl.request.WritableRequestBuilder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class WebSocketHandler extends ChannelDuplexHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketHandler.class);

  private static final IOException PREMATURE_CLOSE = new IOException("Premature close") {
    @Override
    public synchronized Throwable fillInStackTrace() {
      return this;
    }
  };

  private final HttpClientConfig config;
  private HttpTx tx;
  private WebSocketListener wsListener;
  private WebSocketClientHandshaker handshaker;

  WebSocketHandler(HttpClientConfig config) {
    this.config = config;
  }

  private void setActive(HttpTx tx) {
    this.tx = tx;
    wsListener = (WebSocketListener) tx.listener;
  }

  private void crash(ChannelHandlerContext ctx, Throwable cause, boolean close) {
    LOGGER.debug("Crash", cause);
    if (tx == null) {
      return;
    }

    wsListener.onThrowable(cause);
    tx.requestTimeout.cancel();
    tx = null;

    if (close) {
      ctx.close();
    }
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
    if (msg instanceof HttpTx) {

      HttpTx tx = (HttpTx) msg;
      setActive(tx);

      if (tx.requestTimeout.isDone()) {
        return;
      }

      try {
        WritableRequest request = WritableRequestBuilder.buildRequest(tx.request, ctx.alloc(), config, false);

        handshaker =
          WebSocketClientHandshakerFactory.newHandshaker(
            tx.request.getUri().toJavaNetURI(),
            WebSocketVersion.V13,
            null,
            true,
            request.getRequest().headers(),
            config.getWebSocketMaxFramePayloadLength());

        handshaker.handshake(ctx.channel());

      } catch (Exception e) {
        crash(ctx, e, true);
      }
    } else {
      // all other messages are CONNECT request and WebSocket frames
      LOGGER.debug("ctx.write msg={}", msg);
      ctx.write(msg, promise);
    }
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    LOGGER.debug("Read msg={}", msg);

    Channel ch = ctx.channel();
    if (!handshaker.isHandshakeComplete()) {
      if (tx == null || tx.requestTimeout.isDone()) {
        return;
      }

      try {
        // received 101 response
        FullHttpResponse response = (FullHttpResponse) msg;
        handshaker.finishHandshake(ch, response);
        tx.requestTimeout.cancel();

        wsListener.onHttpResponse(response.status(), response.headers());
        wsListener.openWebSocket(ch);

      } catch (WebSocketHandshakeException e) {
        crash(ctx, e, true);
      }
      return;
    }

    WebSocketFrame frame = (WebSocketFrame) msg;
    if (frame instanceof TextWebSocketFrame) {
      wsListener.onTextFrame((TextWebSocketFrame) frame);
    } else if (frame instanceof BinaryWebSocketFrame) {
      wsListener.onBinaryFrame((BinaryWebSocketFrame) frame);
    } else if (frame instanceof PongWebSocketFrame) {
      wsListener.onPongFrame((PongWebSocketFrame) frame);
    } else if (frame instanceof CloseWebSocketFrame) {
      wsListener.onCloseFrame((CloseWebSocketFrame) frame);
      ch.close();
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
    LOGGER.debug("channelInactive");
    crash(ctx, PREMATURE_CLOSE, false);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    LOGGER.debug("exceptionCaught");
    crash(ctx, cause, true);
  }
}
