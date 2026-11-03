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

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An HTTP proxy that only supports CONNECT tunnels, like the ones used for HTTPS requests. It can
 * be reached over TLS, in which case it offers HTTP/2 with ALPN, like a proxy that supports both,
 * so tests can check clients don't negotiate it for the proxy connection.
 */
public final class ConnectProxy implements AutoCloseable {

  private final EventLoopGroup group =
      new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
  private final Channel serverChannel;

  /** the CONNECT request targets, as host:port */
  public final List<String> connectTargets = new CopyOnWriteArrayList<>();

  /** the protocols negotiated with ALPN on the proxy connections, "none" without ALPN */
  public final List<String> negotiatedProtocols = new CopyOnWriteArrayList<>();

  /**
   * @param secured true for a proxy reached over TLS
   */
  public ConnectProxy(boolean secured) throws Exception {
    SslContext sslContext = secured ? Http2TlsTestServer.serverSslContext(true) : null;
    serverChannel =
        new ServerBootstrap()
            .group(group)
            .channel(NioServerSocketChannel.class)
            .childHandler(
                new ChannelInitializer<SocketChannel>() {
                  @Override
                  protected void initChannel(SocketChannel ch) {
                    if (sslContext != null) {
                      SslHandler sslHandler = sslContext.newHandler(ch.alloc());
                      sslHandler
                          .handshakeFuture()
                          .addListener(
                              f -> {
                                String protocol = sslHandler.applicationProtocol();
                                negotiatedProtocols.add(protocol != null ? protocol : "none");
                              });
                      ch.pipeline().addLast(sslHandler);
                    }
                    ch.pipeline()
                        .addLast(
                            new HttpServerCodec(),
                            new HttpObjectAggregator(8 * 1024),
                            new ConnectHandler());
                  }
                })
            .bind(0)
            .sync()
            .channel();
  }

  public int getPort() {
    return ((InetSocketAddress) serverChannel.localAddress()).getPort();
  }

  @Override
  public void close() {
    serverChannel.close().syncUninterruptibly();
    group.shutdownGracefully();
  }

  private final class ConnectHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
      Channel inbound = ctx.channel();
      if (!HttpMethod.CONNECT.equals(request.method())) {
        inbound
            .writeAndFlush(
                new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.METHOD_NOT_ALLOWED))
            .addListener(ChannelFutureListener.CLOSE);
        return;
      }

      String target = request.uri();
      connectTargets.add(target);
      int colon = target.lastIndexOf(':');
      String host = target.substring(0, colon);
      int port = Integer.parseInt(target.substring(colon + 1));

      inbound.config().setAutoRead(false);
      new Bootstrap()
          .group(inbound.eventLoop())
          .channel(NioSocketChannel.class)
          .handler(new Relay(inbound))
          .connect(host, port)
          .addListener(
              (ChannelFutureListener)
                  f -> {
                    if (!f.isSuccess()) {
                      inbound
                          .writeAndFlush(
                              new DefaultFullHttpResponse(
                                  HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY))
                          .addListener(ChannelFutureListener.CLOSE);
                      return;
                    }
                    // the response is encoded synchronously, before the codec is removed
                    inbound.writeAndFlush(
                        new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK));
                    ChannelPipeline pipeline = inbound.pipeline();
                    pipeline.remove(HttpServerCodec.class);
                    pipeline.remove(HttpObjectAggregator.class);
                    pipeline.replace(this, "relay", new Relay(f.channel()));
                    inbound.config().setAutoRead(true);
                  });
    }
  }

  private static final class Relay extends ChannelInboundHandlerAdapter {
    private final Channel peer;

    private Relay(Channel peer) {
      this.peer = peer;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
      peer.writeAndFlush(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
      peer.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      ctx.close();
    }
  }
}
