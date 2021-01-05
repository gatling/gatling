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

package io.gatling.netty.util;

import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.ThreadFactory;

public final class Transports {

  private Transports() {
  }

  public static EventLoopGroup newEventLoopGroup(boolean useNativeTransport, int nThreads, String poolName) {
    ThreadFactory threadFactory = new DefaultThreadFactory(poolName);
    return useNativeTransport && Epoll.isAvailable() ? new EpollEventLoopGroup(nThreads, threadFactory) : new NioEventLoopGroup(nThreads, threadFactory);
  }

  private static final ChannelFactory<? extends SocketChannel> EPOLL_SOCKET_CHANNEL_FACTORY = EpollSocketChannel::new;
  private static final ChannelFactory<? extends SocketChannel> NIO_SOCKET_CHANNEL_FACTORY = NioSocketChannel::new;

  public static ChannelFactory<? extends SocketChannel> newSocketChannelFactory(boolean useNativeTransport) {
    return useNativeTransport && Epoll.isAvailable() ? EPOLL_SOCKET_CHANNEL_FACTORY : NIO_SOCKET_CHANNEL_FACTORY;
  }

  private static final ChannelFactory<? extends DatagramChannel> EPOLL_DATAGRAM_CHANNEL_FACTORY = EpollDatagramChannel::new;
  private static final ChannelFactory<? extends DatagramChannel> NIO_DATAGRAM_CHANNEL_FACTORY = NioDatagramChannel::new;

  public static ChannelFactory<? extends DatagramChannel> newDatagramChannelFactory(boolean useNativeTransport) {
    return useNativeTransport && Epoll.isAvailable() ? EPOLL_DATAGRAM_CHANNEL_FACTORY : NIO_DATAGRAM_CHANNEL_FACTORY;
  }
}
