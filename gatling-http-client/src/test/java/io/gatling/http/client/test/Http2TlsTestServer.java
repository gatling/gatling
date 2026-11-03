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
import io.netty.handler.codec.http2.*;
import io.netty.handler.ssl.*;
import io.netty.util.ReferenceCountUtil;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;

/**
 * A TLS server that negotiates HTTP/2 or HTTP/1.1 with ALPN, and responds to every request with the
 * protocol it was received with as the body: "h2" or "http/1.1".
 */
public final class Http2TlsTestServer implements AutoCloseable {

  private final EventLoopGroup group =
      new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
  private final Channel serverChannel;

  /**
   * @param http2Supported false to emulate an origin that only supports HTTP/1.1
   */
  public Http2TlsTestServer(boolean http2Supported) throws Exception {
    SslContext sslContext = serverSslContext(http2Supported);
    serverChannel =
        new ServerBootstrap()
            .group(group)
            .channel(NioServerSocketChannel.class)
            .childHandler(
                new ChannelInitializer<SocketChannel>() {
                  @Override
                  protected void initChannel(SocketChannel ch) {
                    ch.pipeline()
                        .addLast(
                            sslContext.newHandler(ch.alloc()),
                            new ApplicationProtocolNegotiationHandler(
                                ApplicationProtocolNames.HTTP_1_1) {
                              @Override
                              protected void configurePipeline(
                                  ChannelHandlerContext ctx, String protocol) {
                                if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
                                  ctx.pipeline()
                                      .addLast(
                                          Http2FrameCodecBuilder.forServer().build(),
                                          new Http2MultiplexHandler(Http2Responder.INSTANCE));
                                } else {
                                  ctx.pipeline()
                                      .addLast(
                                          new HttpServerCodec(),
                                          new HttpObjectAggregator(64 * 1024),
                                          new Http1Responder());
                                }
                              }
                            });
                  }
                })
            .bind(0)
            .sync()
            .channel();
  }

  static SslContext serverSslContext(boolean http2Supported) throws Exception {
    KeyStore keyStore = KeyStore.getInstance("JKS");
    try (InputStream is =
        Http2TlsTestServer.class.getClassLoader().getResourceAsStream("ssltest-keystore.jks")) {
      keyStore.load(is, "changeit".toCharArray());
    }
    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(keyStore, "changeit".toCharArray());

    String[] protocols =
        http2Supported
            ? new String[] {ApplicationProtocolNames.HTTP_2, ApplicationProtocolNames.HTTP_1_1}
            : new String[] {ApplicationProtocolNames.HTTP_1_1};
    return SslContextBuilder.forServer(kmf)
        .sslProvider(SslProvider.JDK)
        .applicationProtocolConfig(
            new ApplicationProtocolConfig(
                ApplicationProtocolConfig.Protocol.ALPN,
                ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                protocols))
        .build();
  }

  public int getPort() {
    return ((InetSocketAddress) serverChannel.localAddress()).getPort();
  }

  public String getUrl() {
    return "https://localhost:" + getPort() + "/";
  }

  @Override
  public void close() {
    serverChannel.close().syncUninterruptibly();
    group.shutdownGracefully();
  }

  @ChannelHandler.Sharable
  private static final class Http2Responder extends ChannelInboundHandlerAdapter {
    private static final Http2Responder INSTANCE = new Http2Responder();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
      try {
        if (msg instanceof Http2HeadersFrame && ((Http2HeadersFrame) msg).isEndStream()
            || msg instanceof Http2DataFrame && ((Http2DataFrame) msg).isEndStream()) {
          ctx.write(new DefaultHttp2HeadersFrame(new DefaultHttp2Headers().status("200")));
          ctx.writeAndFlush(
              new DefaultHttp2DataFrame(
                  Unpooled.copiedBuffer(ApplicationProtocolNames.HTTP_2, UTF_8), true));
        }
      } finally {
        ReferenceCountUtil.release(msg);
      }
    }
  }

  private static final class Http1Responder extends SimpleChannelInboundHandler<FullHttpRequest> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
      FullHttpResponse response =
          new DefaultFullHttpResponse(
              HttpVersion.HTTP_1_1,
              HttpResponseStatus.OK,
              Unpooled.copiedBuffer(ApplicationProtocolNames.HTTP_1_1, UTF_8));
      response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
      ctx.writeAndFlush(response);
    }
  }
}
