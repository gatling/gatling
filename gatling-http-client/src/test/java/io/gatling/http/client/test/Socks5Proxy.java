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
import io.netty.handler.codec.socksx.v5.*;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** A SOCKS5 proxy that only supports CONNECT, optionally with username/password authentication. */
public final class Socks5Proxy implements AutoCloseable {

  private final EventLoopGroup group =
      new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
  private final String username;
  private final String password;
  private final Channel serverChannel;

  /** the CONNECT targets, as host:port */
  public final List<String> connectTargets = new CopyOnWriteArrayList<>();

  /** the usernames that successfully authenticated */
  public final List<String> authenticatedUsernames = new CopyOnWriteArrayList<>();

  /**
   * @param username null for no authentication
   * @param password null for no authentication
   */
  public Socks5Proxy(String username, String password) throws InterruptedException {
    this.username = username;
    this.password = password;
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
                            Socks5ServerEncoder.DEFAULT,
                            new Socks5InitialRequestDecoder(),
                            new Socks5Handler());
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

  private final class Socks5Handler extends SimpleChannelInboundHandler<Socks5Message> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Socks5Message msg) {
      ChannelPipeline pipeline = ctx.pipeline();

      if (msg instanceof Socks5InitialRequest) {
        if (username == null) {
          pipeline.replace(
              Socks5InitialRequestDecoder.class, "decoder", new Socks5CommandRequestDecoder());
          ctx.writeAndFlush(new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH));
        } else if (((Socks5InitialRequest) msg).authMethods().contains(Socks5AuthMethod.PASSWORD)) {
          pipeline.replace(
              Socks5InitialRequestDecoder.class, "decoder", new Socks5PasswordAuthRequestDecoder());
          ctx.writeAndFlush(new DefaultSocks5InitialResponse(Socks5AuthMethod.PASSWORD));
        } else {
          ctx.writeAndFlush(new DefaultSocks5InitialResponse(Socks5AuthMethod.UNACCEPTED))
              .addListener(ChannelFutureListener.CLOSE);
        }

      } else if (msg instanceof Socks5PasswordAuthRequest) {
        Socks5PasswordAuthRequest auth = (Socks5PasswordAuthRequest) msg;
        if (username.equals(auth.username()) && password.equals(auth.password())) {
          authenticatedUsernames.add(auth.username());
          pipeline.replace(
              Socks5PasswordAuthRequestDecoder.class, "decoder", new Socks5CommandRequestDecoder());
          ctx.writeAndFlush(
              new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS));
        } else {
          ctx.writeAndFlush(new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.FAILURE))
              .addListener(ChannelFutureListener.CLOSE);
        }

      } else if (msg instanceof Socks5CommandRequest) {
        Socks5CommandRequest command = (Socks5CommandRequest) msg;
        if (command.type() != Socks5CommandType.CONNECT) {
          ctx.writeAndFlush(
                  new DefaultSocks5CommandResponse(
                      Socks5CommandStatus.COMMAND_UNSUPPORTED, command.dstAddrType()))
              .addListener(ChannelFutureListener.CLOSE);
          return;
        }
        connect(ctx, command);
      }
    }

    private void connect(ChannelHandlerContext ctx, Socks5CommandRequest command) {
      Channel inbound = ctx.channel();
      connectTargets.add(command.dstAddr() + ":" + command.dstPort());
      inbound.config().setAutoRead(false);
      new Bootstrap()
          .group(inbound.eventLoop())
          .channel(NioSocketChannel.class)
          .handler(new TcpRelay(inbound))
          .connect(command.dstAddr(), command.dstPort())
          .addListener(
              (ChannelFutureListener)
                  f -> {
                    if (!f.isSuccess()) {
                      inbound
                          .writeAndFlush(
                              new DefaultSocks5CommandResponse(
                                  Socks5CommandStatus.FAILURE, command.dstAddrType()))
                          .addListener(ChannelFutureListener.CLOSE);
                      return;
                    }
                    // the response is encoded synchronously, before the codec is removed
                    inbound.writeAndFlush(
                        new DefaultSocks5CommandResponse(
                            Socks5CommandStatus.SUCCESS,
                            command.dstAddrType(),
                            command.dstAddr(),
                            command.dstPort()));
                    ChannelPipeline pipeline = inbound.pipeline();
                    pipeline.remove(Socks5ServerEncoder.class);
                    pipeline.remove(Socks5CommandRequestDecoder.class);
                    pipeline.replace(this, "relay", new TcpRelay(f.channel()));
                    inbound.config().setAutoRead(true);
                  });
    }
  }
}
