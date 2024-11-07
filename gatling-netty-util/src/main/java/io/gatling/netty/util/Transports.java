/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

package io.gatling.netty.util;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.uring.IoUring;
import io.netty.channel.uring.IoUringDatagramChannel;
import io.netty.channel.uring.IoUringIoHandler;
import io.netty.channel.uring.IoUringSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.util.concurrent.ThreadFactory;

public final class Transports {

  private Transports() {}

  public static EventLoopGroup newEventLoopGroup(
      boolean useNativeTransport, boolean useIoUring, int nThreads, String poolName) {
    ThreadFactory threadFactory = new DefaultThreadFactory(poolName);

    IoHandlerFactory ioHandlerFactory = null;
    if (useNativeTransport) {
      if (useIoUring && IoUring.isAvailable()) {
        ioHandlerFactory = IoUringIoHandler.newFactory();
      } else if (Epoll.isAvailable()) {
        ioHandlerFactory = EpollIoHandler.newFactory();
      }
    }
    if (ioHandlerFactory == null) {
      ioHandlerFactory = NioIoHandler.newFactory();
    }

    return new MultiThreadIoEventLoopGroup(nThreads, threadFactory, ioHandlerFactory);
  }

  private static final ChannelFactory<? extends SocketChannel> EPOLL_SOCKET_CHANNEL_FACTORY =
      EpollSocketChannel::new;

  private static final ChannelFactory<? extends SocketChannel> IOURING_SOCKET_CHANNEL_FACTORY =
      IoUringSocketChannel::new;
  private static final ChannelFactory<? extends SocketChannel> NIO_SOCKET_CHANNEL_FACTORY =
      NioSocketChannel::new;

  public static ChannelFactory<? extends SocketChannel> newSocketChannelFactory(
      boolean useNativeTransport, boolean useIoUring) {
    if (useNativeTransport) {
      if (useIoUring && IoUring.isAvailable()) {
        return IOURING_SOCKET_CHANNEL_FACTORY;
      } else if (Epoll.isAvailable()) {
        return EPOLL_SOCKET_CHANNEL_FACTORY;
      }
    }
    return NIO_SOCKET_CHANNEL_FACTORY;
  }

  private static final ChannelFactory<? extends DatagramChannel> EPOLL_DATAGRAM_CHANNEL_FACTORY =
      EpollDatagramChannel::new;

  private static final ChannelFactory<? extends DatagramChannel> IOURING_DATAGRAM_CHANNEL_FACTORY =
      IoUringDatagramChannel::new;
  private static final ChannelFactory<? extends DatagramChannel> NIO_DATAGRAM_CHANNEL_FACTORY =
      NioDatagramChannel::new;

  public static ChannelFactory<? extends DatagramChannel> newDatagramChannelFactory(
      boolean useNativeTransport, boolean useIoUring) {
    if (useNativeTransport) {
      if (useIoUring && IoUring.isAvailable()) {
        return IOURING_DATAGRAM_CHANNEL_FACTORY;
      } else if (Epoll.isAvailable()) {
        return EPOLL_DATAGRAM_CHANNEL_FACTORY;
      }
    }
    return NIO_DATAGRAM_CHANNEL_FACTORY;
  }

  public static void configureOptions(
      Bootstrap bootstrap,
      int connectTimeout,
      boolean tcpNoDelay,
      boolean soKeepAlive,
      boolean useEpoll) {
    bootstrap
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
        .option(ChannelOption.TCP_NODELAY, tcpNoDelay)
        .option(ChannelOption.SO_KEEPALIVE, soKeepAlive);
  }
}
