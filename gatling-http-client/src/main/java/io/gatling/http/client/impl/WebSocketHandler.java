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
import io.gatling.http.client.WebSocketListener;
import io.gatling.http.client.impl.request.WritableRequest;
import io.gatling.http.client.impl.request.WritableRequestBuilder;
import io.gatling.http.client.proxy.HttpProxyServer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.gatling.http.client.impl.HttpAppHandler.PREMATURE_CLOSE;

public class WebSocketHandler extends ChannelDuplexHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketHandler.class);

  private final HttpClientConfig config;
  private HttpTx tx;
  private WebSocketListener wsListener;
  private WebSocketClientHandshaker handshaker;
  private boolean remotelyClosed;

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

        boolean absoluteUpgradeUrl = !tx.request.getUri().isSecured() && tx.request.getProxyServer() instanceof HttpProxyServer;
        handshaker =
          WebSocketClientHandshakerFactory.newHandshaker(
            tx.request.getUri().toJavaNetURI(), // webSocketURL
            WebSocketVersion.V13, // version
            tx.request.getWsSubprotocol(), // subprotocol
            true, // allowExtensions
            request.getRequest().headers(), // customHeaders
            Integer.MAX_VALUE, // maxFramePayloadLength
            true, // performMasking
            false, // allowMaskMismatch
            -1, //forceCloseTimeoutMillis
            absoluteUpgradeUrl
          );

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
        try {
          handshaker.finishHandshake(ch, response);
          tx.requestTimeout.cancel();

          wsListener.onHttpResponse(response.status(), response.headers());
          wsListener.openWebSocket(ch);
        } finally {
          response.release();
        }

      } catch (WebSocketHandshakeException e) {
        crash(ctx, e, true);
      }
      return;
    }

    WebSocketFrame frame = (WebSocketFrame) msg;
    try {
      if (frame instanceof TextWebSocketFrame) {
        wsListener.onTextFrame((TextWebSocketFrame) frame);
      } else if (frame instanceof BinaryWebSocketFrame) {
        wsListener.onBinaryFrame((BinaryWebSocketFrame) frame);
      } else if (frame instanceof PingWebSocketFrame) {
        ctx.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
      } else if (frame instanceof PongWebSocketFrame) {
        wsListener.onPongFrame((PongWebSocketFrame) frame);
      } else if (frame instanceof CloseWebSocketFrame) {
        remotelyClosed = true;
        wsListener.onCloseFrame((CloseWebSocketFrame) frame);
        ch.close();
      }
    } finally {
      frame.release();
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
    LOGGER.debug("channelInactive");
    if (!remotelyClosed) {
      crash(ctx, PREMATURE_CLOSE, false);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    LOGGER.debug("exceptionCaught", cause);
    crash(ctx, cause, true);
  }
}
