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
import io.gatling.http.client.body.RequestBodyBuilder;
import io.gatling.http.client.proxy.ProxyServer;
import io.gatling.http.client.realm.BasicRealm;
import io.gatling.http.client.realm.Realm;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.cookie.ClientCookieEncoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.resolver.DefaultNameResolver;
import io.netty.resolver.NameResolver;
import io.netty.util.AsciiString;
import io.netty.util.concurrent.ImmediateEventExecutor;

import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

import static io.gatling.http.client.ahc.util.HttpUtils.*;
import static io.gatling.http.client.ahc.util.MiscUtils.isNonEmpty;
import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderNames.HOST;
import static io.netty.handler.codec.http.HttpHeaderNames.ORIGIN;
import static java.nio.charset.StandardCharsets.UTF_8;

public class RequestBuilder {

  private static final AsciiString ACCEPT_ALL_HEADER_VALUE = new AsciiString("*/*");

  private static NameResolver<InetAddress> DEFAULT_NAME_RESOLVER = new DefaultNameResolver(ImmediateEventExecutor.INSTANCE);

  private final HttpMethod method;
  private final Uri uri;
  private List<Param> queryParams;
  private HttpHeaders headers = new DefaultHttpHeaders(false);
  private List<Cookie> cookies;
  private RequestBodyBuilder<?> bodyBuilder;
  private long requestTimeout;
  private String virtualHost;
  private InetAddress localAddress;
  private Realm realm;
  private ProxyServer proxyServer;
  private SignatureCalculator signatureCalculator;
  private NameResolver<InetAddress> nameResolver = DEFAULT_NAME_RESOLVER;
  private boolean http2Enabled;
  private boolean alpnRequired;
  private boolean http2PriorKnowledge;
  private boolean fixUrlEncoding = true;
  private Charset defaultCharset = UTF_8;

  public RequestBuilder(HttpMethod method, Uri uri) {
    this.method = method;
    this.uri = uri;
  }

  public RequestBuilder(Request request, Uri uri) {
    method = request.getMethod();
    this.uri = uri;
    headers = request.getHeaders();
    cookies = request.getCookies();
    bodyBuilder = request.getBody() != null ? request.getBody().newBuilder() : null;
    requestTimeout = request.getRequestTimeout();
    virtualHost = request.getVirtualHost();
    localAddress = request.getLocalAddress();
    realm = request.getRealm();
    proxyServer = request.getProxyServer();
    signatureCalculator = request.getSignatureCalculator();
    nameResolver = request.getNameResolver();
    http2Enabled = request.isHttp2Enabled();
    alpnRequired = request.isAlpnRequired();
    http2PriorKnowledge = request.isHttp2PriorKnowledge();
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

  public RequestBuilder setBodyBuilder(RequestBodyBuilder<?> bodyBuilder) {
    this.bodyBuilder = bodyBuilder;
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

  public RequestBuilder setHttp2Enabled(boolean http2Enabled) {
    this.http2Enabled = http2Enabled;
    return this;
  }

  public RequestBuilder setAlpnRequired(boolean alpnRequired) {
    this.alpnRequired = alpnRequired;
    return this;
  }

  public RequestBuilder setHttp2PriorKnowledge(boolean http2PriorKnowledge) {
    this.http2PriorKnowledge = http2PriorKnowledge;
    return this;
  }

  public RequestBuilder setFixUrlEncoding(boolean fixUrlEncoding) {
    this.fixUrlEncoding = fixUrlEncoding;
    return this;
  }

  public RequestBuilder setDefaultCharset(Charset defaultCharset) {
    this.defaultCharset = defaultCharset;
    return this;
  }

  public Request build() {

    Uri fullUri = UriEncoder.uriEncoder(fixUrlEncoding).encode(uri, queryParams);

    if (!headers.contains(ACCEPT)) {
      headers.set(ACCEPT, ACCEPT_ALL_HEADER_VALUE);
    }

    if (realm instanceof BasicRealm) {
      headers.add(AUTHORIZATION, ((BasicRealm) realm).getAuthorizationHeader());
    }

    String originalAcceptEncoding = headers.get(ACCEPT_ENCODING);
    if (originalAcceptEncoding != null) {
      // we don't support Brotly ATM
      headers.set(ACCEPT_ENCODING, filterOutBrotliFromAcceptEncoding(originalAcceptEncoding));
    }

    if (isNonEmpty(cookies)) {
      headers.set(COOKIE, ClientCookieEncoder.LAX.encode(cookies));
    }

    if (!headers.contains(ORIGIN)) {
      headers.set(ORIGIN, originHeader(uri));
    }

    if (!headers.contains(HOST)) {
      headers.set(HOST, virtualHost != null ? virtualHost : hostHeader(uri));
    }

    RequestBody<?> body = null;
    if (bodyBuilder != null) {
      Charset charset = defaultCharset;
      String contentType = headers.get(CONTENT_TYPE);
      if (contentType != null) {
        Charset contentTypeCharset = extractContentTypeCharsetAttribute(contentType);
        if (contentTypeCharset != null) {
          charset = contentTypeCharset;
        } else {
          // set Content-Type header missing charset attribute
          contentType = contentType + "; charset=" + charset.name();
        }
      }
      body = bodyBuilder.build(contentType, charset);
      String bodyContentType = body.getContentType();
      if (bodyContentType != null) {
        headers.set(CONTENT_TYPE, bodyContentType);
      }
    }

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
      nameResolver,
      http2Enabled,
      alpnRequired,
      http2PriorKnowledge
      );
  }
}
