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

package io.gatling.http.client;

import io.gatling.http.client.impl.DefaultHttpClient;
import io.gatling.http.client.resolver.InetAddressNameResolver;
import io.gatling.http.client.resolver.InetAddressNameResolverWrapper;
import io.gatling.http.client.uri.Uri;
import io.gatling.netty.util.Transports;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import javax.net.ssl.SSLException;

public class GatlingHttpClient implements AutoCloseable {

  private final SslContext sslContext;
  protected final HttpClient client;
  private final EventLoopGroup eventLoopGroup;
  private final InetAddressNameResolver nameResolver; // would be per request in Gatling

  public GatlingHttpClient(HttpClientConfig config) {
    this.client = new DefaultHttpClient(config);
    eventLoopGroup =
        Transports.newEventLoopGroup(
            config.isUseNativeTransport(), config.isUseIoUring(), 0, "gatling-http");
    try {
      sslContext =
          SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
    } catch (SSLException e) {
      throw new ExceptionInInitializerError(e);
    }

    this.nameResolver =
        new InetAddressNameResolverWrapper(
            new DnsNameResolverBuilder(eventLoopGroup.next())
                .datagramChannelFactory(NioDatagramChannel::new)
                .build());
  }

  public void execute(Request request, long clientId, boolean shared, HttpListener listener) {
    client.sendRequest(
        request,
        shared ? -1 : clientId,
        eventLoopGroup.next(),
        listener,
        new SslContextsHolder.Default(sslContext, null));
  }

  public RequestBuilder newRequestBuilder(HttpMethod method, Uri uri) {
    return new RequestBuilder("request", method, uri, nameResolver);
  }

  @Override
  public void close() throws Exception {
    client.close();
    nameResolver.close();
    eventLoopGroup.shutdownGracefully();
  }
}
