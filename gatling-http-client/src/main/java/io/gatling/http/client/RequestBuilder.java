/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

import io.gatling.http.client.ahc.uri.Uri;
import io.gatling.http.client.ahc.uri.UriEncoder;
import io.gatling.http.client.body.RequestBody;
import io.gatling.http.client.proxy.ProxyServer;
import io.gatling.http.client.realm.Realm;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.resolver.DefaultNameResolver;
import io.netty.resolver.NameResolver;
import io.netty.util.concurrent.ImmediateEventExecutor;

import java.net.InetAddress;
import java.util.Collections;
import java.util.List;

public class RequestBuilder {

  private static NameResolver<InetAddress> DEFAULT_NAME_RESOLVER = new DefaultNameResolver(ImmediateEventExecutor.INSTANCE);

  private final HttpMethod method;
  private final Uri uri;
  private List<Param> queryParams;
  private HttpHeaders headers = new DefaultHttpHeaders(false);
  private List<Cookie> cookies;
  private RequestBody body;
  private long requestTimeout;
  private String virtualHost;
  private InetAddress localAddress;
  private Realm realm;
  private ProxyServer proxyServer;
  private SignatureCalculator signatureCalculator;
  private NameResolver<InetAddress> nameResolver = DEFAULT_NAME_RESOLVER;

  public RequestBuilder(HttpMethod method, Uri uri) {
    this.method = method;
    this.uri = uri;
  }

  public RequestBuilder(Request request, Uri uri) {
    method = request.getMethod();
    this.uri = uri;
    headers = request.getHeaders();
    cookies = request.getCookies();
    body = request.getBody();
    requestTimeout = request.getRequestTimeout();
    virtualHost = request.getVirtualHost();
    localAddress = request.getLocalAddress();
    realm = request.getRealm();
    proxyServer = request.getProxyServer();
    signatureCalculator = request.getSignatureCalculator();
    nameResolver = request.getNameResolver();
  }

  public Uri getUri() {
    return uri;
  }

  public RequestBuilder setQueryParams(List<Param> queryParams) {
    this.queryParams = queryParams;
    return this;
  }

  public RequestBuilder setHeaders(HttpHeaders headers) {
    this.headers = headers;
    return this;
  }

  public RequestBuilder addHeader(CharSequence name, Object value) {
    this.headers.add(name, value);
    return this;
  }

  public RequestBuilder setCookies(List<Cookie> cookies) {
    this.cookies = cookies;
    return this;
  }

  public RequestBuilder setBody(RequestBody body) {
    this.body = body;
    return this;
  }

  public RequestBuilder setRequestTimeout(long requestTimeout) {
    this.requestTimeout = requestTimeout;
    return this;
  }

  public RequestBuilder setVirtualHost(String virtualHost) {
    this.virtualHost = virtualHost;
    return this;
  }

  public RequestBuilder setLocalAddress(InetAddress localAddress) {
    this.localAddress = localAddress;
    return this;
  }

  public RequestBuilder setRealm(Realm realm) {
    this.realm = realm;
    return this;
  }

  public RequestBuilder setProxyServer(ProxyServer proxyServer) {
    this.proxyServer = proxyServer;
    return this;
  }

  public RequestBuilder setSignatureCalculator(SignatureCalculator signatureCalculator) {
    this.signatureCalculator = signatureCalculator;
    return this;
  }

  public RequestBuilder setNameResolver(NameResolver<InetAddress> nameResolver) {
    this.nameResolver = nameResolver;
    return this;
  }

  public Request build(boolean fixUrlEncoding) {

    Uri fullUri = UriEncoder.uriEncoder(fixUrlEncoding).encode(uri, queryParams);

    return new Request(
      method,
      fullUri,
      headers,
      cookies != null ? cookies : Collections.emptyList(),
      body,
      requestTimeout,
      virtualHost,
      localAddress,
      realm,
      proxyServer,
      signatureCalculator,
      nameResolver);
  }
}
