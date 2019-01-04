/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.resolver.InetNameResolver;
import io.netty.resolver.dns.DnsNameResolverBuilder;

// FIXME remove
public class GatlingHttpClient implements AutoCloseable {

  protected final HttpClient client;
  private final NioEventLoopGroup nameResolverEventLoopGroup;
  private final InetNameResolver nameResolver; // would be per request in Gatling

  public GatlingHttpClient(HttpClientConfig config) {
    this.client = new DefaultHttpClient(config);

    this.nameResolverEventLoopGroup = new NioEventLoopGroup(1);
    this.nameResolver = new DnsNameResolverBuilder(nameResolverEventLoopGroup.next())
            .channelFactory(NioDatagramChannel::new)
            .build();
  }

  public void execute(Request request, long clientId, boolean shared, HttpListener listener) {
    if (request.getNameResolver() == null) {
      // hack: patch request with name resolver
      request = new RequestBuilder(request, request.getUri())
        .setNameResolver(nameResolver)
        .setFixUrlEncoding(false)
        .build();
    }

    client.sendRequest(request, clientId, shared, listener, null, null);
  }

  public InetNameResolver getNameResolver() {
    return nameResolver;
  }

  @Override
  public void close() throws Exception {
    client.close();
    nameResolver.close();
    nameResolverEventLoopGroup.shutdownGracefully();
  }
}
