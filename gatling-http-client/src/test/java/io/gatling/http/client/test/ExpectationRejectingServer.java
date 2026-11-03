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

package io.gatling.http.client.test;

import static java.nio.charset.StandardCharsets.UTF_8;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An HTTP/1.1 server that doesn't support expectations, like when an HTTP/1.0 hop is involved: it
 * responds to requests with {@code Expect: 100-continue} with a 417 and keeps the connection open,
 * and echoes the body of the other requests.
 */
public final class ExpectationRejectingServer implements AutoCloseable {

  private final EventLoopGroup group =
      new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
  private final Channel serverChannel;

  /** "expect" for rejected requests, "body=..." for the others */
  public final List<String> requests = new CopyOnWriteArrayList<>();

  public final AtomicInteger connections = new AtomicInteger();

  public ExpectationRejectingServer() throws InterruptedException {
    serverChannel =
        new ServerBootstrap()
            .group(group)
            .channel(NioServerSocketChannel.class)
            .childHandler(
                new ChannelInitializer<SocketChannel>() {
                  @Override
                  protected void initChannel(SocketChannel ch) {
                    connections.incrementAndGet();
                    ch.pipeline().addLast(new HttpServerCodec(), new Handler());
                  }
                })
            .bind(0)
            .sync()
            .channel();
  }

  public String getUrl() {
    return "http://localhost:" + ((InetSocketAddress) serverChannel.localAddress()).getPort() + "/";
  }

  @Override
  public void close() {
    serverChannel.close().syncUninterruptibly();
    group.shutdownGracefully();
  }

  private final class Handler extends SimpleChannelInboundHandler<HttpObject> {
    private boolean rejected;
    private final StringBuilder body = new StringBuilder();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
      if (msg instanceof HttpRequest) {
        if (HttpUtil.is100ContinueExpected((HttpRequest) msg)) {
          rejected = true;
          requests.add("expect");
          FullHttpResponse response =
              new DefaultFullHttpResponse(
                  HttpVersion.HTTP_1_1, HttpResponseStatus.EXPECTATION_FAILED);
          response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, 0);
          ctx.writeAndFlush(response);
          return;
        }
        rejected = false;
        body.setLength(0);
      }

      if (msg instanceof HttpContent && !rejected) {
        body.append(((HttpContent) msg).content().toString(UTF_8));
        if (msg instanceof LastHttpContent) {
          requests.add("body=" + body);
          FullHttpResponse response =
              new DefaultFullHttpResponse(
                  HttpVersion.HTTP_1_1,
                  HttpResponseStatus.OK,
                  Unpooled.copiedBuffer(body.toString(), UTF_8));
          response
              .headers()
              .setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
          ctx.writeAndFlush(response);
        }
      }
    }
  }
}
