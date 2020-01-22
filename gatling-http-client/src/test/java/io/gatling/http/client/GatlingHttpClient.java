/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
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

package io.gatling.http.client;

import io.gatling.http.client.impl.DefaultHttpClient;
import io.gatling.http.client.resolver.InetAddressNameResolver;
import io.gatling.http.client.resolver.InetAddressNameResolverWrapper;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import io.netty.util.concurrent.DefaultThreadFactory;

import javax.net.ssl.SSLException;

// FIXME remove
public class GatlingHttpClient implements AutoCloseable {

  private final SslContext sslContext;
  protected final HttpClient client;
  private final EventLoopGroup eventLoopGroup;
  private final InetAddressNameResolver nameResolver; // would be per request in Gatling

  public GatlingHttpClient(HttpClientConfig config) {
    this.client = new DefaultHttpClient(config);
    DefaultThreadFactory threadFactory = new DefaultThreadFactory(config.getThreadPoolName());
    eventLoopGroup = config.isUseNativeTransport() ? new EpollEventLoopGroup(0, threadFactory) : new NioEventLoopGroup(0, threadFactory);
    try {
      sslContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
    } catch (SSLException e) {
      throw new ExceptionInInitializerError(e);
    }

    this.nameResolver = new InetAddressNameResolverWrapper(
      new DnsNameResolverBuilder(eventLoopGroup.next())
        .channelFactory(NioDatagramChannel::new)
        .build()
    );
  }

  public void execute(Request request, long clientId, boolean shared, HttpListener listener) {
    if (request.getNameResolver() == null) {
      // hack: patch request with name resolver
      request = new RequestBuilder(request, request.getUri())
        .setNameResolver(nameResolver)
        .setFixUrlEncoding(false)
        .build();
    }

    client.sendRequest(request, shared ? - 1 : clientId, eventLoopGroup.next(), listener, sslContext, null);
  }

  public InetAddressNameResolver getNameResolver() {
    return nameResolver;
  }

  @Override
  public void close() throws Exception {
    client.close();
    nameResolver.close();
    eventLoopGroup.shutdownGracefully();
  }
}
